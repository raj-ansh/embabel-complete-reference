package com.northwind.knowledge;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.chat.Chatbot;
import com.embabel.chat.agent.AgentProcessChatbot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KnowledgeAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeAssistantApplication.class, args);
    }

    /** A chatbot that runs the platform's @EmbabelComponent actions with the Utility planner. */
    @Bean
    Chatbot chatbot(AgentPlatform platform) {
        return AgentProcessChatbot.utilityFromPlatform(platform);
    }
}
