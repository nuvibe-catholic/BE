package com.umc.nuvibe.domain.tribe.dto.internal;

import com.umc.nuvibe.domain.image.vo.ImageTag;

import java.time.LocalDateTime;

// Active 트라이브 챗 목록 조회 결과 (응답 및 다음 커서 생성용)
public record ActiveTribeRow(

        Long tribeId,
        Long userTribeId,
        ImageTag imageTag,
        Integer counts,
        boolean isFavorite,
        LocalDateTime lastActivityAt,
        int unreadCount,
        Long lastChatId
) {
}
