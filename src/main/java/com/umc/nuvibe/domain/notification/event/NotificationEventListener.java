package com.umc.nuvibe.domain.notification.event;

import com.umc.nuvibe.domain.notification.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final FcmService fcmService;

    // 발행한 쪽 트랜잭션이 커밋된 뒤에만 발송
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationEvent event) {
        if (event.receiverIds().isEmpty()) {
            return;
        }
        fcmService.sendNotificationToUsers(
                event.receiverIds(),
                event.type(),
                event.tag(),
                event.relatedId(),
                event.tribeId()
        );
    }
}
