package com.example.sheepmusic.config;

import com.example.sheepmusic.entity.User;
import com.example.sheepmusic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 管理员账号初始化：
 * 注册接口不再根据用户名发放 admin 角色，系统管理员账号统一在应用启动时保证存在。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:123456}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname("管理员");
        admin.setRole("admin");
        admin.setStatus(1);
        userRepository.save(admin);

        if ("123456".equals(adminPassword)) {
            System.out.println("[AdminInitializer] 已创建默认管理员账号 '" + adminUsername
                + "'（密码 " + adminPassword + "），生产环境请通过 APP_ADMIN_PASSWORD 环境变量指定强密码");
        } else {
            System.out.println("[AdminInitializer] 已创建管理员账号 '" + adminUsername + "'");
        }
    }
}
