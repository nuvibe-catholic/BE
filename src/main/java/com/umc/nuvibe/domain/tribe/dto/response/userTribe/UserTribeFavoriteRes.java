package com.umc.nuvibe.domain.tribe.dto.response.userTribe;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "트라이브 챗 즐겨찾기 응답")
public record UserTribeFavoriteRes(

        @Schema(description = "유저 트라이브 ID")
        Long userTribeId,

        @Schema(description = "트라이브 ID")
        Long tribeId,

        @Schema(description = "이미지 태그 (Enum)", example = "CAFE")
        ImageTag imageTag,

        @Schema(description = "트라이브 챗 고정 여부")
        Boolean isFavorite,

        @Schema(description = "유저 트라이브 상태")
        UserTribeStatus userTribeStatus
) {
    public static UserTribeFavoriteRes from(UserTribe userTribe) {
        return new UserTribeFavoriteRes(
                userTribe.getId(),
                userTribe.getTribe().getId(),
                userTribe.getTribe().getImageTag(),
                userTribe.isFavorite(),
                userTribe.getUserTribeStatus()
        );
    }
}
