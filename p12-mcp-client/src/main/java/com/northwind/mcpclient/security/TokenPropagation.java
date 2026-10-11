package com.northwind.mcpclient.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/**
 * Chapter 88: Embabel runs tool calls on its own worker threads, where a ThreadLocal set by the caller is
 * invisible. With embabel.agent.platform.threading.shared=true Embabel uses Spring Boot's
 * applicationTaskExecutor instead, and Spring Boot applies this decorator to it: the caller's token is
 * captured when work is submitted and restored on the worker thread for the duration of that work.
 */
@Configuration
public class TokenPropagation {

    @Bean
    TaskDecorator carryUserToken() {
        return work -> {
            String token = CurrentUserToken.get();                  // on the submitting thread
            return () -> CurrentUserToken.runAs(token, () -> {      // on the worker thread
                work.run();
                return null;
            });
        };
    }
}
