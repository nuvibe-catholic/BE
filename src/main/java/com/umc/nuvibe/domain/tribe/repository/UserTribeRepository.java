package com.umc.nuvibe.domain.tribe.repository;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.tribe.dto.internal.ActiveTribeRow;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.WaitingTribeItemRes;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserTribeRepository extends JpaRepository<UserTribe, Long> {

    // 해당 태그의 트라이브 챗 입장 여부
    boolean existsByUser_IdAndTribe_ImageTag(Long userId, ImageTag imageTag);

    /**
     * Active 트라이브 챗 목록 조회
     * 고정 여부, 마지막 활동 시각, 안 읽은 메시지 여부, 마지막 chatId 순으로 정렬
     */
    @Query("""
        select new com.umc.nuvibe.domain.tribe.dto.internal.ActiveTribeRow(
            t.id,
            ut.id,
            t.imageTag,
            t.counts,
            ut.isFavorite,
            ut.lastActivityAt,
            ut.unreadCount,
            t.lastChatId
        )
        from UserTribe ut
        join ut.tribe t
        where ut.user.id = :userId
          and ut.userTribeStatus = :status
          and (
            :hasCursor = false
            or (case when ut.isFavorite = true then 1 else 0 end) < :cFav
            or ((case when ut.isFavorite = true then 1 else 0 end) = :cFav
                and ut.lastActivityAt < :cActivityAt)
            or ((case when ut.isFavorite = true then 1 else 0 end) = :cFav
                and ut.lastActivityAt = :cActivityAt
                and (case when ut.unreadCount > 0 then 1 else 0 end) < :cUnread)
            or ((case when ut.isFavorite = true then 1 else 0 end) = :cFav
                and ut.lastActivityAt = :cActivityAt
                and (case when ut.unreadCount > 0 then 1 else 0 end) = :cUnread
                and coalesce(t.lastChatId, 0) < :cLastChatId)
          )
        order by
            ut.isFavorite desc,
            ut.lastActivityAt desc,
            (case when ut.unreadCount > 0 then 1 else 0 end) desc,
            coalesce(t.lastChatId, 0) desc
    """)
    List<ActiveTribeRow> findActiveTribes(
            @Param("userId") Long userId,
            @Param("status") UserTribeStatus status,
            @Param("hasCursor") boolean hasCursor,
            @Param("cFav") int cFav,
            @Param("cActivityAt") LocalDateTime cActivityAt,
            @Param("cUnread") int cUnread,
            @Param("cLastChatId") long cLastChatId,
            Pageable pageable
    );

    // Waiting 트라이브 챗 목록 조회 (tribeId 내림차순)
    @Query("""
        select new com.umc.nuvibe.domain.tribe.dto.response.userTribe.WaitingTribeItemRes(
            t.id,
            ut.id,
            t.imageTag,
            t.counts
        )
        from UserTribe ut
        join ut.tribe t
        where ut.user.id = :userId
          and ut.userTribeStatus = :status
          and (:hasCursor = false or t.id < :cTribeId)
        order by t.id desc
    """)
    List<WaitingTribeItemRes> findWaitingTribes(
            @Param("userId") Long userId,
            @Param("status") UserTribeStatus status,
            @Param("hasCursor") boolean hasCursor,
            @Param("cTribeId") Long cTribeId,
            Pageable pageable
    );
}
