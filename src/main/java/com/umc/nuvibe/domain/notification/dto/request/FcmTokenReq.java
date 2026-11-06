package com.umc.nuvibe.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenReq(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String token
) {
}
