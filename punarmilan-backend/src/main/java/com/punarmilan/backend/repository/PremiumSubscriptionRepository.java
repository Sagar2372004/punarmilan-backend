package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.PremiumSubscription;
import com.punarmilan.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PremiumSubscriptionRepository extends JpaRepository<PremiumSubscription, Long> {

    List<PremiumSubscription> findByUserAndStatus(User user, PremiumSubscription.SubscriptionStatus status);

    List<PremiumSubscription> findByUserOrderByCreatedAtDesc(User user);

    List<PremiumSubscription> findByUser(User user); // Add this if missing

    @Query("SELECT s FROM PremiumSubscription s WHERE s.status = :status AND s.endDate < :date")
    List<PremiumSubscription> findByStatusAndEndDateBefore(
            @Param("status") PremiumSubscription.SubscriptionStatus status,
            @Param("date") LocalDateTime date);

    Optional<PremiumSubscription> findByPaymentId(String paymentId);

    Optional<PremiumSubscription> findBySubscriptionId(String subscriptionId);

    @Query("SELECT COUNT(s) FROM PremiumSubscription s WHERE s.user = :user AND s.status = 'ACTIVE'")
    int countActiveSubscriptionsByUser(@Param("user") User user);

    @Query("SELECT s FROM PremiumSubscription s WHERE s.autoRenew = true AND s.status = 'ACTIVE' AND s.endDate < :date")
    List<PremiumSubscription> findSubscriptionsForRenewal(@Param("date") LocalDateTime date);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUser(User user);
}