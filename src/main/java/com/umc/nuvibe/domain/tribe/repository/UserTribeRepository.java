package com.umc.nuvibe.domain.tribe.repository;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.tribe.dto.internal.ActiveTribeRow;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.WaitingTribeItemRes;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserTribeRepository extends JpaRepository<UserTribe, Long> {

    // 해당 태그의 트라이브 챗 입장 여부
    boolean existsByUser_IdAndTribe_ImageTag(Long userId, ImageTag imageTag);

    // 해당 트라이브 챗 입장 여부
    boolean existsByUser_IdAndTribe_Id(Long userId, Long tribeId);

    // 해당 트라이브 챗에 특정 상태로 참여 중인지 여부
    boolean existsByUser_IdAndTribe_IdAndUserTribeStatus(Long userId, Long tribeId, UserTribeStatus userTribeStatus);

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

    // 채팅 발신 시 트라이브 챗 참여자들의 마지막 활동 시각 갱신
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserTribe ut
           set ut.lastActivityAt = :activityAt
         where ut.tribe.id = :tribeId
           and ut.userTribeStatus = :status
    """)
    int updateLastActivityAt(
            @Param("tribeId") Long tribeId,
            @Param("status") UserTribeStatus status,
            @Param("activityAt") LocalDateTime activityAt
    );

    // 채팅 발신 시 발신자를 제외한 ACTIVE 참여자의 안 읽은 메시지 수 1 증가
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserTribe ut
           set ut.unreadCount = ut.unreadCount + 1
         where ut.tribe.id = :tribeId
           and ut.userTribeStatus = :activeStatus
           and ut.user.id <> :senderUserId
    """)
    int incrementUnreadCountForActiveMembers(
            @Param("tribeId") Long tribeId,
            @Param("activeStatus") UserTribeStatus activeStatus,
            @Param("senderUserId") Long senderUserId
    );

    // 트라이브 챗 종료 시 참여 정보 일괄 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserTribe ut where ut.tribe.id = :tribeId")
    int deleteByTribeId(@Param("tribeId") Long tribeId);

    // 트라이브 챗 읽음 처리 (마지막 읽은 chatId 갱신 및 안 읽은 메시지 수 초기화)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserTribe ut
           set ut.lastReadChatId = :lastChatId,
               ut.unreadCount = 0
         where ut.user.id = :userId
           and ut.tribe.id = :tribeId
           and ut.userTribeStatus = :status
           and (ut.lastReadChatId is null or ut.lastReadChatId < :lastChatId)
    """)
    int readChat(
            @Param("userId") Long userId,
            @Param("tribeId") Long tribeId,
            @Param("lastChatId") Long lastChatId,
            @Param("status") UserTribeStatus status
    );

    // 무음 설정 여부 (이모지 반응 알림 대상 판단)
    boolean existsByUser_IdAndTribe_IdAndIsMutedTrue(Long userId, Long tribeId);

    // 알림 대상: 트라이브 챗 참여자 ID (상태별)
    @Query("select ut.user.id from UserTribe ut where ut.tribe.id = :tribeId and ut.userTribeStatus = :status")
    List<Long> findUserIdsByTribeIdAndStatus(@Param("tribeId") Long tribeId, @Param("status") UserTribeStatus status);

    // 알림 대상: 트라이브 챗 전체 참여자 ID (ACTIVE + WAITING)
    @Query("select ut.user.id from UserTribe ut where ut.tribe.id = :tribeId")
    List<Long> findUserIdsByTribeId(@Param("tribeId") Long tribeId);

    // 알림 대상: 무음이 아닌 ACTIVE 참여자 ID (발신자 제외)
    @Query("""
        select ut.user.id
          from UserTribe ut
         where ut.tribe.id = :tribeId
           and ut.userTribeStatus = :status
           and ut.isMuted = false
           and ut.user.id <> :excludeUserId
    """)
    List<Long> findUnmutedUserIdsExcept(
            @Param("tribeId") Long tribeId,
            @Param("status") UserTribeStatus status,
            @Param("excludeUserId") Long excludeUserId
    );
}
