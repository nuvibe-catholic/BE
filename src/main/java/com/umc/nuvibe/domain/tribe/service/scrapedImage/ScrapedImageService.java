package com.umc.nuvibe.domain.tribe.service.scrapedImage;

import com.umc.nuvibe.domain.tribe.dto.request.ScrapedImageSliceReq;
import com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageListRes;
import com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageToggleRes;

public interface ScrapedImageService {

    /**
     * 채팅 이미지 스크랩 토글
     * 스크랩이 없으면 생성, 이미 있으면 삭제
     */
    ScrapedImageToggleRes toggleScrapedImage(Long userId, Long chatId);

    /**
     * 스크랩한 전체 이미지 목록 조회 (최신순)
     * 태그 지정 시 태그별 조회
     */
    ScrapedImageListRes getTotalScrapedImage(Long userId, ScrapedImageSliceReq req);

    // 해당 트라이브 챗에서 스크랩한 이미지 목록 조회 (최신순)
    ScrapedImageListRes getTribeScrapedImage(Long userId, Long tribeId, ScrapedImageSliceReq req);
}
