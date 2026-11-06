package com.umc.nuvibe.domain.notification.entity;

import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.global.apiPayLoad.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "fcm",
        uniqueConstraints = @UniqueConstraint(name = "uk_fcm_user_token", columnNames = {"user_id", "token"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Fcm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_fcm_user",
                    foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL"))
    private User user;

    @Column(nullable = false)
    private String token;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Builder
    private Fcm(User user, String token) {
        this.user = user;
        this.token = token;
    }

    public static Fcm of(User user, String token) {
        return Fcm.builder()
                .user(user)
                .token(token)
                .build();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
