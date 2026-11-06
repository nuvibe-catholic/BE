package com.umc.nuvibe.domain.tribe.dto.internal;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 트라이브 채널(/topic/tribe.{tribeId})로 발행되는 이모지 변경 메시지
 * action : CREATED / UPDATED / DELETED
 * summaryCounts : 모든 이모지 타입별 개수 (0 포함)
 */
public record EmojiChanged(

        Long tribeId,
        Long chatId,
        Long userId,
        String action,
        String actorEmojiType,
        Map<String, Long> summaryCounts,
        LocalDateTime publishedAt
) {
}
