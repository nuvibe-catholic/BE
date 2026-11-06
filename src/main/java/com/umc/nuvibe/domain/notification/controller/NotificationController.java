package com.umc.nuvibe.domain.notification.controller;

import com.umc.nuvibe.domain.notification.dto.request.FcmTokenReq;
import com.umc.nuvibe.domain.notification.dto.response.NotificationRes;
import com.umc.nuvibe.domain.notification.service.FcmTokenService;
import com.umc.nuvibe.domain.notification.service.NotificationService;
import com.umc.nuvibe.global.apiPayLoad.response.Response;
import com.umc.nuvibe.global.apiPayLoad.result.NotificationResultCode;
import com.umc.nuvibe.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final FcmTokenService fcmTokenService;

    @PostMapping("/fcm-token")
    @Operation(summary = "FCM 토큰 등록", description = "푸시 알림 수신을 위한 기기 FCM 토큰 등록")
    public Response<Void> registerFcmToken(
            @AuthUser Long userId,
            @Valid @RequestBody FcmTokenReq req
    ) {
        fcmTokenService.registerToken(userId, req.token());
        return Response.of(NotificationResultCode.FCM_TOKEN_REGISTERED);
    }

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "알림함 목록을 최신순으로 조회")
    public Response<List<NotificationRes>> getNotifications(@AuthUser Long userId) {
        List<NotificationRes> res = notificationService.getNotifications(userId);
        return Response.of(NotificationResultCode.NOTIFICATION_LIST_SUCCESS, res);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경")
    public Response<Void> markAsRead(
            @AuthUser Long userId,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(userId, notificationId);
        return Response.of(NotificationResultCode.NOTIFICATION_READ_SUCCESS);
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 삭제", description = "특정 알림 삭제")
    public Response<Void> deleteNotification(
            @AuthUser Long userId,
            @PathVariable Long notificationId
    ) {
        notificationService.deleteNotification(userId, notificationId);
        return Response.of(NotificationResultCode.NOTIFICATION_DELETE_SUCCESS);
    }
}
