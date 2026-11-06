package com.umc.nuvibe.domain.tribe.dto.response.tribe;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.tribe.entity.Tribe;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import com.umc.nuvibe.domain.tribe.vo.TribeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "트라이브 챗 입장 응답")
public record TribeJoinRes(

        @Schema(description = "트라이브 ID")
        Long tribeId,

        @Schema(description = "유저 트라이브 ID")
        Long userTribeId,

        @Schema(description = "이미지 태그 (Enum)", example = "CAFE")
        ImageTag imageTag,

        @Schema(description = "트라이브 챗 버전")
        Integer version,

        @Schema(description = "트라이브 챗 상태")
        TribeStatus tribeStatus,

        @Schema(description = "트라이브 챗 참여자 수")
        Integer counts,

        @Schema(description = "입장 시각")
        LocalDateTime joinedAt
) {
    public static TribeJoinRes from(Tribe tribe, UserTribe userTribe) {
        return new TribeJoinRes(
                tribe.getId(),
                userTribe.getId(),
                tribe.getImageTag(),
                tribe.getVersion(),
                tribe.getStatus(),
                tribe.getCounts(),
                userTribe.getCreatedAt()
        );
    }
}
