package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置数据访问层
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    List<SystemConfig> findByConfigKeyStartingWith(String prefix);

    void deleteByConfigKeyStartingWith(String prefix);
}
