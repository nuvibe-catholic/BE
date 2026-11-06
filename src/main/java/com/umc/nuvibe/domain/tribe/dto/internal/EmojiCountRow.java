package com.umc.nuvibe.domain.tribe.dto.internal;

import com.umc.nuvibe.domain.tribe.vo.EmojiType;

// 단일 채팅의 이모지 타입별 집계 결과
public record EmojiCountRow(

        EmojiType emojiType,
        Long count
) {
}
