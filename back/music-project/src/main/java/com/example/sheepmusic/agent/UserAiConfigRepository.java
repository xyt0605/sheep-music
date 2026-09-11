package com.example.sheepmusic.agent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAiConfigRepository extends JpaRepository<UserAiConfig, Long> {

    UserAiConfig findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
