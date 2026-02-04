package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.PaymentTransaction;
import com.punarmilan.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    List<PaymentTransaction> findByUserOrderByCreatedAtDesc(User user);

    List<PaymentTransaction> findByStatus(PaymentTransaction.PaymentStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUser(User user);
}