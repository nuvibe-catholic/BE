package com.umc.nuvibe.domain.notification.service;

import com.umc.nuvibe.domain.notification.vo.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// 알림 발송 진입점, 실제 저장/푸시는 수신자별 비동기로 처리
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmAsyncService fcmAsyncService;

    public void sendNotification(Long userId, NotificationType type, String tag, Long relatedId, Long tribeId) {
        fcmAsyncService.sendNotification(userId, type, tag, relatedId, tribeId);
    }

    public void sendNotificationToUsers(List<Long> userIds, NotificationType type, String tag, Long relatedId, Long tribeId) {
        for (Long userId : userIds) {
            fcmAsyncService.sendNotification(userId, type, tag, relatedId, tribeId);
        }
    }
}
