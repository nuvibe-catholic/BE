package com.umc.nuvibe.domain.notification.service;

import com.umc.nuvibe.domain.notification.entity.Notification;
import com.umc.nuvibe.domain.notification.repository.NotificationRepository;
import com.umc.nuvibe.domain.notification.vo.NotificationType;
import com.umc.nuvibe.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 비동기 발송 스레드에서 알림함 저장을 독립 트랜잭션으로 처리
@Service
@RequiredArgsConstructor
public class FcmDbService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void saveNotification(User user, NotificationType type, String tag, Long relatedId, Long tribeId) {
        notificationRepository.save(Notification.of(user, type, tag, relatedId, tribeId));
    }
}
