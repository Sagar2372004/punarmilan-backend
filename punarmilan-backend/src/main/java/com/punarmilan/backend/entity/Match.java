package com.punarmilan.backend.entity;

import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "matches", uniqueConstraints = @UniqueConstraint(columnNames = { "user1_id", "user2_id" }), indexes = {
        @Index(name = "idx_match_users", columnList = "user1_id, user2_id"),
        @Index(name = "idx_match_status", columnList = "status"),
        @Index(name = "idx_match_created", columnList = "created_at"),
        @Index(name = "idx_match_updated", columnList = "updated_at"),
        @Index(name = "idx_match_matched", columnList = "matched_at"),
        @Index(name = "idx_both_liked", columnList = "user1_liked, user2_liked")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "user1", "user2" })
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.PENDING;

    @Column(name = "user1_liked", nullable = false)
    @Builder.Default
    private boolean user1Liked = false;

    @Column(name = "user2_liked", nullable = false)
    @Builder.Default
    private boolean user2Liked = false;

    @Column(name = "user1_super_liked")
    @Builder.Default
    private boolean user1SuperLiked = false;

    @Column(name = "user2_super_liked")
    @Builder.Default
    private boolean user2SuperLiked = false;

    @Column(name = "is_matched", nullable = false)
    @Builder.Default
    private boolean matched = false;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "last_message", length = 1000)
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "unread_count_user1", nullable = false)
    @Builder.Default
    private Integer unreadCountUser1 = 0;

    @Column(name = "unread_count_user2", nullable = false)
    @Builder.Default
    private Integer unreadCountUser2 = 0;

    @Column(name = "user1_deleted")
    @Builder.Default
    private boolean user1Deleted = false;

    @Column(name = "user2_deleted")
    @Builder.Default
    private boolean user2Deleted = false;

    @Column(name = "is_blocked")
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "blocked_by")
    private Long blockedBy;

    @Column(name = "block_reason", length = 500)
    private String blockReason;

    @Column(name = "compatibility_score")
    private Integer compatibilityScore;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "user1_rated")
    @Builder.Default
    private boolean user1Rated = false;

    @Column(name = "user2_rated")
    @Builder.Default
    private boolean user2Rated = false;

    @Column(name = "user1_rating")
    private Integer user1Rating;

    @Column(name = "user2_rating")
    private Integer user2Rating;

    @Column(name = "user1_feedback", length = 1000)
    private String user1Feedback;

    @Column(name = "user2_feedback", length = 1000)
    private String user2Feedback;

    @Column(name = "user1_viewed_profile")
    @Builder.Default
    private boolean user1ViewedProfile = false;

    @Column(name = "user2_viewed_profile")
    @Builder.Default
    private boolean user2ViewedProfile = false;

    @Column(name = "user1_last_seen")
    private LocalDateTime user1LastSeen;

    @Column(name = "user2_last_seen")
    private LocalDateTime user2LastSeen;

    @Column(name = "user1_typing")
    @Builder.Default
    private boolean user1Typing = false;

    @Column(name = "user2_typing")
    @Builder.Default
    private boolean user2Typing = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "match_source", length = 50)
    private String matchSource; // "search", "suggestion", "premium", "featured"

    @ElementCollection
    @CollectionTable(name = "match_interests", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "interest")
    @Builder.Default
    private Set<String> commonInterests = new HashSet<>();

    @Column(name = "location_distance")
    private Double locationDistance;

    @Column(name = "location_distance_unit", length = 10)
    private String locationDistanceUnit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Enum for match status
    public enum MatchStatus {
        PENDING, // One user liked, waiting for response
        MATCHED, // Both users liked each other
        REJECTED, // One user rejected
        BLOCKED, // One user blocked the other
        EXPIRED, // Match expired (e.g., after 30 days)
        ARCHIVED, // Match archived by user
        SUPER_LIKED, // One user super liked
        REPORTED // Match reported for inappropriate behavior
    }

    @PrePersist
    @PreUpdate
    public void updateMatchStatus() {
        // Update matched status
        if (user1Liked && user2Liked && !matched) {
            matched = true;
            status = MatchStatus.MATCHED;
            matchedAt = LocalDateTime.now();

            // Set expiration (e.g., 30 days from match)
            expiresAt = LocalDateTime.now().plusDays(30);
        } else if (matched && (!user1Liked || !user2Liked)) {
            matched = false;
            matchedAt = null;
            expiresAt = null;
            status = MatchStatus.PENDING;
        }

        // Update status based on blocked
        if (blocked) {
            status = MatchStatus.BLOCKED;
        }

        // Update active status
        active = !user1Deleted && !user2Deleted && !blocked && status != MatchStatus.ARCHIVED;
    }

    // Helper methods

    /**
     * Check if user has liked in this match
     */
    public boolean hasUserLiked(Long userId) {
        if (user1.getId().equals(userId)) {
            return user1Liked;
        } else if (user2.getId().equals(userId)) {
            return user2Liked;
        }
        return false;
    }

    /**
     * Check if user has super liked in this match
     */
    public boolean hasUserSuperLiked(Long userId) {
        if (user1.getId().equals(userId)) {
            return user1SuperLiked;
        } else if (user2.getId().equals(userId)) {
            return user2SuperLiked;
        }
        return false;
    }

    /**
     * Get the other user in the match
     */
    public User getOtherUser(Long userId) {
        if (user1.getId().equals(userId)) {
            return user2;
        } else if (user2.getId().equals(userId)) {
            return user1;
        }
        return null;
    }

    /**
     * Check if user is a participant in this match
     */
    public boolean isParticipant(Long userId) {
        return user1.getId().equals(userId) || user2.getId().equals(userId);
    }

    /**
     * Like a user in the match
     */
    public void likeUser(Long userId, boolean isSuperLike) {
        if (user1.getId().equals(userId)) {
            user1Liked = true;
            user1SuperLiked = isSuperLike;
        } else if (user2.getId().equals(userId)) {
            user2Liked = true;
            user2SuperLiked = isSuperLike;
        }
        updateMatchStatus();
    }

    /**
     * Unlike a user in the match
     */
    public void unlikeUser(Long userId) {
        if (user1.getId().equals(userId)) {
            user1Liked = false;
            user1SuperLiked = false;
        } else if (user2.getId().equals(userId)) {
            user2Liked = false;
            user2SuperLiked = false;
        }
        updateMatchStatus();
    }

    /**
     * Block a user in the match
     */
    public void blockUser(Long userId, String reason) {
        blocked = true;
        blockedBy = userId;
        blockReason = reason;
        status = MatchStatus.BLOCKED;
    }

    /**
     * Unblock the match
     */
    public void unblock() {
        blocked = false;
        blockedBy = null;
        blockReason = null;
        status = matched ? MatchStatus.MATCHED : MatchStatus.PENDING;
    }

    /**
     * Delete match for a user (soft delete)
     */
    public void deleteForUser(Long userId) {
        if (user1.getId().equals(userId)) {
            user1Deleted = true;
        } else if (user2.getId().equals(userId)) {
            user2Deleted = true;
        }
        active = !user1Deleted && !user2Deleted && !blocked;
    }

    /**
     * Restore match for a user
     */
    public void restoreForUser(Long userId) {
        if (user1.getId().equals(userId)) {
            user1Deleted = false;
        } else if (user2.getId().equals(userId)) {
            user2Deleted = false;
        }
        active = !user1Deleted && !user2Deleted && !blocked;
    }

    /**
     * Get unread count for a user
     */
    public Integer getUnreadCount(Long userId) {
        if (user1.getId().equals(userId)) {
            return unreadCountUser1;
        } else if (user2.getId().equals(userId)) {
            return unreadCountUser2;
        }
        return 0;
    }

    /**
     * Increment unread count for a user
     */
    public void incrementUnreadCount(Long userId) {
        if (user1.getId().equals(userId)) {
            unreadCountUser1++;
        } else if (user2.getId().equals(userId)) {
            unreadCountUser2++;
        }
    }

    /**
     * Reset unread count for a user
     */
    public void resetUnreadCount(Long userId) {
        if (user1.getId().equals(userId)) {
            unreadCountUser1 = 0;
        } else if (user2.getId().equals(userId)) {
            unreadCountUser2 = 0;
        }
    }

    /**
     * Update last message
     */
    public void updateLastMessage(String message, Long senderId) {
        this.lastMessage = message;
        this.lastMessageAt = LocalDateTime.now();

        // Increment unread count for receiver
        if (user1.getId().equals(senderId)) {
            unreadCountUser2++;
        } else if (user2.getId().equals(senderId)) {
            unreadCountUser1++;
        }
    }

    /**
     * Set user typing status
     */
    public void setUserTyping(Long userId, boolean typing) {
        if (user1.getId().equals(userId)) {
            user1Typing = typing;
        } else if (user2.getId().equals(userId)) {
            user2Typing = typing;
        }
    }

    /**
     * Set user last seen
     */
    public void setUserLastSeen(Long userId) {
        if (user1.getId().equals(userId)) {
            user1LastSeen = LocalDateTime.now();
        } else if (user2.getId().equals(userId)) {
            user2LastSeen = LocalDateTime.now();
        }
    }

    /**
     * Rate the other user
     */
    public void rateUser(Long userId, Integer rating, String feedback) {
        if (user1.getId().equals(userId)) {
            user1Rated = true;
            user1Rating = rating;
            user1Feedback = feedback;
        } else if (user2.getId().equals(userId)) {
            user2Rated = true;
            user2Rating = rating;
            user2Feedback = feedback;
        }
    }

    /**
     * Check if match is expired
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Check if user can chat (both liked and not blocked)
     */
    public boolean canChat(Long userId) {
        return matched && !blocked && isParticipant(userId) && !isExpired();
    }

    /**
     * Get match compatibility text
     */
    public String getCompatibilityText() {
        if (compatibilityScore == null)
            return "Not calculated";
        if (compatibilityScore >= 90)
            return "Excellent Match";
        if (compatibilityScore >= 80)
            return "Great Match";
        if (compatibilityScore >= 70)
            return "Good Match";
        if (compatibilityScore >= 60)
            return "Fair Match";
        return "Low Compatibility";
    }
}