package com.umc.nuvibe.domain.tribe.dto.response.scrapedImage;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "스크랩 이미지 목록 아이템")
public record ScrapedImageItemRes(

        @Schema(description = "스크랩 이미지 ID")
        Long scrapImageId,

        @Schema(description = "이미지 ID")
        Long imageId,

        @Schema(description = "이미지 썸네일 URL (썸네일이 없으면 원본 URL)")
        String imageUrl,

        @Schema(description = "이미지 태그 (Enum)", example = "CAFE")
        ImageTag imageTag,

        @Schema(description = "스크랩 일시 (정렬/날짜 섹션 기준)")
        LocalDateTime createdAt,

        @Schema(description = "채팅 ID")
        Long chatId,

        @Schema(description = "채팅 작성자 ID")
        Long chatSenderId,

        @Schema(description = "채팅 작성자 닉네임")
        String chatSenderNickname
) {
}
