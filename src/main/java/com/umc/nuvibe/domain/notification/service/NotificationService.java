package com.umc.nuvibe.domain.notification.service;

import com.umc.nuvibe.domain.notification.dto.response.NotificationRes;

import java.util.List;

public interface NotificationService {

    List<NotificationRes> getNotifications(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void deleteNotification(Long userId, Long notificationId);
}
