package com.umc.nuvibe.domain.tribe.dto.request;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "트라이브 챗 입장 요청")
public record TribeJoinReq(

        @Schema(description = "입장할 트라이브 챗의 이미지 태그", example = "CAFE")
        @NotNull
        ImageTag imageTag
) {
}
