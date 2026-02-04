package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.Conversation;
import com.punarmilan.backend.entity.Message;
import com.punarmilan.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

       // Find messages in a conversation
       Page<Message> findByConversationOrderByCreatedAtDesc(Conversation conversation, Pageable pageable);

       // Find unread messages for a user in a conversation
       List<Message> findByReceiverAndConversationAndReadFalse(User receiver, Conversation conversation);

       // Count unread messages for a user
       @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.read = false")
       long countUnreadMessages(@Param("user") User user);

       // Count unread messages in a conversation for a specific user
       @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conversation " +
                     "AND m.receiver = :user AND m.read = false")
       long countUnreadMessagesInConversation(@Param("conversation") Conversation conversation,
                     @Param("user") User user);

       // Mark messages as read
       @Modifying
       @Transactional
       @Query("UPDATE Message m SET m.read = true WHERE m.conversation = :conversation " +
                     "AND m.receiver = :receiver AND m.read = false")
       int markConversationAsRead(@Param("conversation") Conversation conversation,
                     @Param("receiver") User receiver);

       // Get last message in conversation
       Optional<Message> findTopByConversationOrderByCreatedAtDesc(Conversation conversation);

       // Delete messages older than specified date
       @Modifying
       @Transactional
       @Query("DELETE FROM Message m WHERE m.createdAt < :date")
       int deleteOldMessages(@Param("date") LocalDateTime date);

       // Find messages for user (both sent and received)
       @Query("SELECT m FROM Message m WHERE (m.sender = :user OR m.receiver = :user) " +
                     "ORDER BY m.createdAt DESC")
       Page<Message> findMessagesForUser(@Param("user") User user, Pageable pageable);

       // Find messages by conversation
       List<Message> findByConversation(Conversation conversation);

       @Modifying
       @Transactional
       @Query("DELETE FROM Message m WHERE m.sender = :user OR m.receiver = :user")
       void deleteByUser(@Param("user") User user);
}