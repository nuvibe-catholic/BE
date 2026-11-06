package com.umc.nuvibe.domain.tribe.repository;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageItemRes;
import com.umc.nuvibe.domain.tribe.entity.ScrapedImage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * 내 스크랩 이미지 목록 첫 페이지 조회 (최신순)
     * imageTag가 null이면 전체, tribeId가 null이면 모든 트라이브 챗 대상
     */
    @Query("""
        select new com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageItemRes(
            si.id, img.id, coalesce(img.thumbnailUrl, img.imageUrl), img.imageTag, si.createdAt,
            c.id, c.user.id, c.user.nickname
        )
        from ScrapedImage si
        join si.image img
        join Chat c on c.image.id = img.id
        where si.user.id = :userId
          and (:imageTag is null or img.imageTag = :imageTag)
          and (:tribeId is null or si.tribe.id = :tribeId)
        order by si.createdAt desc, si.id desc
    """)
    List<ScrapedImageItemRes> findMyScrapsFirstPage(
            @Param("userId") Long userId,
            @Param("tribeId") Long tribeId,
            @Param("imageTag") ImageTag imageTag,
            Pageable pageable
    );

    // 내 스크랩 이미지 목록 다음 페이지 조회 (복합 커서: createdAt, scrapImageId)
    @Query("""
        select new com.umc.nuvibe.domain.tribe.dto.response.scrapedImage.ScrapedImageItemRes(
            si.id, img.id, coalesce(img.thumbnailUrl, img.imageUrl), img.imageTag, si.createdAt,
            c.id, c.user.id, c.user.nickname
        )
        from ScrapedImage si
        join si.image img
        join Chat c on c.image.id = img.id
        where si.user.id = :userId
          and (:imageTag is null or img.imageTag = :imageTag)
          and (:tribeId is null or si.tribe.id = :tribeId)
          and (
                si.createdAt < :cursorCreatedAt
                or (si.createdAt = :cursorCreatedAt and si.id < :cursorId)
          )
        order by si.createdAt desc, si.id desc
    """)
    List<ScrapedImageItemRes> findMyScrapsNextPage(
            @Param("userId") Long userId,
            @Param("tribeId") Long tribeId,
            @Param("imageTag") ImageTag imageTag,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
