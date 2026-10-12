package com.northwind.tenancy.security;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Chapter 107: Embabel carries its own MDC keys onto its worker threads, but not the application's, so the
 * tenant was missing from the agent's log lines. With embabel.agent.platform.threading.shared=true, Embabel
 * uses Spring Boot's applicationTaskExecutor, and this decorator copies the caller's MDC to each task.
 */
@Configuration
public class MdcPropagation {

    @Bean
    TaskDecorator copyMdc() {
        return work -> {
            Map<String, String> callers = MDC.getCopyOfContextMap();        // on the submitting thread
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (callers != null) {
                    MDC.setContextMap(callers);
                }
                try {
                    work.run();
                } finally {
                    if (previous == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previous);
                    }
                }
            };
        };
    }
}
