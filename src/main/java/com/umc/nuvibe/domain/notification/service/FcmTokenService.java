package com.umc.nuvibe.domain.notification.service;

import com.umc.nuvibe.domain.notification.entity.Fcm;
import com.umc.nuvibe.domain.notification.repository.FcmRepository;
import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import com.umc.nuvibe.global.apiPayLoad.error.UserErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmRepository fcmRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerToken(Long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 1. 같은 기기 토큰이 다른 계정에 활성화 되어있으면 비활성화 (기기당 한 계정만 수신)
        fcmRepository.findByTokenAndIsActiveTrueAndUser_IdNot(token, userId)
                .forEach(Fcm::deactivate);

        // 2. 이미 등록된 토큰이면 재활성화, 없으면 새로 저장
        fcmRepository.findByUser_IdAndToken(userId, token)
                .ifPresentOrElse(
                        Fcm::activate,
                        () -> fcmRepository.save(Fcm.of(user, token))
                );
    }

    // FCM 에서 만료(UNREGISTERED) 응답을 받은 토큰 비활성화
    @Transactional
    public void deactivateToken(String token) {
        fcmRepository.findByToken(token).forEach(Fcm::deactivate);
    }

    // 로그아웃 시 해당 유저의 모든 토큰 비활성화
    @Transactional
    public void deactivateAllTokens(Long userId) {
        fcmRepository.findByUser_IdAndIsActiveTrue(userId).forEach(Fcm::deactivate);
    }
}
