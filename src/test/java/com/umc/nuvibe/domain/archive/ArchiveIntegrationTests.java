package com.umc.nuvibe.domain.archive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.nuvibe.domain.archive.repository.ArchiveBoardRepository;
import com.umc.nuvibe.domain.archive.repository.BoardImageRepository;
import com.umc.nuvibe.domain.image.entity.Image;
import com.umc.nuvibe.domain.image.repository.ImageRepository;
import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.user.entity.User;
import com.umc.nuvibe.domain.user.repository.UserRepository;
import com.umc.nuvibe.domain.user.vo.AuthProvider;
import com.umc.nuvibe.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class ArchiveIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired UserRepository userRepository;
    @Autowired ImageRepository imageRepository;
    @Autowired ArchiveBoardRepository archiveBoardRepository;
    @Autowired BoardImageRepository boardImageRepository;

    @Test
    void 보드_생성_목록_이름수정_삭제() throws Exception {
        User me = saveUser();

        Long boardId = createBoard(me, " 여행 ");
        createBoardRequest(me, "여행").andExpect(jsonPath("$.code").value("ARCHIVE004"));

        Image image = saveImage(ImageTag.Blur);
        call(post("/api/archive/" + boardId + "/images"), me, Map.of("imageId", image.getId()))
                .andExpect(jsonPath("$.code").value("ARCHIVE009"));

        call(get("/api/archive").param("keyword", "여"), me, null)
                .andExpect(jsonPath("$.code").value("ARCHIVE001"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("여행"))
                .andExpect(jsonPath("$.data[0].tagCount").value(1));

        call(patch("/api/archive/" + boardId + "/name"), me, Map.of("name", "일상"))
                .andExpect(jsonPath("$.code").value("ARCHIVE006"));
        assertThat(archiveBoardRepository.findById(boardId).orElseThrow().getName()).isEqualTo("일상");

        call(post("/api/archive/delete"), me, Map.of("boardIds", List.of(boardId)))
                .andExpect(jsonPath("$.code").value("ARCHIVE007"));
        assertThat(archiveBoardRepository.findById(boardId)).isEmpty();
    }

    @Test
    void 다른_유저의_보드는_조회_수정할_수_없다() throws Exception {
        User owner = saveUser();
        User other = saveUser();
        Long boardId = createBoard(owner, "내 보드");

        call(get("/api/archive/" + boardId), other, null)
                .andExpect(jsonPath("$.code").value("ARCHIVE001"))
                .andExpect(jsonPath("$.httpStatus").value("NOT_FOUND"));
        call(patch("/api/archive/" + boardId + "/name"), other, Map.of("name", "뺏기"))
                .andExpect(jsonPath("$.httpStatus").value("NOT_FOUND"));

        // 다른 유저가 보드 삭제를 요청해도 무시된다
        call(post("/api/archive/delete"), other, Map.of("boardIds", List.of(boardId)));
        assertThat(archiveBoardRepository.findById(boardId)).isPresent();
    }

    @Test
    void 이미지는_한_보드에만_담기고_보드간_이동과_삭제가_된다() throws Exception {
        User me = saveUser();
        Long from = createBoard(me, "from");
        Long to = createBoard(me, "to");
        Image image = saveImage(ImageTag.Grain);

        call(post("/api/archive/" + from + "/images"), me, Map.of("imageId", image.getId()));
        call(post("/api/archive/" + to + "/images"), me, Map.of("imageId", image.getId()))
                .andExpect(jsonPath("$.code").value("ARCHIVE006"));

        Long boardImageId = boardImageRepository.findByBoardIdOrderByCreatedAtDesc(from).get(0).getId();

        call(patch("/api/archive/" + from + "/images/move"), me,
                Map.of("targetBoardId", from, "boardImageIds", List.of(boardImageId)))
                .andExpect(jsonPath("$.code").value("ARCHIVE007"));

        call(patch("/api/archive/" + from + "/images/move"), me,
                Map.of("targetBoardId", to, "boardImageIds", List.of(boardImageId, boardImageId)))
                .andExpect(jsonPath("$.code").value("ARCHIVE010"));
        assertThat(boardImageRepository.findByBoardIdOrderByCreatedAtDesc(to)).hasSize(1);

        call(get("/api/archive/" + to).param("tag", "Grain"), me, null)
                .andExpect(jsonPath("$.code").value("ARCHIVE002"));

        call(delete("/api/archive/" + to + "/delete"), me, Map.of("boardImageIds", List.of(boardImageId)))
                .andExpect(jsonPath("$.code").value("ARCHIVE008"));
        call(delete("/api/archive/" + to + "/delete"), me, Map.of("boardImageIds", List.of(boardImageId)))
                .andExpect(jsonPath("$.code").value("ARCHIVE002"));
    }

    @Test
    void 전체_이미지와_vibe_요약을_조회한다() throws Exception {
        User me = saveUser();
        Long boardId = createBoard(me, "vibe");
        for (ImageTag tag : List.of(ImageTag.Blur, ImageTag.Blur, ImageTag.Grain)) {
            call(post("/api/archive/" + boardId + "/images"), me, Map.of("imageId", saveImage(tag).getId()));
        }

        call(get("/api/archive/images").param("size", "2"), me, null)
                .andExpect(jsonPath("$.code").value("ARCHIVE003"))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(3));

        call(get("/api/archive/vibe"), me, null)
                .andExpect(jsonPath("$.code").value("ARCHIVE004"))
                .andExpect(jsonPath("$.data.nickname").value(me.getNickname()));
    }

    @Test
    void 리캡_조회() throws Exception {
        User me = saveUser();

        // 데이터가 없으면 NOT_ENOUGH_DATA
        call(get("/api/recap/tags").param("period", "WEEK"), me, null)
                .andExpect(jsonPath("$.code").value("RECAP_ERR002"));

        Long boardId = createBoard(me, "recap");
        for (ImageTag tag : List.of(ImageTag.Blur, ImageTag.Blur, ImageTag.Grain)) {
            call(post("/api/archive/" + boardId + "/images"), me, Map.of("imageId", saveImage(tag).getId()));
        }

        for (String period : List.of("WEEK", "TOTAL")) {
            call(get("/api/recap/tags").param("period", period), me, null)
                    .andExpect(jsonPath("$.code").value("RECAP001"))
                    .andExpect(jsonPath("$.data.totalDropsCount").value(3))
                    .andExpect(jsonPath("$.data.ranks[0].tag").value("Blur"));

            call(get("/api/recap/board").param("period", period), me, null)
                    .andExpect(jsonPath("$.code").value("RECAP002"))
                    .andExpect(jsonPath("$.data.boardId").value(boardId));

            call(get("/api/recap/active").param("period", period), me, null)
                    .andExpect(jsonPath("$.code").value("RECAP003"))
                    .andExpect(jsonPath("$.data.totalBoardCount").value(1))
                    .andExpect(jsonPath("$.data.maxDailyDropCount").value(3));
        }

        LocalDate today = LocalDate.now();
        call(get("/api/recap/calendar/dates")
                .param("year", String.valueOf(today.getYear()))
                .param("month", String.valueOf(today.getMonthValue())), me, null)
                .andExpect(jsonPath("$.code").value("RECAP004"))
                .andExpect(jsonPath("$.data[0]").value(today.toString()));

        call(get("/api/recap/calendar/dates").param("year", "2026").param("month", "13"), me, null)
                .andExpect(jsonPath("$.code").value("RECAP_ERR003"));

        call(get("/api/recap/calendar/images").param("date", today.toString()), me, null)
                .andExpect(jsonPath("$.code").value("RECAP005"));
    }

    private Long createBoard(User user, String name) throws Exception {
        String body = createBoardRequest(user, name)
                .andExpect(jsonPath("$.code").value("ARCHIVE005"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(body);
        return node.path("data").path("boardId").asLong();
    }

    private ResultActions createBoardRequest(User user, String name) throws Exception {
        return call(post("/api/archive"), user, Map.of("name", name));
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                               User user, Object body) throws Exception {
        req.header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user, AuthProvider.LOCAL));
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
        }
        return mvc.perform(req);
    }

    private Image saveImage(ImageTag tag) {
        return imageRepository.save(Image.builder()
                .imageUrl("https://img/" + UUID.randomUUID())
                .imageTag(tag)
                .fileName(UUID.randomUUID().toString())
                .build());
    }

    private User saveUser() {
        String id = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.createLocalUser("name", "nick" + id, id + "@archive.test", "pw"));
    }
}
