package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.ConnectionRequest;
import com.punarmilan.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {

        // Find pending request between two users
        Optional<ConnectionRequest> findBySenderAndReceiverAndStatus(
                        User sender, User receiver, ConnectionRequest.Status status);

        // Find any existing request between two users (any status)
        Optional<ConnectionRequest> findBySenderAndReceiver(User sender, User receiver);

        // Get all pending requests for a user
        Page<ConnectionRequest> findByReceiverAndStatus(
                        User receiver, ConnectionRequest.Status status, Pageable pageable);

        // Get all sent requests by a user
        Page<ConnectionRequest> findBySenderAndStatus(
                        User sender, ConnectionRequest.Status status, Pageable pageable);

        // Get all accepted connections for a user

        @Query("SELECT cr FROM ConnectionRequest cr " +
                        "WHERE (cr.sender = :user OR cr.receiver = :user) " +
                        "AND cr.status = 'ACCEPTED'")
        Page<ConnectionRequest> findConnectionsByUser(@Param("user") User user, Pageable pageable);

        // Count pending requests for a user
        long countByReceiverAndStatusAndReadFalse(
                        User receiver, ConnectionRequest.Status status);

        // Get requests that need to be expired
        List<ConnectionRequest> findByStatusAndSentAtBefore(
                        ConnectionRequest.Status status, LocalDateTime cutoffDate);

        // Check if two users are connected
        @Query("SELECT COUNT(cr) > 0 FROM ConnectionRequest cr " +
                        "WHERE ((cr.sender = :user1 AND cr.receiver = :user2) OR " +
                        "(cr.sender = :user2 AND cr.receiver = :user1)) " +
                        "AND cr.status = 'ACCEPTED'")
        boolean areUsersConnected(@Param("user1") User user1, @Param("user2") User user2);

        // Get mutual connections count
        @Query("SELECT COUNT(DISTINCT u) FROM User u " +
                        "WHERE u.id IN (" +
                        "  SELECT cr2.receiver.id FROM ConnectionRequest cr1 " +
                        "  JOIN ConnectionRequest cr2 ON cr1.receiver = cr2.sender " +
                        "  WHERE cr1.sender = :user AND cr1.status = 'ACCEPTED' " +
                        "  AND cr2.receiver = :user AND cr2.status = 'ACCEPTED'" +
                        ") OR u.id IN (" +
                        "  SELECT cr2.sender.id FROM ConnectionRequest cr1 " +
                        "  JOIN ConnectionRequest cr2 ON cr1.sender = cr2.receiver " +
                        "  WHERE cr1.receiver = :user AND cr1.status = 'ACCEPTED' " +
                        "  AND cr2.sender = :user AND cr2.status = 'ACCEPTED'" +
                        ")")
        long countMutualConnections(@Param("user") User user);

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        @Query("DELETE FROM ConnectionRequest cr WHERE cr.sender = :user OR cr.receiver = :user")
        void deleteByUser(@Param("user") User user);
}