package com.umc.nuvibe.domain.tribe.dto.response.userTribe;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import com.umc.nuvibe.domain.tribe.vo.TribeStatus;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "유저 트라이브 챗 활성화 응답")
public record UserTribeActivateRes(

        @Schema(description = "유저 트라이브 ID")
        Long userTribeId,

        @Schema(description = "트라이브 ID")
        Long tribeId,

        @Schema(description = "이미지 태그 (Enum)", example = "CAFE")
        ImageTag imageTag,

        @Schema(description = "트라이브 챗 고정 여부")
        Boolean isFavorite,

        @Schema(description = "트라이브 챗 상태")
        TribeStatus tribeStatus,

        @Schema(description = "유저 트라이브 상태")
        UserTribeStatus userTribeStatus,

        @Schema(description = "트라이브 챗 입장 시각")
        LocalDateTime createdAt
) {
    public static UserTribeActivateRes from(UserTribe userTribe) {
        return new UserTribeActivateRes(
                userTribe.getId(),
                userTribe.getTribe().getId(),
                userTribe.getTribe().getImageTag(),
                userTribe.isFavorite(),
                userTribe.getTribe().getStatus(),
                userTribe.getUserTribeStatus(),
                userTribe.getCreatedAt()
        );
    }
}
