package com.umc.nuvibe.domain.tribe.service.tribe;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.tribe.dto.request.TribeJoinReq;
import com.umc.nuvibe.domain.tribe.dto.response.tribe.TribeJoinRes;
import com.umc.nuvibe.domain.tribe.entity.Tribe;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import com.umc.nuvibe.domain.tribe.repository.TribeRepository;
import com.umc.nuvibe.domain.tribe.repository.UserTribeRepository;
import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import com.umc.nuvibe.global.apiPayLoad.error.TribeErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TribeServiceImpl implements TribeService {

    // 트라이브 챗 활성화가 가능해지는 최소 인원
    private static final int ACTIVATION_MIN_COUNT = 5;

    private final TribeRepository tribeRepository;
    private final UserTribeRepository userTribeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TribeJoinRes joinOrCreateTribe(Long userId, TribeJoinReq request) {
        ImageTag selectedTag = request.imageTag();

        // 1. 이미 해당 태그의 트라이브 챗에 입장해있는지 검증
        if (userTribeRepository.existsByUser_IdAndTribe_ImageTag(userId, selectedTag)) {
            throw new BusinessException(TribeErrorCode.ALREADY_JOINED);
        }

        // 2. 유저는 참조로 조회
        User userRef = userRepository.getReferenceById(userId);

        // 3. 인원 수가 100명 미만인 트라이브 챗 중 가장 오래된 버전 조회, 없을 시 새로운 버전 생성
        Tribe tribe = tribeRepository.findAvailableRoom(selectedTag)
                .orElseGet(() -> createNewVersionRoom(selectedTag));

        // 4. 트라이브 인원 수 검증 및 인원 수 1 증가
        if (tribe.isFull()) {
            throw new BusinessException(TribeErrorCode.TRIBE_FULL_RETRY);
        }
        tribe.incrementCounts();

        // 5. 유저 트라이브 생성 및 저장
        UserTribe userTribe = userTribeRepository.save(UserTribe.of(userRef, tribe));

        // 6. 최소 인원 도달 시 활성화 가능 상태로 전환
        if (tribe.getCounts() >= ACTIVATION_MIN_COUNT) {
            tribe.changeStatus();
        }

        return TribeJoinRes.from(tribe, userTribe);
    }

    // 새로운 버전의 트라이브 챗 생성
    private Tribe createNewVersionRoom(ImageTag imageTag) {
        int nextVersion = tribeRepository.findTopByImageTagOrderByVersionDesc(imageTag)
                .map(Tribe::getVersion)
                .map(v -> v + 1)
                .orElse(1);

        return tribeRepository.save(Tribe.create(imageTag, nextVersion));
    }
}
