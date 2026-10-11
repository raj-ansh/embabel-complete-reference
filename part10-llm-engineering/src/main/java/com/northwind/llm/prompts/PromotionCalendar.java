package com.northwind.llm.prompts;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/** Chapter 74: marketing changes the promotion; prompts pick it up on the next call. */
@Component
public class PromotionCalendar {

    private final AtomicReference<String> current =
            new AtomicReference<>("Autumn sale, 20% off sleeping bags until 31 October.");

    public String current() {
        return current.get();
    }

    public void change(String promotion) {
        current.set(promotion);
    }
}
