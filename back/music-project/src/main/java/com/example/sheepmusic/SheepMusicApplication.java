package com.example.sheepmusic;

import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCaching // 歌手全量列表等热点查询启用进程内缓存（ConcurrentMapCache）
public class SheepMusicApplication {

    public static void main(String[] args) {
        SpringApplication.run(SheepMusicApplication.class, args);
    }

}
