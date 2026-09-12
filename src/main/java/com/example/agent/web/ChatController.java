package com.example.agent.web;

import com.example.agent.agent.Agent;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class ChatController {

    private static final String DEFAULT_SESSION = "default";

    private final Agent agent;

    public ChatController(Agent agent) {
        this.agent = agent;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? DEFAULT_SESSION : request.sessionId();
        String reply = agent.chat(sessionId, request.message());
        return new ChatResponse(sessionId, reply);
    }
}
