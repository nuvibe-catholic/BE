package com.umc.nuvibe.domain.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.nuvibe.domain.archive.entity.ArchiveBoard;
import com.umc.nuvibe.domain.archive.entity.BoardImage;
import com.umc.nuvibe.domain.archive.repository.ArchiveBoardRepository;
import com.umc.nuvibe.domain.archive.repository.BoardImageRepository;
import com.umc.nuvibe.domain.image.entity.Image;
import com.umc.nuvibe.domain.image.repository.ImageRepository;
import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.notification.entity.Fcm;
import com.umc.nuvibe.domain.notification.entity.Notification;
import com.umc.nuvibe.domain.notification.repository.FcmRepository;
import com.umc.nuvibe.domain.notification.repository.NotificationRepository;
import com.umc.nuvibe.domain.notification.vo.NotificationType;
import com.umc.nuvibe.domain.tribe.dto.request.TribeJoinReq;
import com.umc.nuvibe.domain.tribe.service.tribe.TribeService;
import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import com.umc.nuvibe.domain.user.vo.AuthProvider;
import com.umc.nuvibe.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired FcmRepository fcmRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ImageRepository imageRepository;
    @Autowired ArchiveBoardRepository archiveBoardRepository;
    @Autowired BoardImageRepository boardImageRepository;
    @Autowired TribeService tribeService;

    @Test
    void 같은_기기_토큰을_다른_계정이_등록하면_기존_계정_토큰은_비활성화된다() throws Exception {
        User first = saveUser();
        User second = saveUser();
        String deviceToken = "device-" + UUID.randomUUID();

        registerToken(first, deviceToken);
        registerToken(second, deviceToken);

        assertThat(fcmRepository.findByUser_IdAndIsActiveTrue(first.getId())).isEmpty();
        assertThat(fcmRepository.findByUser_IdAndIsActiveTrue(second.getId()))
                .extracting(Fcm::getToken)
                .containsExactly(deviceToken);

        // 로그아웃하면 남은 토큰도 비활성화
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(second)))
                .andExpect(status().isOk());
        assertThat(fcmRepository.findByUser_IdAndIsActiveTrue(second.getId())).isEmpty();
    }

    @Test
    void 본인_알림만_조회_읽음_삭제할_수_있다() throws Exception {
        User me = saveUser();
        User other = saveUser();
        Notification mine = notificationRepository.save(Notification.of(me, NotificationType.NOTI_02, "Blur", 1L, null));
        Notification others = notificationRepository.save(Notification.of(other, NotificationType.NOTI_02, "Blur", 1L, null));

        mvc.perform(get("/api/notifications").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTI001"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("TRIBE_CHAT_IMAGE_UPLOADED"))
                .andExpect(jsonPath("$.data[0].mainMessage").value("트라이브 챗에 새 바이브가 올라왔어요"))
                .andExpect(jsonPath("$.data[0].actionMessage").value("#Blur 트라이브 챗을 지금 확인해 보세요!"));

        // 타인 알림은 존재 여부를 드러내지 않고 404
        mvc.perform(patch("/api/notifications/{id}/read", others.getId()).header("Authorization", bearer(me)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTI_ERR001"));
        mvc.perform(delete("/api/notifications/{id}", others.getId()).header("Authorization", bearer(me)))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/notifications/{id}/read", mine.getId()).header("Authorization", bearer(me)))
                .andExpect(status().isOk());
        assertThat(notificationRepository.findById(mine.getId()).orElseThrow().isRead()).isTrue();

        mvc.perform(delete("/api/notifications/{id}", mine.getId()).header("Authorization", bearer(me)))
                .andExpect(status().isOk());
        assertThat(notificationRepository.findById(mine.getId())).isEmpty();
        assertThat(notificationRepository.findById(others.getId())).isPresent();
    }

    @Test
    void 트라이브_챗이_5명이_되는_순간에만_기존_대기자에게_오픈_알림이_간다() throws Exception {
        ImageTag tag = ImageTag.values()[ImageTag.values().length - 1];
        List<User> members = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            User user = saveUser();
            members.add(user);
            tribeService.joinOrCreateTribe(user.getId(), new TribeJoinReq(tag));
        }

        // 5번째 입장 시 앞선 4명에게만 발송 (비동기라 잠시 대기)
        for (User waiter : members.subList(0, 4)) {
            assertThat(awaitNotifications(waiter, 1)).hasSize(1)
                    .first()
                    .satisfies(n -> {
                        assertThat(n.getType()).isEqualTo(NotificationType.NOTI_01);
                        assertThat(n.getMainMessage()).isEqualTo("기다리던 트라이브 챗이 열렸어요");
                    });
        }

        // 5번째(본인)와 이미 열린 뒤 들어온 6번째는 받지 않는다
        Thread.sleep(300);
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(members.get(4).getId())).isEmpty();
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(members.get(5).getId())).isEmpty();
        // 6번째 입장으로 기존 대기자에게 중복 발송되지 않는다
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(members.get(0).getId())).hasSize(1);
    }

    @Test
    void 탈퇴한_유저의_보드가_있어도_드랍하지_않은_유저가_조회된다() {
        User dropped = saveUser();
        User notDropped = saveUser();
        LocalDateTime since = LocalDateTime.now().minusMinutes(1);

        saveDrop(dropped);
        // 탈퇴 등으로 user_id 가 null 이 된 보드의 드랍 (NOT IN 쿼리였다면 전체가 누락되는 케이스)
        saveDrop(null);

        List<Long> withoutDrop = userRepository.findUserIdsWithoutDropSince(since, PageRequest.of(0, 1000)).getContent();
        assertThat(withoutDrop).contains(notDropped.getId()).doesNotContain(dropped.getId());

        List<Long> withDrop = userRepository.findUserIdsWithDropBetween(since, LocalDateTime.now().plusMinutes(1), PageRequest.of(0, 1000)).getContent();
        assertThat(withDrop).contains(dropped.getId()).doesNotContain(notDropped.getId());
    }

    private List<Notification> awaitNotifications(User user, int expected) throws InterruptedException {
        List<Notification> found = List.of();
        for (int i = 0; i < 50; i++) {
            found = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            if (found.size() >= expected) {
                break;
            }
            Thread.sleep(100);
        }
        return found;
    }

    private void registerToken(User user, String token) throws Exception {
        mvc.perform(post("/api/notifications/fcm-token")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NOTI004"));
    }

    private void saveDrop(User owner) {
        ArchiveBoard board = archiveBoardRepository.save(ArchiveBoard.builder().user(owner).name("board").build());
        Image image = imageRepository.save(Image.builder().imageUrl("url").fileName(UUID.randomUUID().toString()).build());
        boardImageRepository.save(BoardImage.builder().board(board).image(image).build());
    }

    private User saveUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.createLocalUser("name", "nick" + id, id + "@noti.test", "pw"));
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(user, AuthProvider.LOCAL);
    }
}
