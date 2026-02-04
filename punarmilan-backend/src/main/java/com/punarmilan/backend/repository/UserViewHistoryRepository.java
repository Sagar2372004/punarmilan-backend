package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.entity.UserViewHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserViewHistoryRepository extends JpaRepository<UserViewHistory, Long> {

    Optional<UserViewHistory> findByViewerAndViewedUser(User viewer, User viewedUser);

    Page<UserViewHistory> findByViewer(User viewer, Pageable pageable);

    Page<UserViewHistory> findByViewedUser(User viewedUser, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT v.viewedUser.id) FROM UserViewHistory v WHERE v.viewer = :viewer")
    long countDistinctViewedUsers(@Param("viewer") User viewer);

    @Query("SELECT COUNT(v) FROM UserViewHistory v WHERE v.viewedUser = :user AND v.viewedAt >= :since")
    long countViewsSince(@Param("user") User user, @Param("since") LocalDateTime since);

    // Add this NEW method:
    @Query("SELECT v.viewedUser FROM UserViewHistory v WHERE v.viewer = :viewer " +
            "GROUP BY v.viewedUser.id ORDER BY MAX(v.viewedAt) DESC")
    Page<User> findRecentlyViewedUsers(@Param("viewer") User viewer, Pageable pageable);

    @Query("SELECT v FROM UserViewHistory v WHERE v.viewer = :viewer AND v.viewedAt >= :date")
    List<UserViewHistory> findTodayViews(@Param("viewer") User viewer, @Param("date") LocalDateTime date);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM UserViewHistory v WHERE v.viewer = :user OR v.viewedUser = :user")
    void deleteByUser(@Param("user") User user);
}