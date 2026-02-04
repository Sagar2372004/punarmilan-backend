package com.punarmilan.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;

    @Column(name = "last_message_by")
    private Long lastMessageBy;

    @Column(name = "unread_count_user1")
    @Builder.Default
    private Integer unreadCountUser1 = 0;

    @Column(name = "unread_count_user2")
    @Builder.Default
    private Integer unreadCountUser2 = 0;

    @Column(name = "user1_deleted")
    @Builder.Default
    private Boolean user1Deleted = false;

    @Column(name = "user2_deleted")
    @Builder.Default
    private Boolean user2Deleted = false;

    @Column(name = "is_blocked")
    @Builder.Default
    private Boolean blocked = false;

    @Column(name = "blocked_by")
    private Long blockedBy;

    @Column(name = "block_reason", length = 500)
    private String blockReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("createdAt DESC")
    private List<Message> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}