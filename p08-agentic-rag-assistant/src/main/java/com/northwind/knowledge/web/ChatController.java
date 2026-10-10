package com.northwind.knowledge.web;

import com.embabel.agent.api.channel.DevNullOutputChannel;
import com.embabel.agent.api.identity.SimpleUser;
import com.embabel.chat.ChatSession;
import com.embabel.chat.Chatbot;
import com.embabel.chat.Message;
import com.embabel.chat.MessageRole;
import com.embabel.chat.UserMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * P8's chat API. A conversation is a long-lived agent process; the client keeps its conversationId.
 * Headers stand in for authentication (Chapter 84).
 */
@RestController
public class ChatController {

    public record ChatRequest(@Nullable String conversationId, @NotBlank @Size(max = 2000) String message) {
    }

    public record ChatResponse(String conversationId, String reply) {
    }

    private final Chatbot chatbot;

    public ChatController(Chatbot chatbot) {
        this.chatbot = chatbot;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestHeader("X-User-Id") String userId, @Valid @RequestBody ChatRequest request) {
        ChatSession session = request.conversationId() == null
                ? chatbot.createSession(new SimpleUser(userId, userId, userId, null), DevNullOutputChannel.INSTANCE, null, null)
                : chatbot.findSession(request.conversationId());
        if (session == null || !userId.equals(session.getUser().getId())) {
            // someone else's conversation looks exactly like a missing one
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such conversation");
        }
        session.onUserMessage(new UserMessage(request.message()));
        List<Message> messages = session.getConversation().getMessages();
        Message last = messages.getLast();
        String reply = last.getRole() == MessageRole.ASSISTANT ? last.getContent() : "";
        return new ChatResponse(session.getConversation().getId(), reply);
    }
}
