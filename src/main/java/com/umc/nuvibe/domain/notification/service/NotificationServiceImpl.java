package com.umc.nuvibe.domain.notification.service;

import com.umc.nuvibe.domain.notification.dto.response.NotificationRes;
import com.umc.nuvibe.domain.notification.entity.Notification;
import com.umc.nuvibe.domain.notification.repository.NotificationRepository;
import com.umc.nuvibe.global.apiPayLoad.error.NotificationErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationRes> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationRes::from)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        getMyNotification(userId, notificationId).read();
    }

    @Override
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        notificationRepository.delete(getMyNotification(userId, notificationId));
    }

    // 본인 알림만 조회 (타인 알림 ID 는 존재 여부를 드러내지 않도록 NOT_FOUND)
    private Notification getMyNotification(Long userId, Long notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
