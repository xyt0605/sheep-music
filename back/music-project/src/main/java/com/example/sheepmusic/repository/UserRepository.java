package com.example.sheepmusic.repository;

import com.example.sheepmusic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 判断用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 判断邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据用户名或昵称模糊搜索用户
     */
    java.util.List<User> findByUsernameContainingOrNicknameContaining(String username, String nickname);

    /**
     * 按 ID 升序找第一个指定角色的用户（AI 配置回退：定位 admin 账号）
     */
    Optional<User> findFirstByRoleOrderByIdAsc(String role);
}

