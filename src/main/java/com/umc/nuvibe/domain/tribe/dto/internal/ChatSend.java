package com.umc.nuvibe.domain.tribe.dto.internal;

import java.time.LocalDateTime;

// 트라이브 채널(/topic/tribe.{tribeId})로 발행되는 채팅 메시지
public record ChatSend(

        Long chatId,
        Long senderUserId,
        Long imageId,
        String imageUrl,
        LocalDateTime createdAt
) {
}
