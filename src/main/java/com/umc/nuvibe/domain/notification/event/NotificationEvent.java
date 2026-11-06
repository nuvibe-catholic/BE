package com.umc.nuvibe.domain.notification.event;

import com.umc.nuvibe.domain.notification.vo.NotificationType;

import java.util.List;

/**
 * 다른 도메인에서 알림 발송을 요청할 때 발행하는 이벤트
 * 트랜잭션 커밋 이후에만 발송되므로 롤백된 작업에는 알림이 나가지 않는다
 */
public record NotificationEvent(
        List<Long> receiverIds,
        NotificationType type,
        String tag,
        Long relatedId,
        Long tribeId
) {
    public static NotificationEvent of(List<Long> receiverIds, NotificationType type, String tag, Long relatedId, Long tribeId) {
        return new NotificationEvent(List.copyOf(receiverIds), type, tag, relatedId, tribeId);
    }

    public static NotificationEvent of(Long receiverId, NotificationType type, String tag, Long relatedId, Long tribeId) {
        return new NotificationEvent(List.of(receiverId), type, tag, relatedId, tribeId);
    }
}
