package com.umc.nuvibe.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.umc.nuvibe.domain.notification.entity.Fcm;
import com.umc.nuvibe.domain.notification.repository.FcmRepository;
import com.umc.nuvibe.domain.notification.vo.NotificationType;
import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import com.umc.nuvibe.domain.user.vo.UserSetting;
import com.umc.nuvibe.global.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmAsyncService {

    private static final String PUSH_TITLE = "Nuvibe";

    private final FcmRepository fcmRepository;
    private final UserRepository userRepository;
    private final FcmDbService fcmDbService;
    private final FcmTokenService fcmTokenService;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    public void sendNotification(Long userId, NotificationType type, String tag, Long relatedId, Long tribeId) {

        // 1. 수신자 조회 및 알림 설정 확인 (탈퇴했거나 해당 알림을 끈 유저는 스킵)
        Optional<User> receiver = userRepository.findById(userId);
        if (receiver.isEmpty() || !isNotificationEnabled(receiver.get().getSetting(), type)) {
            return;
        }

        // 2. 알림함 저장
        try {
            fcmDbService.saveNotification(receiver.get(), type, tag, relatedId, tribeId);
        } catch (Exception e) {
            log.error("알림 저장 실패. userId={}, type={}", userId, type, e);
        }

        // 3. 푸시 대상이 아니거나 FCM 이 꺼져 있으면 알림함 저장까지만
        if (!firebaseEnabled || !type.hasPush()) {
            return;
        }

        // 4. 유저의 활성 토큰 전체로 푸시 발송
        String body = type.formatPushMessage(tag);
        for (Fcm fcm : fcmRepository.findByUser_IdAndIsActiveTrue(userId)) {
            sendPush(fcm.getToken(), body);
        }
    }

    // 설정 값이 없으면(기존 가입자 등) 수신 허용으로 본다
    private boolean isNotificationEnabled(UserSetting setting, NotificationType type) {
        if (setting == null) {
            return true;
        }

        Boolean enabled = switch (type) {
            case NOTI_01 -> setting.getIsTribeCreateAlert();
            case NOTI_02, NOTI_04, NOTI_05 -> setting.getIsTribeChatAlert();
            case NOTI_03 -> setting.getIsReactionAlert();
            case NOTI_06, NOTI_07 -> setting.getIsRecommendAlert();
            case NOTI_08, NOTI_09 -> setting.getIsRecapAlert();
            case NOTI_10, NOTI_11 -> setting.getIsSecurityAlert();
        };
        return !Boolean.FALSE.equals(enabled);
    }

    private void sendPush(String token, String body) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(PUSH_TITLE)
                        .setBody(body)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 발송 실패. errorCode={}", e.getMessagingErrorCode(), e);
            // 앱 삭제 등으로 만료된 토큰은 다음 발송부터 제외
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                fcmTokenService.deactivateToken(token);
            }
        }
    }
}
