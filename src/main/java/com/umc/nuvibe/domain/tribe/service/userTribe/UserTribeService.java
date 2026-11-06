package com.umc.nuvibe.domain.tribe.service.userTribe;

import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeActivateRes;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeFavoriteRes;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeMuteRes;

public interface UserTribeService {

    // 유저 트라이브 챗 활성화
    UserTribeActivateRes activateUserTribe(Long userId, Long userTribeId);

    // 트라이브 챗 즐겨찾기 토글
    UserTribeFavoriteRes toggleFavorite(Long userId, Long userTribeId);

    // 트라이브 챗 무음 토글
    UserTribeMuteRes toggleMute(Long userId, Long userTribeId);
}
