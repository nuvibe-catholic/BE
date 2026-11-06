package com.umc.nuvibe.domain.user.repository;


import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.domain.user.service.AuthService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findById(Long userId);
    boolean existsByEmail(String email);

    // 알림 대상: 특정 시각 이후 드랍(보드에 이미지 저장)이 없는 유저
    // 탈퇴로 board.user 가 null 인 행이 있어도 누락되지 않도록 NOT IN 대신 NOT EXISTS 사용
    @Query("""
        select u.id from User u
        where not exists (
            select 1 from BoardImage bi
            where bi.board.user = u
              and bi.image.createdAt >= :since
        )
        order by u.id
    """)
    Slice<Long> findUserIdsWithoutDropSince(@Param("since") LocalDateTime since, Pageable pageable);

    // 알림 대상: 기간 내 드랍이 있는 유저
    @Query("""
        select u.id from User u
        where exists (
            select 1 from BoardImage bi
            where bi.board.user = u
              and bi.image.createdAt >= :start
              and bi.image.createdAt < :end
        )
        order by u.id
    """)
    Slice<Long> findUserIdsWithDropBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

}
