package com.example.sheepmusic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 好友关系实体类
 */
@Data
@Entity
@Table(name = "tb_friendship",
       uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "friendId"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Friendship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 发起好友请求的用户ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 被请求的用户ID（好友ID）
     */
    @Column(nullable = false)
    private Long friendId;
    
    /**
     * 状态：pending-待确认, accepted-已接受, rejected-已拒绝, blocked-已拉黑
     */
    @Column(nullable = false, length = 20)
    private String status = "pending";
    
    /**
     * 备注名
     */
    @Column(length = 50)
    private String remark;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    private LocalDateTime updateTime;
    
    /**
     * 关联好友信息（懒加载）
     * 注意：使用 @JsonIgnore 避免序列化懒加载代理对象，使用冗余字段 friendName 和 friendAvatar
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friendId", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User friend;
    
    /**
     * 冗余字段：好友昵称
     */
    @Column(length = 50)
    private String friendName;
    
    /**
     * 冗余字段：好友头像
     */
    @Column(length = 500)
    private String friendAvatar;
    
    /**
     * 冗余字段：发起者昵称（用于好友请求列表显示）
     */
    @Column(length = 50)
    private String userName;
    
    /**
     * 冗余字段：发起者头像（用于好友请求列表显示）
     */
    @Column(length = 500)
    private String userAvatar;
}

