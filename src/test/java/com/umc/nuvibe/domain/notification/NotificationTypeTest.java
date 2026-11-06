package com.umc.nuvibe.domain.notification;

import com.umc.nuvibe.domain.notification.vo.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTypeTest {

    @Test
    void 태그가_있으면_해시태그로_치환된다() {
        assertThat(NotificationType.NOTI_02.formatPushMessage("Blur"))
                .isEqualTo("#Blur 트라이브 챗에 새로운 바이브가 올라왔어요.");
        assertThat(NotificationType.NOTI_07.formatPushMessage("Grain"))
                .isEqualTo("오늘의 추천 태그 #Grain — 떠오르는 바이브가 있나요?");
    }

    @Test
    void 태그가_없으면_자리째_지워진다() {
        assertThat(NotificationType.NOTI_02.formatPushMessage(null))
                .isEqualTo("트라이브 챗에 새로운 바이브가 올라왔어요.");
        assertThat(NotificationType.NOTI_01.formatActionMessage(" "))
                .isEqualTo("트라이브 챗에 지금 들어가 볼까요?");
    }

    @Test
    void 푸시_문구가_없는_타입은_알림함_전용이다() {
        assertThat(NotificationType.NOTI_10.hasPush()).isFalse();
        assertThat(NotificationType.NOTI_10.formatPushMessage("tag")).isNull();
        assertThat(NotificationType.NOTI_10.formatMainMessage(null)).isEqualTo("비밀번호가 변경됐어요");
    }
}
