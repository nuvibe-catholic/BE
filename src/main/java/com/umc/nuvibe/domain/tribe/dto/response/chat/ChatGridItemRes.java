package com.umc.nuvibe.domain.tribe.dto.response.chat;

import com.umc.nuvibe.domain.image.entity.Image;
import com.umc.nuvibe.domain.tribe.entity.Chat;
import com.umc.nuvibe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅 이미지 그리드 아이템")
public record ChatGridItemRes(

        @Schema(description = "채팅 ID")
        Long chatId,

        @Schema(description = "이미지 ID")
        Long imageId,

        @Schema(description = "이미지 썸네일 URL")
        String imageUrl,

        @Schema(description = "이미지 업로드 시각 (정렬/날짜 섹션 기준)")
        LocalDateTime createdAt,

        @Schema(description = "채팅 전송자 닉네임")
        String chatSenderNickname
) {
    public static ChatGridItemRes from(Chat chat) {
        Image image = chat.getImage();
        User sender = chat.getUser();
        return new ChatGridItemRes(
                chat.getId(),
                image.getId(),
                image.getThumbnailUrl(),
                chat.getCreatedAt(),
                sender != null ? sender.getNickname() : null
        );
    }
}
