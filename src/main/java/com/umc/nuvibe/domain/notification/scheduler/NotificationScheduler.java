package com.umc.nuvibe.domain.notification.scheduler;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.notification.service.FcmService;
import com.umc.nuvibe.domain.notification.vo.NotificationType;
import com.umc.nuvibe.domain.tribe.repository.TribeRepository;
import com.umc.nuvibe.domain.tribe.repository.UserTribeRepository;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    // 트라이브 챗 종료 기준 (TribeCloseRunner 와 동일)
    private static final int MIN_ACTIVE_COUNT = 5;
    private static final long CLOSE_GRACE_HOURS = 168L;
    // 종료 예고 시점 (종료 24시간 전)
    private static final long WARNING_HOURS = CLOSE_GRACE_HOURS - 24L;
    // 예고 배치 주기, 한 트라이브가 한 번만 걸리도록 조회 구간을 주기와 맞춘다
    private static final long WARNING_BATCH_INTERVAL_HOURS = 12L;
    private static final int PAGE_SIZE = 100;

    private final TribeRepository tribeRepository;
    private final UserTribeRepository userTribeRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final Clock clock;

    // NOTI-04: 트라이브 챗 종료 예고 (매일 0시, 12시)
    // 생성 후 6일이 지났고 활성 인원이 5명 미만인 트라이브 참여자 대상
    @Scheduled(cron = "0 0 0/12 * * *", zone = "Asia/Seoul")
    public void sendTribeCloseWarning() {
        // 같은 시각 cron 인 종료 배치 뒤에 실행되어 지연돼도 조회 구간이 밀리지 않도록 12시간 슬롯 경계로 내림
        LocalDateTime now = truncateToBatchSlot(LocalDateTime.now(clock));
        LocalDateTime cutoff = now.minusHours(WARNING_HOURS);
        LocalDateTime excludeCutoff = cutoff.minusHours(WARNING_BATCH_INTERVAL_HOURS);

        forEachPage(
                pageable -> tribeRepository.findCloseWarningTargets(
                        cutoff, excludeCutoff, UserTribeStatus.ACTIVE, MIN_ACTIVE_COUNT, pageable),
                target -> {
                    try {
                        String tag = target.getImageTag() != null ? target.getImageTag().name() : null;
                        fcmService.sendNotificationToUsers(
                                userTribeRepository.findUserIdsByTribeIdAndStatus(target.getTribeId(), UserTribeStatus.ACTIVE),
                                NotificationType.NOTI_04,
                                tag,
                                target.getTribeId(),
                                null
                        );
                    } catch (Exception e) {
                        log.error("NOTI-04 발송 실패. tribeId={}", target.getTribeId(), e);
                    }
                }
        );
    }

    // NOTI-06: 드랍 미션 리마인드 (매일 18시)
    // 오늘 드랍하지 않은 유저 대상
    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Seoul")
    public void sendDropReminder() {
        LocalDateTime todayStart = LocalDate.now(clock).atStartOfDay();

        forEachUser(
                pageable -> userRepository.findUserIdsWithoutDropSince(todayStart, pageable),
                userId -> fcmService.sendNotification(userId, NotificationType.NOTI_06, null, null, null),
                NotificationType.NOTI_06
        );
    }

    // NOTI-07: 추천 태그 알림 (매주 수, 토 15시)
    // 최근 3일간 드랍하지 않은 유저 대상, 유저별 랜덤 태그
    @Scheduled(cron = "0 0 15 * * WED,SAT", zone = "Asia/Seoul")
    public void sendTagRecommendation() {
        LocalDateTime threeDaysAgo = LocalDateTime.now(clock).minusDays(3);
        ImageTag[] tags = ImageTag.values();

        forEachUser(
                pageable -> userRepository.findUserIdsWithoutDropSince(threeDaysAgo, pageable),
                userId -> {
                    ImageTag tag = tags[ThreadLocalRandom.current().nextInt(tags.length)];
                    fcmService.sendNotification(userId, NotificationType.NOTI_07, tag.name(), null, null);
                },
                NotificationType.NOTI_07
        );
    }

    // NOTI-08: 주간 리캡 알림 (매주 일요일 15시)
    // 지난 7일간 드랍한 유저 대상
    @Scheduled(cron = "0 0 15 * * SUN", zone = "Asia/Seoul")
    public void sendWeeklyRecap() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime start = today.minusWeeks(1).atStartOfDay();
        LocalDateTime end = today.atStartOfDay();

        forEachUser(
                pageable -> userRepository.findUserIdsWithDropBetween(start, end, pageable),
                userId -> fcmService.sendNotification(userId, NotificationType.NOTI_08, null, null, null),
                NotificationType.NOTI_08
        );
    }

    // NOTI-09: 전체 리캡 알림 (매월 1일 18시)
    // 지난달 드랍한 유저 대상
    @Scheduled(cron = "0 0 18 1 * *", zone = "Asia/Seoul")
    public void sendMonthlyRecap() {
        LocalDate thisMonth = LocalDate.now(clock).withDayOfMonth(1);
        LocalDateTime start = thisMonth.minusMonths(1).atStartOfDay();
        LocalDateTime end = thisMonth.atStartOfDay();

        forEachUser(
                pageable -> userRepository.findUserIdsWithDropBetween(start, end, pageable),
                userId -> fcmService.sendNotification(userId, NotificationType.NOTI_09, null, null, null),
                NotificationType.NOTI_09
        );
    }

    private static LocalDateTime truncateToBatchSlot(LocalDateTime time) {
        int slotHour = (int) (time.getHour() / WARNING_BATCH_INTERVAL_HOURS * WARNING_BATCH_INTERVAL_HOURS);
        return time.toLocalDate().atTime(slotHour, 0);
    }

    // 유저 단위 발송, 한 명이 실패해도 나머지는 계속 발송
    private void forEachUser(Function<Pageable, Slice<Long>> query,
                             Consumer<Long> sender,
                             NotificationType type) {
        forEachPage(query, userId -> {
            try {
                sender.accept(userId);
            } catch (Exception e) {
                log.error("{} 발송 실패. userId={}", type.getCode(), userId, e);
            }
        });
    }

    private <T> void forEachPage(Function<Pageable, Slice<T>> query, Consumer<T> action) {
        int page = 0;
        Slice<T> slice;
        do {
            slice = query.apply(PageRequest.of(page++, PAGE_SIZE));
            slice.forEach(action);
        } while (slice.hasNext());
    }
}
