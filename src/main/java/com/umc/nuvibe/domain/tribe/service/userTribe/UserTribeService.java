package com.umc.nuvibe.domain.tribe.service.userTribe;

import com.umc.nuvibe.domain.tribe.dto.request.ActiveTribeListReq;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.*;

public interface UserTribeService {

    // 유저 트라이브 챗 활성화
    UserTribeActivateRes activateUserTribe(Long userId, Long userTribeId);

    // 트라이브 챗 즐겨찾기 토글
    UserTribeFavoriteRes toggleFavorite(Long userId, Long userTribeId);

    // 트라이브 챗 무음 토글
    UserTribeMuteRes toggleMute(Long userId, Long userTribeId);

    // 활성화된 트라이브 챗 목록 조회
    ActiveTribeListRes getActiveTribeList(Long userId, ActiveTribeListReq req);

    // 대기 중인 트라이브 챗 목록 조회
    WaitingTribeListRes getWaitingTribeList(Long userId, Long cursorTribeId, Integer size);
}
