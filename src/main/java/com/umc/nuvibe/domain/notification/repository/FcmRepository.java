package com.umc.nuvibe.domain.notification.repository;

import com.umc.nuvibe.domain.notification.entity.Fcm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmRepository extends JpaRepository<Fcm, Long> {

    List<Fcm> findByUser_IdAndIsActiveTrue(Long userId);

    Optional<Fcm> findByUser_IdAndToken(Long userId, String token);

    List<Fcm> findByToken(String token);

    // 같은 기기 토큰이 다른 계정에 활성화 되어있는 경우
    List<Fcm> findByTokenAndIsActiveTrueAndUser_IdNot(String token, Long userId);
}
