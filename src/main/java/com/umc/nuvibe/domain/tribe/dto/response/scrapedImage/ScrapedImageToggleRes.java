package com.umc.nuvibe.domain.tribe.dto.response.scrapedImage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 스크랩 토글 응답")
public record ScrapedImageToggleRes(

        @Schema(description = "스크랩 시 생성되는 스크랩 이미지 ID (스크랩 취소 시 null)")
        Long scrapedImageId
) {
}
