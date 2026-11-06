package com.umc.nuvibe.domain.tribe.service.emoji;

import com.umc.nuvibe.domain.tribe.dto.internal.EmojiChanged;
import com.umc.nuvibe.domain.tribe.dto.internal.EmojiCountRow;
import com.umc.nuvibe.domain.tribe.entity.Emoji;
import com.umc.nuvibe.domain.tribe.repository.ChatRepository;
import com.umc.nuvibe.domain.tribe.repository.EmojiRepository;
import com.umc.nuvibe.domain.tribe.repository.UserTribeRepository;
import com.umc.nuvibe.domain.tribe.vo.EmojiType;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import com.umc.nuvibe.global.apiPayLoad.error.ChatErrorCode;
import com.umc.nuvibe.global.apiPayLoad.error.UserTribeErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmojiServiceImpl implements EmojiService {

    private static final String TRIBE_TOPIC_PREFIX = "/topic/tribe.";

    private static final String ACTION_CREATED = "CREATED";
    private static final String ACTION_UPDATED = "UPDATED";
    private static final String ACTION_DELETED = "DELETED";

    private final EmojiRepository emojiRepository;
    private final ChatRepository chatRepository;
    private final UserTribeRepository userTribeRepository;
    private final UserRepository userRepository;

    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    @Override
    @Transactional
    public void reactEmoji(Long userId, Long chatId, EmojiType type) {

        // 1. 채팅이 속한 트라이브 조회 및 반응 권한(ACTIVE 참여자) 검증
        Long tribeId = chatRepository.findTribeIdByChatId(chatId)
                .orElseThrow(() -> new BusinessException(ChatErrorCode.CHAT_NOT_FOUND));

        boolean canReact = userTribeRepository.existsByUser_IdAndTribe_IdAndUserTribeStatus(
                userId, tribeId, UserTribeStatus.ACTIVE
        );
        if (!canReact) {
            throw new BusinessException(UserTribeErrorCode.USERTRIBE_FORBIDDEN);
        }

        // 2. 기존 반응 여부에 따라 등록 / 취소 / 변경
        Optional<Emoji> existingEmoji = emojiRepository.findByChat_IdAndUser_Id(chatId, userId);

        String action;
        String actorEmojiType;

        if (existingEmoji.isEmpty()) {
            emojiRepository.save(Emoji.of(
                    type,
                    chatRepository.getReferenceById(chatId),
                    userRepository.getReferenceById(userId)
            ));
            action = ACTION_CREATED;
            actorEmojiType = type.name();

        } else if (existingEmoji.get().getType() == type) {
            emojiRepository.delete(existingEmoji.get());
            action = ACTION_DELETED;
            actorEmojiType = null;

        } else {
            existingEmoji.get().changeType(type);
            action = ACTION_UPDATED;
            actorEmojiType = type.name();
        }

        // 3. 커밋 이후 트라이브 채널로 이모지 집계 발행
        publishEmojiAfterCommit(tribeId, chatId, userId, action, actorEmojiType);
    }

    // 트랜잭션 커밋 이후 변경된 이모지 집계를 트라이브 채널로 발행
    private void publishEmojiAfterCommit(Long tribeId, Long chatId, Long userId, String action, String actorEmojiType) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Map<String, Long> summaryCounts = buildSummaryCounts(emojiRepository.countGroupByType(chatId));

                EmojiChanged emojiChanged = new EmojiChanged(
                        tribeId,
                        chatId,
                        userId,
                        action,
                        actorEmojiType,
                        summaryCounts,
                        LocalDateTime.now(clock)
                );

                try {
                    messagingTemplate.convertAndSend(TRIBE_TOPIC_PREFIX + tribeId, emojiChanged);
                } catch (MessagingException e) {
                    log.error("웹소켓 이모지 발행 실패. chatId={}, tribeId={}", chatId, tribeId, e);
                }
            }
        });
    }

    // 모든 이모지 타입을 0으로 채운 뒤 집계 결과 반영
    private Map<String, Long> buildSummaryCounts(List<EmojiCountRow> rows) {
        Map<String, Long> summaryCounts = new LinkedHashMap<>();
        for (EmojiType emojiType : EmojiType.values()) {
            summaryCounts.put(emojiType.name(), 0L);
        }
        for (EmojiCountRow row : rows) {
            summaryCounts.put(row.emojiType().name(), row.count());
        }
        return summaryCounts;
    }
}
