package com.umc.nuvibe.domain.tribe.service.scrapedImage;

import com.umc.nuvibe.domain.image.entity.Image;
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
import com.umc.nuvibe.global.apiPayLoad.error.UserErrorCode;
import com.umc.nuvibe.global.apiPayLoad.error.UserTribeErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
