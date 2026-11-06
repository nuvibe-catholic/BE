package com.umc.nuvibe.domain.tribe.repository;

import com.umc.nuvibe.domain.image.vo.ImageTag;
import com.umc.nuvibe.domain.tribe.entity.UserTribe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTribeRepository extends JpaRepository<UserTribe, Long> {

    // 해당 태그의 트라이브 챗 입장 여부
    boolean existsByUser_IdAndTribe_ImageTag(Long userId, ImageTag imageTag);
}
