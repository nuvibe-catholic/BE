package com.umc.nuvibe.domain.notification.entity;

import com.umc.nuvibe.domain.notification.vo.NotificationType;
import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.global.apiPayLoad.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_notification_user",
                    foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 알림함 분류 (채팅, 알림, 미션)
    @Column(nullable = false)
    private String category;

    @Column(name = "main_message", nullable = false)
    private String mainMessage;

    @Column(name = "action_message", nullable = false)
    private String actionMessage;

    // 연관 ID (채팅 ID 등)
    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "tribe_id")
    private Long tribeId;

    @Column(name = "is_read")
    private boolean isRead = false;

    @Builder
    private Notification(User user, NotificationType type, String category,
                         String mainMessage, String actionMessage, Long relatedId, Long tribeId) {
        this.user = user;
        this.type = type;
        this.category = category;
        this.mainMessage = mainMessage;
        this.actionMessage = actionMessage;
        this.relatedId = relatedId;
        this.tribeId = tribeId;
    }

    public static Notification of(User user, NotificationType type, String tag, Long relatedId, Long tribeId) {
        return Notification.builder()
                .user(user)
                .type(type)
                .category(type.getCategory())
                .mainMessage(type.formatMainMessage(tag))
                .actionMessage(type.formatActionMessage(tag))
                .relatedId(relatedId)
                .tribeId(tribeId)
                .build();
    }

    public void read() {
        this.isRead = true;
    }
}
