package com.umc.nuvibe.domain.tribe.service.tribe;

import com.umc.nuvibe.domain.tribe.dto.request.TribeJoinReq;
import com.umc.nuvibe.domain.tribe.dto.response.tribe.TribeJoinRes;

public interface TribeService {

    /**
     * 트라이브 챗 입장
     * 입장 가능한 트라이브 챗이 없을 시 새로운 버전의 트라이브 챗 생성 후 입장
     */
    TribeJoinRes joinOrCreateTribe(Long userId, TribeJoinReq request);
}
