package com.umc.nuvibe.domain.tribe.repository;

import com.umc.nuvibe.domain.tribe.entity.ScrapedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScrapedImageRepository extends JpaRepository<ScrapedImage, Long> {

    // 주어진 이미지 중 해당 트라이브 챗에서 내가 스크랩한 imageId 목록 조회
    @Query("""
        select si.image.id
        from ScrapedImage si
        where si.user.id = :userId
          and si.tribe.id = :tribeId
          and si.image.id in :imageIds
    """)
    List<Long> findImageIdsByUserIdAndTribeIdAndImageIds(
            @Param("userId") Long userId,
            @Param("tribeId") Long tribeId,
            @Param("imageIds") List<Long> imageIds
    );
}
