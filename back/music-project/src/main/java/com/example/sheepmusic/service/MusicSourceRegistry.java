package com.example.sheepmusic.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 音源注册表：Spring 自动收集所有 MusicSourceProvider 实现
 */
@Component
public class MusicSourceRegistry {

    private final Map<String, MusicSourceProvider> providers;
    private final List<MusicSourceProvider> providerList;

    public MusicSourceRegistry(List<MusicSourceProvider> providerList) {
        this.providerList = List.copyOf(providerList);
        this.providers = providerList.stream()
                .collect(Collectors.toMap(MusicSourceProvider::source, Function.identity()));
    }

    public Optional<MusicSourceProvider> optionalGet(String source) {
        return Optional.ofNullable(source).map(providers::get);
    }

    public List<MusicSourceProvider> all() {
        return providerList;
    }
}
