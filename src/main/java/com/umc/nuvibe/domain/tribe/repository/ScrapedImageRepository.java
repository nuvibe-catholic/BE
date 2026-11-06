package com.umc.nuvibe.domain.tribe.repository;

import com.umc.nuvibe.domain.tribe.entity.ScrapedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScrapedImageRepository extends JpaRepository<ScrapedImage, Long> {

    // 해당 트라이브 챗에서 스크랩한 이미지 단건 조회
    Optional<ScrapedImage> findByUser_IdAndTribe_IdAndImage_Id(Long userId, Long tribeId, Long imageId);

    // 유저의 이미지 스크랩 여부
    boolean existsByUser_IdAndImage_Id(Long userId, Long imageId);

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
