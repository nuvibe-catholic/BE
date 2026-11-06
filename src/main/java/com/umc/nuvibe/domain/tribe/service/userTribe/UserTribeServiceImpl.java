package com.umc.nuvibe.domain.tribe.service.userTribe;

import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeActivateRes;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeFavoriteRes;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeMuteRes;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import com.umc.nuvibe.domain.tribe.repository.UserTribeRepository;
import com.umc.nuvibe.domain.tribe.vo.TribeStatus;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import com.umc.nuvibe.global.apiPayLoad.error.TribeErrorCode;
import com.umc.nuvibe.global.apiPayLoad.error.UserTribeErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserTribeServiceImpl implements UserTribeService {

    private final UserTribeRepository userTribeRepository;

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
