package com.umc.nuvibe.domain.tribe.controller;

import com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageToggleRes;
import com.umc.nuvibe.domain.tribe.service.scrapedImage.ScrapedImageService;
import com.umc.nuvibe.global.apiPayLoad.response.Response;
import com.umc.nuvibe.global.apiPayLoad.result.ScrapedImageResultCode;
import com.umc.nuvibe.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scrapedImage")
@Tag(name = "ScrapedImage", description = "스크랩 이미지 API")
public class ScrapedImageController {

    private final ScrapedImageService scrapedImageService;

    @PostMapping("/chat/{chatId}")
    @Operation(summary = "이미지 스크랩 토글", description = "기존 스크랩 존재 시 삭제, 부재 시 새로운 스크랩 생성")
    public Response<ScrapedImageToggleRes> toggleScrapedImage(
            @AuthUser Long userId,
            @PathVariable Long chatId
    ) {
        ScrapedImageToggleRes res = scrapedImageService.toggleScrapedImage(userId, chatId);

        // scrapedImageId가 있으면 스크랩 생성, 없으면 스크랩 취소
        ScrapedImageResultCode resultCode = (res.scrapedImageId() != null)
                ? ScrapedImageResultCode.SCRAPEDIMAGE_CREATED
                : ScrapedImageResultCode.SCRAPEDIMAGE_DELETED;

        return Response.of(resultCode, res);
    }
}
