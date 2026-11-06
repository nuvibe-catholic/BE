package com.umc.nuvibe.domain.tribe.service.internal;

import com.umc.nuvibe.domain.notification.event.NotificationEvent;
import com.umc.nuvibe.domain.notification.vo.NotificationType;
import com.umc.nuvibe.domain.tribe.entity.Tribe;
import com.umc.nuvibe.domain.tribe.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TribeCloseProcessor {

    private final ChatRepository chatRepository;
    private final EmojiRepository emojiRepository;
    private final ScrapedImageRepository scrapedImageRepository;
    private final UserTribeRepository userTribeRepository;
    private final TribeRepository tribeRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 트라이브 챗 종료 (하드 삭제)
     * 트라이브 단위로 커밋해 하나가 실패해도 다른 트라이브 처리에 영향이 없도록 REQUIRES_NEW 사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTribeClose(Long tribeId) {

        // 삭제 전에 종료 알림 대상(전체 참여자)과 태그를 먼저 조회
        String tag = tribeRepository.findById(tribeId)
                .map(Tribe::getImageTag)
                .map(Enum::name)
                .orElse(null);
        List<Long> participantIds = userTribeRepository.findUserIdsByTribeId(tribeId);

        // FK 참조 순서대로 삭제: 이모지 -> 스크랩 이미지 -> 채팅 -> 유저 트라이브 -> 트라이브
        List<Long> chatIds = chatRepository.findIdsByTribeId(tribeId);
        if (!chatIds.isEmpty()) {
            emojiRepository.deleteByChatIds(chatIds);
        }

        scrapedImageRepository.deleteByTribeId(tribeId);
        chatRepository.deleteByTribeId(tribeId);
        userTribeRepository.deleteByTribeId(tribeId);
        tribeRepository.deleteById(tribeId);

        // 커밋 이후 참여자 전원에게 종료 알림 (NOTI-05)
        eventPublisher.publishEvent(NotificationEvent.of(
                participantIds,
                NotificationType.NOTI_05,
                tag,
                tribeId,
                null
        ));
    }
}
