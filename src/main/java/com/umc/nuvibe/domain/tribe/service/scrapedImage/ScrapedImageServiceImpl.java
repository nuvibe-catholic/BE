package com.umc.nuvibe.domain.tribe.service.scrapedImage;

import com.umc.nuvibe.domain.image.entity.Image;
import com.umc.nuvibe.domain.tribe.dto.request.ScrapedImageSliceReq;
import com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageItemRes;
import com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageListRes;
import com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageToggleRes;
import com.umc.nuvibe.domain.tribe.entity.Chat;
import com.umc.nuvibe.domain.tribe.entity.ScrapedImage;
import com.umc.nuvibe.domain.tribe.entity.Tribe;
import com.umc.nuvibe.domain.tribe.repository.ChatRepository;
import com.umc.nuvibe.domain.tribe.repository.ScrapedImageRepository;
import com.umc.nuvibe.domain.tribe.repository.UserTribeRepository;
import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import com.umc.nuvibe.global.apiPayLoad.error.ChatErrorCode;
import com.umc.nuvibe.global.apiPayLoad.error.ScrapedImageErrorCode;
import com.umc.nuvibe.global.apiPayLoad.error.UserErrorCode;
import com.umc.nuvibe.global.apiPayLoad.error.UserTribeErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScrapedImageServiceImpl implements ScrapedImageService {

    private final ScrapedImageRepository scrapedImageRepository;
    private final ChatRepository chatRepository;
    private final UserTribeRepository userTribeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ScrapedImageToggleRes toggleScrapedImage(Long userId, Long chatId) {

        // 1. 채팅과 연관된 트라이브, 이미지 조회
        Chat chat = chatRepository.findByIdWithImageAndTribe(chatId)
                .orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_NOT_FOUND));

        Tribe tribe = chat.getTribe();
        Image image = chat.getImage();

        // 2. 해당 트라이브 챗 접근 권한 검증
        validateUserInTribe(userId, tribe.getId());
        User user = getUserOrThrow(userId);

        // 3. 이미 스크랩했으면 삭제 후 null 반환, 없으면 저장 후 스크랩 이미지 ID 반환
        return scrapedImageRepository.findByUser_IdAndTribe_IdAndImage_Id(userId, tribe.getId(), image.getId())
                .map(existing -> {
                    scrapedImageRepository.delete(existing);
                    return new ScrapedImageToggleRes(null);
                })
                .orElseGet(() -> {
                    ScrapedImage saved = scrapedImageRepository.save(ScrapedImage.of(user, tribe, image));
                    return new ScrapedImageToggleRes(saved.getId());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ScrapedImageListRes getTotalScrapedImage(Long userId, ScrapedImageSliceReq req) {
        return getScrapedImages(userId, null, req);
    }

    @Override
    @Transactional(readOnly = true)
    public ScrapedImageListRes getTribeScrapedImage(Long userId, Long tribeId, ScrapedImageSliceReq req) {
        validateUserInTribe(userId, tribeId);
        return getScrapedImages(userId, tribeId, req);
    }

    // tribeId가 null이면 전체, 있으면 해당 트라이브 챗 내 스크랩 목록 조회
    private ScrapedImageListRes getScrapedImages(Long userId, Long tribeId, ScrapedImageSliceReq req) {

        // 1. 커서는 (createdAt, id) 둘 다 있거나 둘 다 없어야 함
        if (req.hasCursor() && !req.isCursorComplete()) {
            throw new BusinessException(ScrapedImageErrorCode.SCRAPEDIMAGE_CURSOR_ERROR);
        }

        // 2. 다음 페이지 존재 여부 판단을 위해 size보다 1개 더 조회
        int limit = req.size();
        Pageable pageable = PageRequest.of(0, limit + 1);

        List<ScrapedImageItemRes> scraps = req.hasCursor()
                ? scrapedImageRepository.findMyScrapsNextPage(
                        userId, tribeId, req.imageTag(), req.cursorCreatedAt(), req.cursorId(), pageable)
                : scrapedImageRepository.findMyScrapsFirstPage(userId, tribeId, req.imageTag(), pageable);

        boolean hasNext = scraps.size() > limit;
        List<ScrapedImageItemRes> items = hasNext ? scraps.subList(0, limit) : scraps;

        // 3. 다음 페이지가 있을 때만 마지막 데이터 기준으로 커서 설정
        LocalDateTime nextCursorCreatedAt = null;
        Long nextCursorId = null;

        if (hasNext) {
            ScrapedImageItemRes last = items.get(items.size() - 1);
            nextCursorCreatedAt = last.createdAt();
            nextCursorId = last.scrapImageId();
        }

        return new ScrapedImageListRes(items, nextCursorCreatedAt, nextCursorId, hasNext);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    // 유저가 해당 트라이브 챗에 입장해있는지 확인
    private void validateUserInTribe(Long userId, Long tribeId) {
        if (!userTribeRepository.existsByUser_IdAndTribe_Id(userId, tribeId)) {
            throw new BusinessException(UserTribeErrorCode.USERTRIBE_NOT_FOUND);
        }
    }
}
