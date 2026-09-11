package com.example.sheepmusic.agent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface DjChatMessageRepository extends org.springframework.data.jpa.repository.JpaRepository<DjChatMessage, Long> {

    Page<DjChatMessage> findByUserIdAndSessionIdOrderByCreateTimeDesc(Long userId, String sessionId, Pageable pageable);

    void deleteByUserIdAndSessionIdAndCreateTimeBefore(Long userId, String sessionId, LocalDateTime time);

    long deleteByCreateTimeBefore(LocalDateTime time);
}
