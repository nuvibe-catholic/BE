package com.umc.nuvibe.domain.tribe.dto.response.userTribe;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "트라이브 챗 무음 설정 응답")
public record UserTribeMuteRes(

        @Schema(description = "유저 트라이브 ID")
        Long userTribeId,

        @Schema(description = "무음 설정 여부")
        boolean isMuted
) {
}
