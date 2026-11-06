package com.umc.nuvibe.domain.tribe.service.internal;

import com.umc.nuvibe.domain.tribe.dto.internal.CloseTargetView;
import com.umc.nuvibe.domain.tribe.repository.TribeRepository;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TribeCloseRunner {

    // 활성화 인원이 5명 미만이면 종료 대상
    private static final int MIN_ACTIVE_COUNT = 5;
    // 1회 조회 시 최대 처리 개수
    private static final int MAX_PROCESS_LIMIT = 100;
    // 생성 후 종료까지 유예 시간 (7일)
    private static final long GRACE_HOURS = 168L;

    private final TribeRepository tribeRepository;
    private final TribeCloseProcessor processor;

    public void run(LocalDateTime batchNow) {

        LocalDateTime cutoff = batchNow.minusHours(GRACE_HOURS);
        Set<Long> failedTribeIds = new HashSet<>();

        while (true) {

            // 처리된 트라이브는 삭제되므로 항상 첫 페이지만 조회
            Slice<CloseTargetView> closeTargets = tribeRepository.findCloseTargets(
                    cutoff,
                    UserTribeStatus.ACTIVE,
                    MIN_ACTIVE_COUNT,
                    PageRequest.of(0, MAX_PROCESS_LIMIT)
            );

            if (closeTargets.isEmpty()) {
                break;
            }

            int closedCount = 0;
            for (CloseTargetView target : closeTargets) {
                Long tribeId = target.getTribeId();
                if (failedTribeIds.contains(tribeId)) {
                    continue;
                }

                try {
                    processor.processTribeClose(tribeId);
                    closedCount++;
                } catch (Exception e) {
                    // 실패한 트라이브는 건너뛰고 다음 배치에서 재시도
                    failedTribeIds.add(tribeId);
                    log.error("트라이브 챗 종료 실패. tribeId={}", tribeId, e);
                }
            }

            // 조회된 대상이 모두 실패한 트라이브면 무한 반복 방지를 위해 종료
            if (closedCount == 0) {
                break;
            }
        }
    }
}
