package com.umc.nuvibe.domain.tribe.controller;

import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeActivateRes;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeFavoriteRes;
import com.umc.nuvibe.domain.tribe.dto.response.userTribe.UserTribeMuteRes;
import com.umc.nuvibe.domain.tribe.service.userTribe.UserTribeService;
import com.umc.nuvibe.global.apiPayLoad.response.Response;
import com.umc.nuvibe.global.apiPayLoad.result.UserTribeResultCode;
import com.umc.nuvibe.global.security.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userTribe")
@Tag(name = "UserTribe", description = "유저 트라이브 API")
public class UserTribeController {

    private final UserTribeService userTribeService;

    @PatchMapping("/{userTribeId}/activate")
    @Operation(summary = "유저 트라이브 챗 활성화", description = "인원 수가 5명 이상인 트라이브 챗에 대해 유저 별로 활성화")
    public Response<UserTribeActivateRes> activateUserTribe(
            @AuthUser Long userId,
            @PathVariable Long userTribeId
    ) {
        UserTribeActivateRes res = userTribeService.activateUserTribe(userId, userTribeId);
        return Response.of(UserTribeResultCode.USERTRIBE_ACTIVATE_SUCCESS, res);
    }

    @PatchMapping("/{userTribeId}/favorite")
    @Operation(summary = "트라이브 챗 즐겨찾기", description = "원하는 트라이브 챗을 즐겨찾기로 등록 (재요청 시 해제)")
    public Response<UserTribeFavoriteRes> favoriteUserTribe(
            @AuthUser Long userId,
            @PathVariable Long userTribeId
    ) {
        UserTribeFavoriteRes res = userTribeService.toggleFavorite(userId, userTribeId);
        return Response.of(UserTribeResultCode.USERTRIBE_FAVORITE_SUCCESS, res);
    }

    @PatchMapping("/{userTribeId}/mute")
    @Operation(summary = "트라이브 챗 무음 설정", description = "특정 트라이브 챗의 푸시 알림 무음 토글")
    public Response<UserTribeMuteRes> muteUserTribe(
            @AuthUser Long userId,
            @PathVariable Long userTribeId
    ) {
        UserTribeMuteRes res = userTribeService.toggleMute(userId, userTribeId);
        return Response.of(UserTribeResultCode.USERTRIBE_MUTE_SUCCESS, res);
    }
}
