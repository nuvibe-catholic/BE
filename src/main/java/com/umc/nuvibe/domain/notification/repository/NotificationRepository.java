package com.umc.nuvibe.domain.notification.repository;

import com.umc.nuvibe.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 유저 알림 목록 (최신순)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 본인 알림 단건 조회
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
