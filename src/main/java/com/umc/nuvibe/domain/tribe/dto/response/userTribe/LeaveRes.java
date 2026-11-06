package com.umc.nuvibe.domain.tribe.dto.response.userTribe;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "트라이브 챗 퇴장 응답")
public record LeaveRes(

        @Schema(description = "삭제된 유저 트라이브 ID")
        Long userTribeId,

        @Schema(description = "퇴장한 트라이브 ID")
        Long tribeId
) {
}
