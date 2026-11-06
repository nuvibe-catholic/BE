package com.umc.nuvibe.domain.tribe.dto.internal;

import com.umc.nuvibe.domain.tribe.vo.EmojiType;

/**
 * 여러 채팅에 대한 이모지 집계 결과
 * (chatId, EmojiType) 기준 count
 */
public record EmojiAggRow(
        Long chatId,
        EmojiType emojiType,
        Long count
) {
}
