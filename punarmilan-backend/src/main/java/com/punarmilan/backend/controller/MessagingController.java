package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.ConversationDto;


import com.punarmilan.backend.dto.MessageDto;
import com.punarmilan.backend.dto.SendMessageDto;
import com.punarmilan.backend.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
@Tag(name = "Messaging", description = "Chat and messaging system")
@PreAuthorize("hasRole('USER')")
public class MessagingController {

    private final MessagingService messagingService;

    @Operation(summary = "Send a message")
    @PostMapping("/send")
    public ResponseEntity<MessageDto> sendMessage(@RequestBody SendMessageDto sendMessageDto) {
        MessageDto message = messagingService.sendMessage(sendMessageDto);
        return ResponseEntity.ok(message);
    }

    @Operation(summary = "Get conversation with user")
    @GetMapping("/conversation/{userId}")
    public ResponseEntity<ConversationDto> getConversation(@PathVariable Long userId) {
        ConversationDto conversation = messagingService.getConversation(userId);
        return ResponseEntity.ok(conversation);
    }

    @Operation(summary = "Get all conversations")
    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationDto>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastMessageTime").descending());
        Page<ConversationDto> conversations = messagingService.getConversations(pageable);
        return ResponseEntity.ok(conversations);
    }

    @Operation(summary = "Get messages in conversation")
    @GetMapping("/conversation/{conversationId}/messages")
    public ResponseEntity<Page<MessageDto>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MessageDto> messages = messagingService.getMessages(conversationId, pageable);
        return ResponseEntity.ok(messages);
    }

    @Operation(summary = "Mark conversation as read")
    @PatchMapping("/conversation/{conversationId}/read")
    public ResponseEntity<Void> markConversationAsRead(@PathVariable Long conversationId) {
        messagingService.markConversationAsRead(conversationId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete conversation")
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long conversationId) {
        messagingService.deleteConversation(conversationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Block/Unblock user")
    @PostMapping("/block/{userId}")
    public ResponseEntity<Void> toggleBlockUser(
            @PathVariable Long userId,
            @RequestParam boolean block,
            @RequestParam(required = false) String reason) {
        
        messagingService.toggleBlockUser(userId, block, reason);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get unread message count")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = messagingService.getUnreadMessageCount();
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "Get unread conversations")
    @GetMapping("/unread-conversations")
    public ResponseEntity<List<ConversationDto>> getUnreadConversations() {
        List<ConversationDto> conversations = messagingService.getUnreadConversations();
        return ResponseEntity.ok(conversations);
    }

    @Operation(summary = "Send typing indicator")
    @PostMapping("/typing/{conversationId}")
    public ResponseEntity<Void> sendTypingIndicator(
            @PathVariable Long conversationId,
            @RequestParam boolean isTyping) {
        
        messagingService.sendTypingIndicator(conversationId, isTyping);
        return ResponseEntity.ok().build();
    }
       
    @Operation(summary = "Send read receipt")
    @PostMapping("/read-receipt/{messageId}")
    public ResponseEntity<Void> sendReadReceipt(@PathVariable Long messageId) {
        messagingService.sendReadReceipt(messageId);
        return ResponseEntity.ok().build();
    }
}