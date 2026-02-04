package com.punarmilan.backend.service;

import com.punarmilan.backend.dto.ConversationDto;
import com.punarmilan.backend.dto.MessageDto;
import com.punarmilan.backend.dto.SendMessageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessagingService {
    
    // Send a message
    MessageDto sendMessage(SendMessageDto sendMessageDto);
    
    // Get conversation between two users
    ConversationDto getConversation(Long otherUserId);
    
    // Get all conversations for current user
    Page<ConversationDto> getConversations(Pageable pageable);
    
    // Get messages in a conversation
    Page<MessageDto> getMessages(Long conversationId, Pageable pageable);
    
    // Mark conversation as read
    void markConversationAsRead(Long conversationId);
    
    // Delete conversation
    void deleteConversation(Long conversationId);
    
    // Block/Unblock user
    void toggleBlockUser(Long userId, boolean block, String reason);
    
    // Check if conversation exists
    boolean conversationExists(Long user1Id, Long user2Id);
    
    // Get unread message count
    long getUnreadMessageCount();
    
    // Get unread conversations
    List<ConversationDto> getUnreadConversations();
    
    // Send typing indicator
    void sendTypingIndicator(Long conversationId, boolean isTyping);
    
    // Send read receipt
    void sendReadReceipt(Long messageId);
}