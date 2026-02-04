package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.Match;
import com.punarmilan.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

       // ==================== BASIC QUERIES ====================

       Optional<Match> findByUser1AndUser2(User user1, User user2);

       Optional<Match> findByUser2AndUser1(User user2, User user1);

       /**
        * Find match between two users regardless of order
        */
       default Optional<Match> findMatchBetweenUsers(User user1, User user2) {
              Optional<Match> match = findByUser1AndUser2(user1, user2);
              if (match.isEmpty()) {
                     match = findByUser2AndUser1(user1, user2);
              }
              return match;
       }

       List<Match> findByUser1OrUser2(User user1, User user2);

       // ==================== STATUS-BASED QUERIES ====================

       Page<Match> findByStatus(Match.MatchStatus status, Pageable pageable);

       Page<Match> findByUser1AndStatus(User user1, Match.MatchStatus status, Pageable pageable);

       Page<Match> findByUser2AndStatus(User user2, Match.MatchStatus status, Pageable pageable);

       @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) AND m.status = :status")
       Page<Match> findByUserAndStatus(@Param("user") User user,
                     @Param("status") Match.MatchStatus status,
                     Pageable pageable);

       // ==================== MATCHED (CONFIRMED) QUERIES ====================

       @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) " +
                     "AND m.matched = true AND m.active = true")
       Page<Match> findConfirmedMatchesByUser(@Param("user") User user, Pageable pageable);

       @Query("SELECT COUNT(m) FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) " +
                     "AND m.matched = true AND m.active = true")
       long countConfirmedMatches(@Param("user") User user);

       @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) " +
                     "AND m.matched = true AND m.active = true " +
                     "AND m.createdAt >= :since")
       Page<Match> findRecentConfirmedMatches(@Param("user") User user,
                     @Param("since") LocalDateTime since,
                     Pageable pageable);

       // ==================== PENDING LIKES QUERIES ====================

       // User liked someone but not yet matched
       @Query("SELECT m FROM Match m WHERE " +
                     "((m.user1 = :user AND m.user1Liked = true AND m.user2Liked = false) OR " +
                     "(m.user2 = :user AND m.user2Liked = true AND m.user1Liked = false)) " +
                     "AND m.active = true AND m.blocked = false")
       Page<Match> findPendingLikesByUser(@Param("user") User user, Pageable pageable);

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "((m.user1 = :user AND m.user1Liked = true AND m.user2Liked = false) OR " +
                     "(m.user2 = :user AND m.user2Liked = true AND m.user1Liked = false)) " +
                     "AND m.active = true AND m.blocked = false")
       long countPendingLikesSent(@Param("user") User user);

       // Likes received (someone liked user but user hasn't responded)
       @Query("SELECT m FROM Match m WHERE " +
                     "((m.user2 = :user AND m.user1Liked = true AND m.user2Liked = false) OR " +
                     "(m.user1 = :user AND m.user2Liked = true AND m.user1Liked = false)) " +
                     "AND m.active = true AND m.blocked = false")
       Page<Match> findLikesReceived(@Param("user") User user, Pageable pageable);

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "((m.user2 = :user AND m.user1Liked = true AND m.user2Liked = false) OR " +
                     "(m.user1 = :user AND m.user2Liked = true AND m.user1Liked = false)) " +
                     "AND m.active = true AND m.blocked = false")
       long countLikesReceived(@Param("user") User user);

       // ==================== SUPER LIKES QUERIES ====================

       @Query("SELECT m FROM Match m WHERE " +
                     "(m.user1 = :user AND m.user2SuperLiked = true) OR " +
                     "(m.user2 = :user AND m.user1SuperLiked = true)")
       Page<Match> findSuperLikesReceived(@Param("user") User user, Pageable pageable);

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "(m.user1 = :user AND m.user2SuperLiked = true) OR " +
                     "(m.user2 = :user AND m.user1SuperLiked = true)")
       long countSuperLikesReceived(@Param("user") User user);

       // ==================== ACTIVE/INACTIVE QUERIES ====================

       Page<Match> findByActiveTrue(Pageable pageable);

       Page<Match> findByActiveFalse(Pageable pageable);

       @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) AND m.active = true")
       Page<Match> findActiveMatchesByUser(@Param("user") User user, Pageable pageable);

       @Query("SELECT m FROM Match m WHERE (m.user1 = :user OR m.user2 = :user) AND m.active = false")
       Page<Match> findInactiveMatchesByUser(@Param("user") User user, Pageable pageable);

       // ==================== BLOCKED QUERIES ====================

       @Query("SELECT m FROM Match m WHERE " +
                     "(m.user1 = :user OR m.user2 = :user) AND m.blocked = true")
       Page<Match> findBlockedMatchesByUser(@Param("user") User user, Pageable pageable);

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "(m.user1 = :user OR m.user2 = :user) AND m.blocked = true")
       long countBlockedMatches(@Param("user") User user);

       // ==================== CONNECTION CHECK QUERIES ====================

       @Query("SELECT COUNT(m) > 0 FROM Match m WHERE " +
                     "((m.user1 = :user1 AND m.user2 = :user2) OR " +
                     "(m.user1 = :user2 AND m.user2 = :user1)) " +
                     "AND m.matched = true AND m.active = true AND m.blocked = false")
       boolean areUsersMatched(@Param("user1") User user1, @Param("user2") User user2);

       @Query("SELECT COUNT(m) > 0 FROM Match m WHERE " +
                     "((m.user1 = :user1 AND m.user2 = :user2) OR " +
                     "(m.user1 = :user2 AND m.user2 = :user1)) " +
                     "AND m.user1Liked = true AND m.user2Liked = true")
       boolean haveUsersLikedEachOther(@Param("user1") User user1, @Param("user2") User user2);

       @Query("SELECT m FROM Match m WHERE " +
                     "((m.user1 = :user1 AND m.user2 = :user2) OR " +
                     "(m.user1 = :user2 AND m.user2 = :user1)) " +
                     "AND m.active = true")
       Optional<Match> findActiveMatchBetweenUsers(@Param("user1") User user1,
                     @Param("user2") User user2);

       // ==================== UNREAD MESSAGES QUERIES ====================

       @Query("SELECT m FROM Match m WHERE " +
                     "(m.user1 = :user AND m.unreadCountUser1 > 0) OR " +
                     "(m.user2 = :user AND m.unreadCountUser2 > 0)")
       Page<Match> findMatchesWithUnreadMessages(@Param("user") User user, Pageable pageable);

       @Query("SELECT SUM(CASE WHEN m.user1 = :user THEN m.unreadCountUser1 " +
                     "WHEN m.user2 = :user THEN m.unreadCountUser2 ELSE 0 END) " +
                     "FROM Match m WHERE m.active = true")
       Long totalUnreadMessages(@Param("user") User user);

       // ==================== TIME-BASED QUERIES ====================

       @Query("SELECT m FROM Match m WHERE m.createdAt >= :startDate AND m.createdAt <= :endDate")
       Page<Match> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate,
                     Pageable pageable);

       @Query("SELECT m FROM Match m WHERE m.matchedAt >= :startDate AND m.matchedAt <= :endDate")
       Page<Match> findByMatchedAtBetween(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate,
                     Pageable pageable);

       @Query("SELECT m FROM Match m WHERE m.updatedAt >= :since")
       Page<Match> findByUpdatedAfter(@Param("since") LocalDateTime since, Pageable pageable);

       // Today's matches
       @Query("SELECT m FROM Match m WHERE " +
                     "(m.user1 = :user OR m.user2 = :user) " +
                     "AND m.matched = true AND DATE(m.matchedAt) = CURRENT_DATE")
       Page<Match> findTodaysMatches(@Param("user") User user, Pageable pageable);

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "(m.user1 = :user OR m.user2 = :user) " +
                     "AND m.matched = true AND DATE(m.matchedAt) = CURRENT_DATE")
       long countTodaysMatches(@Param("user") User user);

       // Recent matches (last 7 days)
       @Query("SELECT m FROM Match m WHERE " +
                     "(m.user1 = :user OR m.user2 = :user) " +
                     "AND m.matched = true AND m.matchedAt >= :weekAgo")
       List<Match> findRecentMatches(@Param("user") User user,
                     @Param("weekAgo") LocalDateTime weekAgo);

       // ==================== COMPATIBILITY QUERIES ====================

       @Query("SELECT m FROM Match m WHERE " +
                     "(m.user1 = :user OR m.user2 = :user) " +
                     "AND m.compatibilityScore >= :minScore " +
                     "AND m.active = true AND m.matched = true " +
                     "ORDER BY m.compatibilityScore DESC")
       Page<Match> findHighCompatibilityMatches(@Param("user") User user,
                     @Param("minScore") Integer minScore,
                     Pageable pageable);

       @Query("SELECT AVG(m.compatibilityScore) FROM Match m WHERE " +
                     "(m.user1 = :user OR m.user2 = :user) AND m.matched = true")
       Double findAverageCompatibilityScore(@Param("user") User user);

       // ==================== MUTUAL CONNECTIONS QUERIES ====================

       @Query("SELECT m2 FROM Match m1 JOIN Match m2 ON " +
                     "(m1.user1 = :user OR m1.user2 = :user) AND " +
                     "((m2.user1 = m1.user1 AND m2.user2 = :otherUser) OR " +
                     "(m2.user1 = :otherUser AND m2.user2 = m1.user1) OR " +
                     "(m2.user1 = m1.user2 AND m2.user2 = :otherUser) OR " +
                     "(m2.user1 = :otherUser AND m2.user2 = m1.user2)) " +
                     "WHERE m1.matched = true AND m2.matched = true")
       List<Match> findMutualMatches(@Param("user") User user, @Param("otherUser") User otherUser);

       @Query("SELECT COUNT(DISTINCT CASE " +
                     "WHEN m1.user1 = :user THEN m2.user1 " +
                     "WHEN m1.user2 = :user THEN m2.user2 " +
                     "END) FROM Match m1 " +
                     "JOIN Match m2 ON (m1.user1 = m2.user1 OR m1.user1 = m2.user2 OR " +
                     "m1.user2 = m2.user1 OR m1.user2 = m2.user2) " +
                     "WHERE (m1.user1 = :targetUser OR m1.user2 = :targetUser) " +
                     "AND (m2.user1 != :user AND m2.user2 != :user) " +
                     "AND m1.matched = true AND m2.matched = true")
       Long countMutualConnections(@Param("user") User user, @Param("targetUser") User targetUser);

       // ==================== EXPIRED QUERIES ====================

       @Query("SELECT m FROM Match m WHERE m.expiresAt IS NOT NULL AND m.expiresAt < :now")
       List<Match> findExpiredMatches(@Param("now") LocalDateTime now);

       // ==================== STATISTICS QUERIES ====================

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "(m.user1 = :user OR m.user2 = :user) " +
                     "AND m.active = true")
       long countTotalMatches(@Param("user") User user);

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "(m.user1 = :user AND m.user1Liked = true) OR " +
                     "(m.user2 = :user AND m.user2Liked = true)")
       long countTotalLikesSent(@Param("user") User user);

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "(m.user2 = :user AND m.user1Liked = true) OR " +
                     "(m.user1 = :user AND m.user2Liked = true)")
       long countTotalLikesReceived(@Param("user") User user);

       @Query("SELECT COUNT(m) FROM Match m WHERE " +
                     "m.matched = true AND DATE(m.createdAt) = CURRENT_DATE")
       long countDailyMatches();

       // Match success rate (likes that turned into matches)
       @Query("SELECT CASE WHEN COUNT(m) = 0 THEN 0 ELSE " +
                     "(SUM(CASE WHEN m.matched = true THEN 1 ELSE 0 END) * 100.0 / COUNT(m)) END " +
                     "FROM Match m WHERE m.user1 = :user OR m.user2 = :user")
       Double calculateMatchSuccessRate(@Param("user") User user);

       // ==================== ADMIN QUERIES ====================

       @Query("SELECT m FROM Match m WHERE " +
                     "(:userId IS NULL OR m.user1.id = :userId OR m.user2.id = :userId) " +
                     "AND (:status IS NULL OR m.status = :status) " +
                     "AND (:matched IS NULL OR m.matched = :matched) " +
                     "AND (:blocked IS NULL OR m.blocked = :blocked) " +
                     "AND (:active IS NULL OR m.active = :active)")
       Page<Match> searchMatches(@Param("userId") Long userId,
                     @Param("status") Match.MatchStatus status,
                     @Param("matched") Boolean matched,
                     @Param("blocked") Boolean blocked,
                     @Param("active") Boolean active,
                     Pageable pageable);

       @Query("SELECT COUNT(m) FROM Match m WHERE DATE(m.createdAt) = CURRENT_DATE")
       long countMatchesCreatedToday();

       @Query("SELECT COUNT(m) FROM Match m WHERE DATE(m.matchedAt) = CURRENT_DATE")
       long countMatchesMatchedToday();

       // ==================== UPDATE QUERIES ====================

       @Modifying
       @Transactional
       @Query("UPDATE Match m SET m.lastMessage = :message, m.lastMessageAt = :timestamp " +
                     "WHERE m.id = :matchId")
       void updateLastMessage(@Param("matchId") Long matchId,
                     @Param("message") String message,
                     @Param("timestamp") LocalDateTime timestamp);

       @Modifying
       @Transactional
       @Query("UPDATE Match m SET m.unreadCountUser1 = 0 WHERE m.id = :matchId")
       void resetUnreadCountUser1(@Param("matchId") Long matchId);

       @Modifying
       @Transactional
       @Query("UPDATE Match m SET m.unreadCountUser2 = 0 WHERE m.id = :matchId")
       void resetUnreadCountUser2(@Param("matchId") Long matchId);

       @Modifying
       @Transactional
       @Query("UPDATE Match m SET m.active = false WHERE m.id = :matchId")
       void deactivateMatch(@Param("matchId") Long matchId);

       @Modifying
       @Transactional
       @Query("UPDATE Match m SET m.status = 'EXPIRED' WHERE m.expiresAt < :now AND m.active = true")
       int expireOldMatches(@Param("now") LocalDateTime now);

       // ==================== POTENTIAL MATCHES (NATIVE QUERY) ====================

       @Query(value = "SELECT u.* FROM users u " +
                     "INNER JOIN profiles p ON u.id = p.user_id " +
                     "WHERE u.id != :currentUserId " +
                     "AND u.active = 1 " +
                     "AND p.profile_complete = 1 " +
                     "AND (:preferredGender IS NULL OR p.gender = :preferredGender) " +
                     "AND u.id NOT IN ( " +
                     "    SELECT DISTINCT " +
                     "        CASE " +
                     "            WHEN m.user1_id = :currentUserId THEN m.user2_id " +
                     "            WHEN m.user2_id = :currentUserId THEN m.user1_id " +
                     "        END " +
                     "    FROM matches m " +
                     "    WHERE (m.user1_id = :currentUserId OR m.user2_id = :currentUserId) " +
                     "    AND m.active = 1 " +
                     ") " +
                     "ORDER BY RAND()", countQuery = "SELECT COUNT(u.id) FROM users u " +
                                   "INNER JOIN profiles p ON u.id = p.user_id " +
                                   "WHERE u.id != :currentUserId " +
                                   "AND u.active = 1 " +
                                   "AND p.profile_complete = 1 " +
                                   "AND (:preferredGender IS NULL OR p.gender = :preferredGender) " +
                                   "AND u.id NOT IN ( " +
                                   "    SELECT DISTINCT " +
                                   "        CASE " +
                                   "            WHEN m.user1_id = :currentUserId THEN m.user2_id " +
                                   "            WHEN m.user2_id = :currentUserId THEN m.user1_id " +
                                   "        END " +
                                   "    FROM matches m " +
                                   "    WHERE (m.user1_id = :currentUserId OR m.user2_id = :currentUserId) " +
                                   "    AND m.active = 1 " +
                                   ")", nativeQuery = true)
       Page<User> findPotentialMatchesNative(@Param("currentUserId") Long currentUserId,
                     @Param("preferredGender") String preferredGender,
                     Pageable pageable);

       @Modifying
       @Transactional
       @Query("DELETE FROM Match m WHERE m.user1 = :user OR m.user2 = :user")
       void deleteByUser(@Param("user") User user);
}