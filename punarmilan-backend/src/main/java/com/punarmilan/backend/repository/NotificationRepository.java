package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.Notification;
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

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find notifications for user, ordered by creation time
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // Find unread notifications for user
    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);
    
    // Count unread notifications
    long countByUserAndReadFalse(User user);
    
    // Count unseen notifications
    long countByUserAndSeenFalse(User user);
    
    // Mark all notifications as read for user
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    int markAllAsRead(@Param("user") User user);
    
    // Mark all notifications as seen for user
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.seen = true WHERE n.user = :user AND n.seen = false")
    int markAllAsSeen(@Param("user") User user);
    
    // Delete old notifications
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("user") User user, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Find similar notifications (to avoid duplicates)
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.type = :type " +
           "AND n.relatedId = :relatedId ORDER BY n.createdAt DESC")
    List<Notification> findSimilarNotifications(@Param("user") User user,
                                                @Param("type") Notification.NotificationType type,
                                                @Param("relatedId") Long relatedId);
}