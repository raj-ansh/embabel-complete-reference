package com.northwind.springbasics.catalog;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Demonstrates lifecycle callbacks: warmed up after injection, cleared on shutdown. */
@Component
public class CatalogCache {

    private final Map<String, String> names = new ConcurrentHashMap<>();

    @PostConstruct
    void warmUp() {
        names.put("TENT-2P", "Ridgeline 2-Person Tent");
        names.put("BOOT-W9", "Trailblazer Waterproof Boot");
        System.out.println("[lifecycle] catalog cache warmed with " + names.size() + " items");
    }

    @PreDestroy
    void clear() {
        names.clear();
        System.out.println("[lifecycle] catalog cache cleared");
    }

    public String nameOf(String sku) {
        return names.getOrDefault(sku, sku);
    }
}
