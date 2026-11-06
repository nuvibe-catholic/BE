package com.umc.nuvibe.domain.tribe.service.scrapedImage;

import com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageToggleRes;

public interface ScrapedImageService {

    /**
     * 채팅 이미지 스크랩 토글
     * 스크랩이 없으면 생성, 이미 있으면 삭제
     */
    ScrapedImageToggleRes toggleScrapedImage(Long userId, Long chatId);
}
