package com.umc.nuvibe.domain.tribe.service.chat;

import com.umc.nuvibe.domain.archive.entity.ArchiveBoard;
import com.umc.nuvibe.domain.archive.entity.BoardImage;
import com.umc.nuvibe.domain.archive.repository.ArchiveBoardRepository;
import com.umc.nuvibe.domain.archive.repository.BoardImageRepository;
import com.umc.nuvibe.domain.image.entity.Image;
import com.umc.nuvibe.domain.image.repository.ImageRepository;
import com.umc.nuvibe.domain.image.vo.ImageStatus;
import com.umc.nuvibe.domain.tribe.dto.internal.ChatSend;
import com.umc.nuvibe.domain.tribe.dto.internal.EmojiAggRow;
import com.umc.nuvibe.domain.tribe.dto.internal.MyEmojiRow;
import com.umc.nuvibe.domain.tribe.dto.request.ChatGridReq;
import com.umc.nuvibe.domain.tribe.dto.request.ChatTimelineReq;
import com.umc.nuvibe.domain.tribe.dto.response.chat.*;
import com.umc.nuvibe.domain.tribe.entity.Chat;
import com.umc.nuvibe.domain.tribe.entity.Tribe;
import com.umc.nuvibe.domain.tribe.repository.*;
import com.umc.nuvibe.domain.tribe.vo.EmojiType;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import com.umc.nuvibe.global.apiPayLoad.error.*;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String TRIBE_TOPIC_PREFIX = "/topic/tribe.";

    private final ChatRepository chatRepository;
    private final EmojiRepository emojiRepository;
    private final TribeRepository tribeRepository;
    private final UserTribeRepository userTribeRepository;
    private final ScrapedImageRepository scrapedImageRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final ArchiveBoardRepository archiveBoardRepository;
    private final BoardImageRepository boardImageRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public ChatTimelineListRes getChatTimelineList(Long userId, Long tribeId, ChatTimelineReq req) {

        // 1. 트라이브 챗 접근 권한 및 커서 유효성 검증
        validateUserInTribe(userId, tribeId);
        if (req.lastChatId() != null) {
            validateCursorChatInTribe(req.lastChatId(), tribeId);
        }

        // 2. 다음 페이지 존재 여부 판단을 위해 size보다 1개 더 조회
        int limit = req.size();
        Pageable pageable = PageRequest.of(0, limit + 1);

        List<Chat> chats = (req.lastChatId() == null)
                ? chatRepository.findChatTimelineFirstPage(tribeId, pageable)
                : chatRepository.findChatTimelineNextPage(tribeId, req.lastChatId(), pageable);

        boolean hasNext = chats.size() > limit;
        List<Chat> pageItems = hasNext ? chats.subList(0, limit) : chats;

        // 3. 채팅이 없으면 빈 목록 반환
        if (pageItems.isEmpty()) {
            return new ChatTimelineListRes(List.of(), null, false);
        }

        List<Long> chatIds = pageItems.stream()
                .map(Chat::getId)
                .toList();
        List<Long> imageIds = pageItems.stream()
                .map(chat -> chat.getImage().getId())
                .toList();

        // 4. 채팅별 이모지 요약, 내 이모지, 내 스크랩 여부 조회
        Map<Long, List<EmojiSummaryRes>> emojiSummaryMap = buildEmojiSummaryMap(chatIds);
        Map<Long, EmojiType> myEmojiMap = buildMyEmojiMap(userId, chatIds);
        Set<Long> myScrapedImageIds = findMyScrapedImageIds(userId, tribeId, imageIds);

        List<ChatTimelineItemRes> items = pageItems.stream()
                .map(chat -> ChatTimelineItemRes.from(
                        chat,
                        emojiSummaryMap.getOrDefault(chat.getId(), List.of()),
                        myEmojiMap.get(chat.getId()),
                        myScrapedImageIds.contains(chat.getImage().getId())
                ))
                .toList();

        // 5. 다음 페이지가 있을 때만 마지막 chatId로 커서 설정
        Long nextLastChatId = hasNext ? items.get(items.size() - 1).chatId() : null;

        return new ChatTimelineListRes(items, nextLastChatId, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatGridListRes getChatGridList(Long tribeId, ChatGridReq req) {

        // 1. 커서는 (createdAt, chatId) 둘 다 있거나 둘 다 없어야 함
        if (req.hasCursor() && !req.isCursorComplete()) {
            throw new BusinessException(ChatErrorCode.CHAT_CURSOR_INVALID);
        }
        if (req.hasCursor()) {
            validateCursorChatInTribe(req.cursorChatId(), tribeId);
        }

        // 2. 다음 페이지 존재 여부 판단을 위해 size보다 1개 더 조회
        int limit = req.size();
        Pageable pageable = PageRequest.of(0, limit + 1);

        List<Chat> chats = req.hasCursor()
                ? chatRepository.findChatGridNextPage(tribeId, req.cursorCreatedAt(), req.cursorChatId(), pageable)
                : chatRepository.findChatGridFirstPage(tribeId, pageable);

        boolean hasNext = chats.size() > limit;
        List<Chat> pageItems = hasNext ? chats.subList(0, limit) : chats;

        List<ChatGridItemRes> items = pageItems.stream()
                .map(ChatGridItemRes::from)
                .toList();

        // 3. 다음 페이지가 있을 때만 마지막 데이터 기준으로 커서 설정
        LocalDateTime nextCursorCreatedAt = null;
        Long nextCursorChatId = null;

        if (hasNext) {
            ChatGridItemRes last = items.get(items.size() - 1);
            nextCursorCreatedAt = last.createdAt();
            nextCursorChatId = last.chatId();
        }

        return new ChatGridListRes(items, nextCursorCreatedAt, nextCursorChatId, hasNext);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatDetailRes getChatDetail(Long userId, Long chatId) {

        Chat chat = chatRepository.findDetailByChatId(chatId)
                .orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_NOT_FOUND));

        validateUserInTribe(userId, chat.getTribe().getId());

        boolean isScraped = scrapedImageRepository.existsByUser_IdAndImage_Id(userId, chat.getImage().getId());

        return ChatDetailRes.from(chat, isScraped);
    }

    @Override
    @Transactional
    public void sendChat(Long userId, Long tribeId, Long imageId, Long boardId) {

        // 1. 트라이브 존재 및 발신 권한(ACTIVE 참여자) 검증
        Tribe tribe = tribeRepository.findById(tribeId)
                .orElseThrow(() -> new BusinessException(TribeErrorCode.TRIBE_NOT_FOUND));

        boolean canSend = userTribeRepository.existsByUser_IdAndTribe_IdAndUserTribeStatus(
                userId, tribeId, UserTribeStatus.ACTIVE
        );
        if (!canSend) {
            throw new BusinessException(UserTribeErrorCode.USERTRIBE_FORBIDDEN);
        }

        // 2. 업로드가 완료된 이미지인지 검증
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));
        if (image.getStatus() != ImageStatus.ACTIVE) {
            throw new BusinessException(ImageErrorCode.IMAGE_UPLOAD_NOT_COMPLETED);
        }

        // 3. 선택한 보드에 이미지 저장
        saveImageToBoard(userId, boardId, image);

        // 4. 채팅 저장 (유저는 참조로 조회)
        User userRef = userRepository.getReferenceById(userId);
        Chat chat = chatRepository.saveAndFlush(Chat.of(userRef, tribe, image));

        // 5. 트라이브 마지막 채팅 및 참여자 활동 시각 갱신
        tribe.updateLastChat(chat.getId());
        userTribeRepository.updateLastActivityAt(tribeId, UserTribeStatus.ACTIVE, chat.getCreatedAt());

        // 6. 발신자를 제외한 ACTIVE 참여자의 안 읽은 메시지 수 증가
        userTribeRepository.incrementUnreadCountForActiveMembers(tribeId, UserTribeStatus.ACTIVE, userId);

        // 7. 커밋 이후 트라이브 채널로 채팅 발행
        ChatSend chatSend = new ChatSend(
                chat.getId(),
                userId,
                image.getId(),
                image.getThumbnailUrl(),
                chat.getCreatedAt()
        );
        publishChatAfterCommit(tribeId, chatSend);
    }

    // 본인 보드인지 확인 후 이미지 저장 (이미지는 하나의 보드에만 저장 가능)
    private void saveImageToBoard(Long userId, Long boardId, Image image) {
        ArchiveBoard board = archiveBoardRepository.findByIdAndUserId(boardId, userId)
                .orElseThrow(() -> new BusinessException(ArchiveErrorCode.BOARD_NOT_FOUND));

        if (boardImageRepository.existsByImageId(image.getId())) {
            throw new BusinessException(ArchiveErrorCode.BOARD_IMAGE_ALREADY_EXISTS);
        }

        boardImageRepository.save(BoardImage.builder()
                .board(board)
                .image(image)
                .build());
    }

    // 트랜잭션 커밋 이후 트라이브 채널로 채팅 발행
    private void publishChatAfterCommit(Long tribeId, ChatSend chatSend) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    messagingTemplate.convertAndSend(TRIBE_TOPIC_PREFIX + tribeId, chatSend);
                } catch (MessagingException e) {
                    log.error("웹소켓 채팅 발행 실패. chatId={}, tribeId={}", chatSend.chatId(), tribeId, e);
                }
            }
        });
    }

    // 채팅별 이모지 타입 개수 집계
    private Map<Long, List<EmojiSummaryRes>> buildEmojiSummaryMap(List<Long> chatIds) {
        List<EmojiAggRow> rows = emojiRepository.countByChatIdGroupByType(chatIds);

        return rows.stream()
                .collect(Collectors.groupingBy(
                        EmojiAggRow::chatId,
                        Collectors.mapping(
                                r -> new EmojiSummaryRes(r.emojiType(), r.count()),
                                Collectors.toList()
                        )
                ));
    }

    // 채팅별 내가 누른 이모지 (chatId당 최대 1건)
    private Map<Long, EmojiType> buildMyEmojiMap(Long userId, List<Long> chatIds) {
        List<MyEmojiRow> rows = emojiRepository.findMyEmoji(userId, chatIds);

        return rows.stream()
                .collect(Collectors.toMap(
                        MyEmojiRow::chatId,
                        MyEmojiRow::emojiType
                ));
    }

    // 조회한 이미지 중 내가 스크랩한 imageId 목록
    private Set<Long> findMyScrapedImageIds(Long userId, Long tribeId, List<Long> imageIds) {
        return new HashSet<>(
                scrapedImageRepository.findImageIdsByUserIdAndTribeIdAndImageIds(userId, tribeId, imageIds)
        );
    }

    // 커서로 넘어온 채팅이 해당 트라이브 챗의 채팅인지 확인
    private void validateCursorChatInTribe(Long chatId, Long tribeId) {
        if (!chatRepository.existsByIdAndTribe_Id(chatId, tribeId)) {
            throw new BusinessException(ChatErrorCode.CHAT_CURSOR_INVALID);
        }
    }

    // 유저가 해당 트라이브 챗에 입장해있는지 확인
    private void validateUserInTribe(Long userId, Long tribeId) {
        if (!userTribeRepository.existsByUser_IdAndTribe_Id(userId, tribeId)) {
            throw new BusinessException(UserTribeErrorCode.USERTRIBE_NOT_FOUND);
        }
    }
}
