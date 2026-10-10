package com.example.l4j.web;

import com.example.l4j.orchestrator.AgentReply;
import com.example.l4j.orchestrator.OrchestratingAgent;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class ChatController {

    private static final String DEFAULT_SESSION = "default";

    private final OrchestratingAgent agent;

    public ChatController(OrchestratingAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? DEFAULT_SESSION : request.sessionId();
        AgentReply reply = agent.chat(sessionId, request.message());
        return new ChatResponse(sessionId, reply.content(), reply.agent());
    }
}
