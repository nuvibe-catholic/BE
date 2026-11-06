package com.umc.nuvibe.domain.tribe.service.internal;

import com.umc.nuvibe.domain.tribe.repository.*;
import lombok.RequiredArgsConstructor;
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

    /**
     * 트라이브 챗 종료 (하드 삭제)
     * 트라이브 단위로 커밋해 하나가 실패해도 다른 트라이브 처리에 영향이 없도록 REQUIRES_NEW 사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processTribeClose(Long tribeId) {

        // FK 참조 순서대로 삭제: 이모지 -> 스크랩 이미지 -> 채팅 -> 유저 트라이브 -> 트라이브
        List<Long> chatIds = chatRepository.findIdsByTribeId(tribeId);
        if (!chatIds.isEmpty()) {
            emojiRepository.deleteByChatIds(chatIds);
        }

        scrapedImageRepository.deleteByTribeId(tribeId);
        chatRepository.deleteByTribeId(tribeId);
        userTribeRepository.deleteByTribeId(tribeId);
        tribeRepository.deleteById(tribeId);
    }
}
