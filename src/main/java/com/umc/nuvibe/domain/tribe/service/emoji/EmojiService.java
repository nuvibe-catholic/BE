package com.umc.nuvibe.domain.tribe.service.emoji;

import com.umc.nuvibe.domain.tribe.vo.EmojiType;

public interface EmojiService {

    /**
     * 채팅 이모지 반응
     * 기존 반응 없음 : 등록 / 같은 이모지 : 취소 / 다른 이모지 : 변경
     */
    void reactEmoji(Long userId, Long chatId, EmojiType type);
}
