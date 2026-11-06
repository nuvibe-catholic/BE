package com.umc.nuvibe.global.security.websocket;

import com.umc.nuvibe.domain.tribe.repository.UserTribeRepository;
import com.umc.nuvibe.domain.tribe.vo.UserTribeStatus;
import com.umc.nuvibe.global.apiPayLoad.error.AuthErrorCode;
import com.umc.nuvibe.global.apiPayLoad.error.WebsocketErrorCode;
import com.umc.nuvibe.global.apiPayLoad.exception.BusinessException;
import com.umc.nuvibe.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    // 트라이브 채널 destination 형식 (/topic/tribe.{tribeId})
    private static final Pattern TRIBE_TOPIC = Pattern.compile("^/topic/tribe\\.(\\d+)$");

    private final JwtTokenProvider jwtTokenProvider;
    private final UserTribeRepository userTribeRepository;

    // STOMP 프레임이 서버로 들어오기 직전에 호출
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // heartbeat 등 command가 없는 프레임은 통과
        if (accessor.getCommand() == null) {
            return message;
        }

        // 헤더 변경 사항이 메시지에 반영되도록 설정
        accessor.setLeaveMutable(true);

        return switch (accessor.getCommand()) {
            case CONNECT -> handleConnect(message, accessor);
            case SUBSCRIBE -> handleSubscribe(message, accessor);
            default -> message;
        };
    }

    // CONNECT: Authorization 헤더의 JWT 검증 후 userId를 Principal로 등록
    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {

        String bearer = accessor.getFirstNativeHeader("Authorization");
        String token = jwtTokenProvider.extractBearerToken(bearer);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new BusinessException(AuthErrorCode.JWT_INVALID_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null) {
            sessionAttrs.put("userId", userId);
        }

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    // SUBSCRIBE: 트라이브 채널은 해당 트라이브 챗에 ACTIVE 상태로 참여 중인 유저만 구독 가능
    private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {

        Long userId = extractUserId(accessor);
        String destination = accessor.getDestination();

        if (destination == null) {
            return message;
        }

        Matcher matcher = TRIBE_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return message;
        }

        Long tribeId = Long.valueOf(matcher.group(1));

        boolean isActiveMember = userTribeRepository.existsByUser_IdAndTribe_IdAndUserTribeStatus(
                userId, tribeId, UserTribeStatus.ACTIVE
        );
        if (!isActiveMember) {
            throw new BusinessException(WebsocketErrorCode.WS_SUBSCRIBE_FORBIDDEN);
        }

        return message;
    }

    // CONNECT 단계에서 저장한 userId 조회
    private Long extractUserId(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof Long id) {
            return id;
        }

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null && sessionAttrs.get("userId") instanceof Long id) {
            return id;
        }

        throw new BusinessException(WebsocketErrorCode.WS_UNAUTHORIZED);
    }
}
