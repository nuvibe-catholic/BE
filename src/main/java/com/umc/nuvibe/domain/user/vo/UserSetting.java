package com.umc.nuvibe.domain.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 유저 알림 설정 (users.setting JSON 컬럼)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSetting implements Serializable {

    // 서비스 알림
    private Boolean isServiceAlert;

    // 계정 보안 알림
    private Boolean isSecurityAlert;

    // 추천 알림
    private Boolean isRecommendAlert;

    // 리캡 알림
    private Boolean isRecapAlert;

    // 트라이브 챗 생성 알림
    private Boolean isTribeCreateAlert;

    // 트라이브 챗 채팅 알림
    private Boolean isTribeChatAlert;

    // 이미지 반응 알림
    private Boolean isReactionAlert;

    // 회원가입 시 기본값 (전체 허용)
    public static UserSetting createDefault() {
        return UserSetting.builder()
                .isServiceAlert(true)
                .isSecurityAlert(true)
                .isRecommendAlert(true)
                .isRecapAlert(true)
                .isTribeCreateAlert(true)
                .isTribeChatAlert(true)
                .isReactionAlert(true)
                .build();
    }
}
