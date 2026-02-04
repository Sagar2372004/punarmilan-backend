package com.punarmilan.backend.service.impl;

import com.punarmilan.backend.dto.ConversationDto;

import com.punarmilan.backend.dto.MessageDto;
import com.punarmilan.backend.dto.SendMessageDto;
import com.punarmilan.backend.entity.Conversation;
import com.punarmilan.backend.entity.Message;
import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.exception.BadRequestException;
import com.punarmilan.backend.exception.ResourceNotFoundException;
import com.punarmilan.backend.repository.ConversationRepository;
import com.punarmilan.backend.repository.MatchRepository;
import com.punarmilan.backend.repository.MessageRepository;
import com.punarmilan.backend.repository.UserRepository;
import com.punarmilan.backend.service.MessagingService;
import com.punarmilan.backend.service.PhotoVisibilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessagingServiceImpl implements MessagingService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final PhotoVisibilityService photoVisibilityService;

    @Override
    public MessageDto sendMessage(SendMessageDto sendMessageDto) {
        User currentUser = getCurrentUser();
        User receiver = userRepository.findById(sendMessageDto.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receiver not found with id: " + sendMessageDto.getReceiverId()));

        // Check if users can message each other
        validateMessagingPermission(currentUser, receiver);

        // Check if sender is hidden
        if (currentUser.isHidden()) {
            throw new BadRequestException("You cannot send messages while your profile is hidden. Unhide it first.");
        }

        // Get or create conversation
        Conversation conversation = getOrCreateConversation(currentUser, receiver);

        // Determine message type safely
        Message.MessageType messageType = parseMessageType(sendMessageDto.getMessageType());

        // Create message object
        Message message = Message.builder()
                .sender(currentUser)
                .receiver(receiver)
                .conversation(conversation)
                .content(sendMessageDto.getContent())
                .messageType(messageType)
                .fileUrl(sendMessageDto.getFileUrl())
                .fileName(sendMessageDto.getFileName())
                .fileSize(sendMessageDto.getFileSize())
                .repliedToMessageId(sendMessageDto.getRepliedToMessageId())
                .read(false)
                .delivered(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Save and get the managed entity
        final Message savedMessage = messageRepository.save(message);

        // Update conversation metadata
        updateConversation(conversation, savedMessage);

        // Sync with Match if it exists to keep unread counts consistent across UI
        // We use currentUser and receiver which are already managed and have IDs
        matchRepository.findMatchBetweenUsers(currentUser, receiver).ifPresent(match -> {
            match.updateLastMessage(savedMessage.getContent(), currentUser.getId());
            matchRepository.save(match);
        });

        // Asynchronous WebSocket notification
        sendWebSocketNotification(savedMessage, conversation);

        // Push notification
        sendPushNotification(currentUser, receiver, savedMessage);

        log.info("Message sent from {} to {}: type={}",
                currentUser.getEmail(), receiver.getEmail(), messageType);

        return mapToMessageDto(savedMessage, currentUser.getId());
    }

    private Message.MessageType parseMessageType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return Message.MessageType.TEXT;
        }
        try {
            return Message.MessageType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid message type received: {}, defaulting to TEXT", typeStr);
            return Message.MessageType.TEXT;
        }
    }

    @Override
    public ConversationDto getConversation(Long otherUserId) {
        User currentUser = getCurrentUser();
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + otherUserId));

        Conversation conversation = conversationRepository
                .findConversationBetweenUsers(currentUser, otherUser)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        return mapToConversationDto(conversation, currentUser.getId());
    }

    @Override
    public Page<ConversationDto> getConversations(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Conversation> conversations = conversationRepository
                .findConversationsForUser(currentUser, pageable);

        return conversations.map(conv -> mapToConversationDto(conv, currentUser.getId()));
    }

    @Override
    public Page<MessageDto> getMessages(Long conversationId, Pageable pageable) {
        User currentUser = getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        // Verify user is part of conversation
        if (!conversation.getUser1().getId().equals(currentUser.getId()) &&
                !conversation.getUser2().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("You are not part of this conversation");
        }

        // Mark messages as read
        markMessagesAsRead(conversation, currentUser);

        // Reset unread count
        resetUnreadCount(conversation, currentUser);

        Page<Message> messages = messageRepository
                .findByConversationOrderByCreatedAtDesc(conversation, pageable);

        return messages.map(msg -> mapToMessageDto(msg, currentUser.getId()));
    }

    @Override
    public void markConversationAsRead(Long conversationId) {
        User currentUser = getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        // Verify user is part of conversation
        if (!conversation.getUser1().getId().equals(currentUser.getId()) &&
                !conversation.getUser2().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("You are not part of this conversation");
        }

        markMessagesAsRead(conversation, currentUser);
        resetUnreadCount(conversation, currentUser);

        // Sync with Match
        User otherUser = getOtherUserInConversation(conversation, currentUser);
        matchRepository.findMatchBetweenUsers(currentUser, otherUser).ifPresent(match -> {
            match.resetUnreadCount(currentUser.getId());
            matchRepository.save(match);
        });

        log.info("Conversation {} marked as read by user {}", conversationId, currentUser.getEmail());
    }

    @Override
    public void deleteConversation(Long conversationId) {
        User currentUser = getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (conversation.getUser1().getId().equals(currentUser.getId())) {
            conversation.setUser1Deleted(true);
        } else if (conversation.getUser2().getId().equals(currentUser.getId())) {
            conversation.setUser2Deleted(true);
        } else {
            throw new ResourceNotFoundException("You are not part of this conversation");
        }

        // If both users deleted, delete the conversation
        if (conversation.getUser1Deleted() && conversation.getUser2Deleted()) {
            conversationRepository.delete(conversation);
            log.info("Conversation {} deleted completely", conversationId);
        } else {
            conversationRepository.save(conversation);
            log.info("Conversation {} soft deleted by user {}", conversationId, currentUser.getEmail());
        }
    }

    @Override
    public void toggleBlockUser(Long userId, boolean block, String reason) {
        User currentUser = getCurrentUser();
        User otherUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Conversation conversation = conversationRepository
                .findConversationBetweenUsers(currentUser, otherUser)
                .orElse(null);

        if (conversation == null) {
            // Create conversation for blocking
            conversation = Conversation.builder()
                    .user1(currentUser)
                    .user2(otherUser)
                    .blocked(block)
                    .blockedBy(block ? currentUser.getId() : null)
                    .blockReason(block ? reason : null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else {
            conversation.setBlocked(block);
            conversation.setBlockedBy(block ? currentUser.getId() : null);
            conversation.setBlockReason(block ? reason : null);
            conversation.setUpdatedAt(LocalDateTime.now());
        }

        conversationRepository.save(conversation);
        log.info("User {} {} by {}", otherUser.getEmail(),
                block ? "blocked" : "unblocked", currentUser.getEmail());
    }

    @Override
    public boolean conversationExists(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user1Id));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user2Id));

        // Use the custom query method from ConversationRepository
        return conversationRepository.existsBetweenUsers(user1, user2);
    }

    @Override
    public long getUnreadMessageCount() {
        User currentUser = getCurrentUser();
        return messageRepository.countUnreadMessages(currentUser);
    }

    @Override
    public List<ConversationDto> getUnreadConversations() {
        User currentUser = getCurrentUser();
        List<Conversation> conversations = conversationRepository
                .findUnreadConversations(currentUser);

        return conversations.stream()
                .map(conv -> mapToConversationDto(conv, currentUser.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public void sendTypingIndicator(Long conversationId, boolean isTyping) {
        User currentUser = getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        User receiver = getOtherUserInConversation(conversation, currentUser);

        // Check if hidden
        if (currentUser.isHidden()) {
            return; // Don't send typing indicator if hidden
        }

        // Send typing indicator via WebSocket
        Map<String, Object> typingData = new HashMap<>();
        typingData.put("conversationId", conversationId);
        typingData.put("userId", currentUser.getId());
        typingData.put("isTyping", isTyping);
        typingData.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/typing",
                typingData);
    }

    @Override
    public void sendReadReceipt(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        message.setRead(true);
        messageRepository.save(message);

        // Send read receipt via WebSocket
        Map<String, Object> receiptData = new HashMap<>();
        receiptData.put("messageId", messageId);
        receiptData.put("readAt", LocalDateTime.now().toString());

        messagingTemplate.convertAndSendToUser(
                message.getSender().getId().toString(),
                "/queue/read-receipt",
                receiptData);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void validateMessagingPermission(User sender, User receiver) {
        // Check if receiver blocked sender
        Conversation existingConv = conversationRepository
                .findConversationBetweenUsers(sender, receiver)
                .orElse(null);

        if (existingConv != null && existingConv.getBlocked() &&
                existingConv.getBlockedBy() != null && existingConv.getBlockedBy().equals(receiver.getId())) {
            throw new BadRequestException("You cannot message this user - you have been blocked");
        }

        // Check if sender blocked receiver
        if (existingConv != null && existingConv.getBlocked() &&
                existingConv.getBlockedBy() != null && existingConv.getBlockedBy().equals(sender.getId())) {
            throw new BadRequestException("You have blocked this user. Unblock them to send messages.");
        }

        // Add more validation logic as needed
        // e.g., check if both users are verified, premium features, etc.
    }

    private Conversation getOrCreateConversation(User user1, User user2) {
        return conversationRepository
                .findConversationBetweenUsers(user1, user2)
                .orElseGet(() -> createNewConversation(user1, user2));
    }

    private Conversation createNewConversation(User user1, User user2) {
        Conversation conversation = Conversation.builder()
                .user1(user1)
                .user2(user2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return conversationRepository.save(conversation);
    }

    private void updateConversation(Conversation conversation, Message message) {
        conversation.setLastMessage(message.getContent());
        conversation.setLastMessageTime(message.getCreatedAt());
        conversation.setLastMessageBy(message.getSender().getId());

        // Update unread count for receiver
        if (conversation.getUser1().getId().equals(message.getReceiver().getId())) {
            conversation.setUnreadCountUser1(conversation.getUnreadCountUser1() + 1);
        } else {
            conversation.setUnreadCountUser2(conversation.getUnreadCountUser2() + 1);
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private void markMessagesAsRead(Conversation conversation, User user) {
        List<Message> unreadMessages = messageRepository
                .findByReceiverAndConversationAndReadFalse(user, conversation);

        for (Message message : unreadMessages) {
            message.setRead(true);
        }

        messageRepository.saveAll(unreadMessages);
    }

    private void resetUnreadCount(Conversation conversation, User user) {
        if (conversation.getUser1().getId().equals(user.getId())) {
            conversation.setUnreadCountUser1(0);
        } else {
            conversation.setUnreadCountUser2(0);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private User getOtherUserInConversation(Conversation conversation, User currentUser) {
        if (conversation.getUser1().getId().equals(currentUser.getId())) {
            return conversation.getUser2();
        } else {
            return conversation.getUser1();
        }
    }

    private void sendWebSocketNotification(Message message, Conversation conversation) {
        User receiver = message.getReceiver();

        // Send message to receiver
        MessageDto messageDto = mapToMessageDto(message, receiver.getId());
        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/messages",
                messageDto);

        // Send conversation update to both users
        ConversationDto conversationDtoForSender = mapToConversationDto(conversation, message.getSender().getId());
        ConversationDto conversationDtoForReceiver = mapToConversationDto(conversation, receiver.getId());

        messagingTemplate.convertAndSendToUser(
                message.getSender().getId().toString(),
                "/queue/conversations",
                conversationDtoForSender);

        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/conversations",
                conversationDtoForReceiver);
    }

    private void sendPushNotification(User sender, User receiver, Message message) {
        String contentPreview = message.getContent().length() > 50
                ? message.getContent().substring(0, 50) + "..."
                : message.getContent();

        eventPublisher.publishEvent(new com.punarmilan.backend.event.NotificationEvent(
                this,
                receiver,
                sender,
                "MESSAGE_RECEIVED",
                "New message: " + contentPreview,
                message));
    }

    private MessageDto mapToMessageDto(Message message, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        return MessageDto.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getEmail())
                .senderPhotoUrl(photoVisibilityService.getProfilePhoto(currentUser, message.getSender()))
                .receiverId(message.getReceiver().getId())
                .receiverName(message.getReceiver().getEmail())
                .receiverPhotoUrl(photoVisibilityService.getProfilePhoto(currentUser, message.getReceiver()))
                .conversationId(message.getConversation().getId())
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .read(message.isRead())
                .delivered(message.isDelivered())
                .repliedToMessageId(message.getRepliedToMessageId())
                .createdAt(message.getCreatedAt())
                .isMine(message.getSender().getId().equals(currentUserId))
                .build();
    }

    private ConversationDto mapToConversationDto(Conversation conversation, Long currentUserId) {
        // Find the other user without doing a redundant database lookup
        User otherUser;
        if (conversation.getUser1().getId().equals(currentUserId)) {
            otherUser = conversation.getUser2();
        } else {
            otherUser = conversation.getUser1();
        }

        int unreadCount = conversation.getUser1().getId().equals(currentUserId) ? conversation.getUnreadCountUser1()
                : conversation.getUnreadCountUser2();

        String otherUserName = otherUser != null ? otherUser.getEmail() : "Unknown User";

        User currentUser = userRepository.findById(currentUserId).orElse(null);

        return ConversationDto.builder()
                .id(conversation.getId())
                .user1Id(conversation.getUser1().getId())
                .user1Name(conversation.getUser1().getEmail())
                .user2Id(conversation.getUser2().getId())
                .user2Name(conversation.getUser2().getEmail())
                .otherUserId(otherUser != null ? otherUser.getId() : null)
                .otherUserName(otherUserName)
                .otherUserPhotoUrl(photoVisibilityService.getProfilePhoto(currentUser, otherUser))
                .lastMessage(conversation.getLastMessage())
                .lastMessageTime(conversation.getLastMessageTime())
                .lastMessageBy(conversation.getLastMessageBy())
                .unreadCount(unreadCount)
                .blocked(conversation.getBlocked())
                .blockedBy(conversation.getBlockedBy())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}