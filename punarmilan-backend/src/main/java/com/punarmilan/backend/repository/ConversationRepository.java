package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.Conversation;
import com.punarmilan.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

       // Find conversation between two users
       @Query("SELECT c FROM Conversation c WHERE " +
                     "(c.user1 = :user1 AND c.user2 = :user2) OR " +
                     "(c.user1 = :user2 AND c.user2 = :user1)")
       Optional<Conversation> findConversationBetweenUsers(@Param("user1") User user1,
                     @Param("user2") User user2);

       // Find all conversations for a user
       @Query("SELECT c FROM Conversation c WHERE " +
                     "(c.user1 = :user AND c.user1Deleted = false) OR " +
                     "(c.user2 = :user AND c.user2Deleted = false) " +
                     "ORDER BY COALESCE(c.lastMessageTime, c.updatedAt) DESC")
       Page<Conversation> findConversationsForUser(@Param("user") User user, Pageable pageable);

       // Find unread conversations for a user
       @Query("SELECT c FROM Conversation c WHERE " +
                     "((c.user1 = :user AND c.unreadCountUser1 > 0) OR " +
                     "(c.user2 = :user AND c.unreadCountUser2 > 0)) " +
                     "AND ((c.user1 = :user AND c.user1Deleted = false) OR " +
                     "(c.user2 = :user AND c.user2Deleted = false)) " +
                     "ORDER BY c.updatedAt DESC")
       List<Conversation> findUnreadConversations(@Param("user") User user);

       // Count total unread conversations
       @Query("SELECT COUNT(c) FROM Conversation c WHERE " +
                     "((c.user1 = :user AND c.unreadCountUser1 > 0) OR " +
                     "(c.user2 = :user AND c.unreadCountUser2 > 0)) " +
                     "AND ((c.user1 = :user AND c.user1Deleted = false) OR " +
                     "(c.user2 = :user AND c.user2Deleted = false))")
       long countUnreadConversations(@Param("user") User user);

       // Check if conversation exists between users
       @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
                     "FROM Conversation c WHERE " +
                     "(c.user1 = :user1 AND c.user2 = :user2) OR " +
                     "(c.user1 = :user2 AND c.user2 = :user1)")
       boolean existsBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

       // Simple find methods
       Optional<Conversation> findByUser1AndUser2(User user1, User user2);

       Optional<Conversation> findByUser2AndUser1(User user2, User user1);

       // Find conversations where user is either user1 or user2
       @Query("SELECT c FROM Conversation c WHERE c.user1 = :user OR c.user2 = :user")
       List<Conversation> findByUser1OrUser2(@Param("user") User user);

       // Find conversations by blocked status
       List<Conversation> findByBlockedTrue();

       // Find conversations by user with blocking status
       @Query("SELECT c FROM Conversation c WHERE (c.user1 = :user OR c.user2 = :user) AND c.blocked = :blocked")
       List<Conversation> findByUserAndBlocked(@Param("user") User user, @Param("blocked") Boolean blocked);

       @org.springframework.data.jpa.repository.Modifying
       @org.springframework.transaction.annotation.Transactional
       @Query("DELETE FROM Conversation c WHERE c.user1 = :user OR c.user2 = :user")
       void deleteByUser(@Param("user") User user);
}