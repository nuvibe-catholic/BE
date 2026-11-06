package com.umc.nuvibe.domain.tribe.repository;

import com.umc.nuvibe.domain.tribe.dto.internal.EmojiAggRow;
import com.umc.nuvibe.domain.tribe.dto.internal.EmojiCountRow;
import com.umc.nuvibe.domain.tribe.dto.internal.MyEmojiRow;
import com.umc.nuvibe.domain.tribe.entity.Emoji;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmojiRepository extends JpaRepository<Emoji, Long> {

    // 채팅에 내가 누른 이모지 조회
    Optional<Emoji> findByChat_IdAndUser_Id(Long chatId, Long userId);

    // 단일 채팅의 이모지 타입별 집계
    @Query("""
        select new com.umc.nuvibe.domain.tribe.dto.internal.EmojiCountRow(
            e.type,
            count(e.id)
        )
        from Emoji e
        where e.chat.id = :chatId
        group by e.type
    """)
    List<EmojiCountRow> countGroupByType(@Param("chatId") Long chatId);

    /**
     * 각 채팅에 대한 이모지 타입별 집계
     * count가 0인 이모지 타입은 결과에 포함되지 않음
     */
    @Query("""
        select new com.umc.nuvibe.domain.tribe.dto.internal.EmojiAggRow(
            e.chat.id,
            e.type,
            count(e.id)
        )
        from Emoji e
        where e.chat.id in :chatIds
        group by e.chat.id, e.type
    """)
    List<EmojiAggRow> countByChatIdGroupByType(@Param("chatIds") List<Long> chatIds);

    /**
     * 내가 누른 이모지 조회
     * 이모지는 종류와 관계없이 chatId당 최대 1건
     */
    @Query("""
        select new com.umc.nuvibe.domain.tribe.dto.internal.MyEmojiRow(
            e.chat.id,
            e.type
        )
        from Emoji e
        where e.user.id = :userId
          and e.chat.id in :chatIds
    """)
    List<MyEmojiRow> findMyEmoji(
            @Param("userId") Long userId,
            @Param("chatIds") List<Long> chatIds
    );
}
