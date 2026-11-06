package com.umc.nuvibe.domain.tribe.service.chat;

import com.umc.nuvibe.domain.tribe.dto.request.ChatGridReq;
import com.umc.nuvibe.domain.tribe.dto.request.ChatTimelineReq;
import com.umc.nuvibe.domain.tribe.dto.response.chat.ChatDetailRes;
import com.umc.nuvibe.domain.tribe.dto.response.chat.ChatGridListRes;
import com.umc.nuvibe.domain.tribe.dto.response.chat.ChatTimelineListRes;

public interface ChatService {

    /**
     * 트라이브 챗 내 채팅 타임라인 조회 (최신순)
     * 발신자 정보, 채팅별 이모지 요약, 내 이모지, 스크랩 여부 포함
     */
    ChatTimelineListRes getChatTimelineList(Long userId, Long tribeId, ChatTimelineReq req);

    // 트라이브 챗 내 채팅 이미지 그리드 목록 조회 (최신순)
    ChatGridListRes getChatGridList(Long tribeId, ChatGridReq req);

    // 채팅 이미지 상세 조회
    ChatDetailRes getChatDetail(Long userId, Long chatId);

    /**
     * 채팅 발신
     * 업로드 완료된 이미지를 선택한 보드에 저장한 뒤 트라이브 챗으로 발신
     */
    void sendChat(Long userId, Long tribeId, Long imageId, Long boardId);
}
