package com.punarmilan.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
public class WebSocketController {

    @MessageMapping("/chat.test")
    @SendTo("/topic/test")
    public Map<String, Object> testMessage(Map<String, Object> message, Principal principal) {
        log.info("Test message from {}: {}", 
            principal != null ? principal.getName() : "anonymous", 
            message);
        
        return Map.of(
            "type", "TEST",
            "sender", principal != null ? principal.getName() : "anonymous",
            "content", message.get("content"),
            "timestamp", LocalDateTime.now().toString(),
            "status", "success"
        );
    }

    @MessageMapping("/chat.private.test")
    @SendToUser("/queue/private")
    public Map<String, Object> testPrivateMessage(Map<String, Object> message, Principal principal) {
        log.info("Private test message from {}: {}", 
            principal != null ? principal.getName() : "anonymous", 
            message);
        
        return Map.of(
            "type", "PRIVATE_TEST",
            "sender", principal != null ? principal.getName() : "anonymous",
            "content", message.get("content"),
            "timestamp", LocalDateTime.now().toString(),
            "status", "success"
        );
    }
}