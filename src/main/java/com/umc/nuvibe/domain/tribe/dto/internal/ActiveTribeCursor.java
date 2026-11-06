package com.umc.nuvibe.domain.tribe.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Active 트라이브 챗 목록 커서")
public record ActiveTribeCursor(

        @Schema(description = "즐겨찾기 여부")
        Boolean fav,

        @Schema(description = "마지막 활동 시각")
        LocalDateTime lastActivityAt,

        @Schema(description = "안 읽은 메시지 존재 여부")
        Boolean unread,

        @Schema(description = "마지막 채팅 ID")
        Long lastChatId
) {

    // 정렬 조건 비교를 위한 int 변환
    public int favInt() {
        return Boolean.TRUE.equals(fav) ? 1 : 0;
    }

    public int unreadInt() {
        return Boolean.TRUE.equals(unread) ? 1 : 0;
    }

    // 커서 값이 모두 채워져 있는지 확인
    public boolean isComplete() {
        return fav != null && unread != null && lastActivityAt != null && lastChatId != null;
    }
}
