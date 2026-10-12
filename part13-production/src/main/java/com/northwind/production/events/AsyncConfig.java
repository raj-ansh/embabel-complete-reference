package com.northwind.production.events;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Chapter 99: @Async listeners run on Spring Boot's applicationTaskExecutor, not on the publisher's thread. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
