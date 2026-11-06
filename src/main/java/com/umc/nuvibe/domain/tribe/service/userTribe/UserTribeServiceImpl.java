package com.umc.nuvibe.domain.tribe.service.userTribe;

import com.umc.nuvibe.domain.tribe.dto.internal.ActiveTribeCursor;
import com.umc.nuvibe.domain.tribe.dto.internal.ActiveTribeRow;
import com.umc.nuvibe.domain.tribe.dto.request.ActiveTribeListReq;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.*;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import com.umc.nuvibe.domain.tribe.repository.*;
import com.umc.nuvibe.domain.tribe.vo.TribeStatus;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import com.umc.nuvibe.global.apiPayLoad.error.TribeErrorCode;
import com.umc.nuvibe.global.apiPayLoad.error.UserTribeErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserTribeServiceImpl implements UserTribeService {

    private static final int DEFAULT_WAITING_PAGE_SIZE = 20;

    private final UserTribeRepository userTribeRepository;
    private final TribeRepository tribeRepository;
    private final ChatRepository chatRepository;
    private final EmojiRepository emojiRepository;
    private final ScrapedImageRepository scrapedImageRepository;

    @Override
    @Transactional
    public UserTribeActivateRes activateUserTribe(Long userId, Long userTribeId) {

        UserTribe userTribe = getMyUserTribe(userId, userTribeId);

        if (userTribe.getUserTribeStatus() == UserTribeStatus.ACTIVE) {
            throw new BusinessException(UserTribeErrorCode.USERTRIBE_ALREADY_ACTIVE);
        }

        if (userTribe.getTribe().getStatus() != TribeStatus.WAITING) {
            throw new BusinessException(TribeErrorCode.ACTIVATION_NOT_READY);
        }

        // 유저 트라이브 활성화 및 활동 시각 최신화
        userTribe.activate();
        userTribe.updateLastActivityAt(LocalDateTime.now());

        return UserTribeActivateRes.from(userTribe);
    }

    @Override
    @Transactional
    public UserTribeFavoriteRes toggleFavorite(Long userId, Long userTribeId) {

        UserTribe userTribe = getMyUserTribe(userId, userTribeId);
        userTribe.toggleFavorite();

        return UserTribeFavoriteRes.from(userTribe);
    }

    @Override
    @Transactional
    public UserTribeMuteRes toggleMute(Long userId, Long userTribeId) {

        UserTribe userTribe = getMyUserTribe(userId, userTribeId);
        userTribe.toggleMute();

        return new UserTribeMuteRes(userTribe.getId(), userTribe.isMuted());
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveTribeListRes getActiveTribeList(Long userId, ActiveTribeListReq req) {

        int size = req.size();
        boolean hasCursor = req.hasCursor();
        ActiveTribeCursor cursor = req.cursor();

        // 1. 다음 페이지 존재 여부 판단을 위해 size보다 1개 더 조회
        Pageable pageable = PageRequest.of(0, size + 1);

        // 2. 커서가 없으면 첫 페이지, 있으면 커서 이후 데이터만 조회
        List<ActiveTribeRow> rows = userTribeRepository.findActiveTribes(
                userId,
                UserTribeStatus.ACTIVE,
                hasCursor,
                hasCursor ? cursor.favInt() : 0,
                hasCursor ? cursor.lastActivityAt() : null,
                hasCursor ? cursor.unreadInt() : 0,
                hasCursor ? cursor.lastChatId() : 0L,
                pageable
        );

        // 3. 다음 페이지 존재 여부 판단 및 size까지만 반환
        boolean hasNext = rows.size() > size;
        List<ActiveTribeRow> content = hasNext ? rows.subList(0, size) : rows;

        List<ActiveTribeItemRes> items = content.stream()
                .map(ActiveTribeItemRes::from)
                .toList();

        // 4. 다음 페이지가 있을 때만 마지막 요소 기준으로 커서 생성
        ActiveTribeCursor nextCursor = null;
        if (hasNext) {
            ActiveTribeRow last = content.get(content.size() - 1);
            nextCursor = new ActiveTribeCursor(
                    last.isFavorite(),
                    last.lastActivityAt(),
                    last.unreadCount() > 0,
                    last.lastChatId() == null ? 0L : last.lastChatId()
            );
        }

        return new ActiveTribeListRes(items, hasNext, nextCursor);
    }

    @Override
    @Transactional(readOnly = true)
    public WaitingTribeListRes getWaitingTribeList(Long userId, Long cursorTribeId, Integer size) {

        // 1. size 미지정 시 기본 20
        int pageSize = (size == null || size < 1) ? DEFAULT_WAITING_PAGE_SIZE : size;
        boolean hasCursor = cursorTribeId != null;

        // 2. 커서가 없으면 첫 페이지, 있으면 cursorTribeId보다 작은 tribeId만 조회
        List<WaitingTribeItemRes> rows = userTribeRepository.findWaitingTribes(
                userId,
                UserTribeStatus.WAITING,
                hasCursor,
                hasCursor ? cursorTribeId : 0L,
                PageRequest.of(0, pageSize + 1)
        );

        // 3. 다음 페이지 존재 여부 판단 및 size까지만 반환
        boolean hasNext = rows.size() > pageSize;
        List<WaitingTribeItemRes> content = hasNext ? rows.subList(0, pageSize) : rows;

        // 4. 다음 페이지가 있을 때만 마지막 tribeId로 커서 생성
        Long nextCursor = hasNext ? content.get(content.size() - 1).tribeId() : null;

        return new WaitingTribeListRes(content, hasNext, nextCursor);
    }

    @Override
    @Transactional
    public TribeReadRes readChat(Long userId, Long tribeId) {

        // 1. ACTIVE 참여자만 읽음 처리 가능
        boolean isActiveMember = userTribeRepository.existsByUser_IdAndTribe_IdAndUserTribeStatus(
                userId, tribeId, UserTribeStatus.ACTIVE
        );
        if (!isActiveMember) {
            throw new BusinessException(UserTribeErrorCode.USERTRIBE_FORBIDDEN);
        }

        // 2. 트라이브 챗의 최신 chatId 조회, 채팅이 없으면 그대로 성공 반환
        Optional<Long> lastChatId = tribeRepository.findLastChatId(tribeId);
        if (lastChatId.isEmpty()) {
            return new TribeReadRes(tribeId, null);
        }

        // 3. 최신 chatId까지 읽음 처리 및 안 읽은 메시지 수 초기화
        Long lastId = lastChatId.get();
        userTribeRepository.readChat(userId, tribeId, lastId, UserTribeStatus.ACTIVE);

        return new TribeReadRes(tribeId, lastId);
    }

    @Override
    @Transactional
    public LeaveRes leaveTribe(Long userId, Long userTribeId) {

        UserTribe userTribe = getMyUserTribe(userId, userTribeId);
        Long tribeId = userTribe.getTribe().getId();

        // 1. 해당 트라이브 챗에서 스크랩한 이미지 삭제 (원본 이미지는 유지)
        scrapedImageRepository.deleteAllByUserIdAndTribeId(userId, tribeId);

        // 2. 내가 남긴 이모지 반응 및 내 채팅에 달린 이모지 반응 삭제
        emojiRepository.deleteAllByUserIdAndTribeId(userId, tribeId);
        emojiRepository.deleteAllOnMyChatsByUserIdAndTribeId(userId, tribeId);

        // 3. 내 채팅 삭제
        chatRepository.deleteAllByUserIdAndTribeId(userId, tribeId);

        // 4. 유저 트라이브 삭제 및 트라이브 인원 수 감소
        userTribeRepository.delete(userTribe);
        tribeRepository.decrementCounts(tribeId);

        return new LeaveRes(userTribeId, tribeId);
    }

    // 유저 트라이브 조회 및 본인 소유 여부 검증
    private UserTribe getMyUserTribe(Long userId, Long userTribeId) {

        UserTribe userTribe = userTribeRepository.findById(userTribeId)
                .orElseThrow(() -> new BusinessException(UserTribeErrorCode.USERTRIBE_NOT_FOUND));

        if (!userTribe.getUser().getId().equals(userId)) {
            throw new BusinessException(UserTribeErrorCode.USERTRIBE_NOT_JOINED);
        }

        return userTribe;
    }
}
