package com.punarmilan.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.punarmilan.backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(String role);

    void deleteByEmail(String email);
    
    // ✅ FIXED: Use 'active' (field name) not 'isActive'
    List<User> findByCreatedAtAfterAndActiveTrue(LocalDateTime date);
    
    // ✅ FIXED: Use 'active' (field name) not 'isActive'
    List<User> findByLastLoginAfterAndActiveTrue(LocalDateTime date);
    
    // ✅ FIXED: Use 'active' (field name) not 'isActive'
    List<User> findByActiveTrue();
    
    // ✅ FIXED: Use 'u.active' (field name) not 'u.isActive'
    @Query("SELECT u FROM User u WHERE u.active = true AND u.id != :userId")
    List<User> findActiveUsersExcept(@Param("userId") Long userId);
    
    // ✅ FIXED: Use 'u.active' (field name) not 'u.isActive'
    @Query("SELECT u FROM User u WHERE u.active = true AND u.createdAt >= :date")
    List<User> findNewUsersSince(@Param("date") LocalDateTime date);
    
    // ✅ FIXED: Use 'u.active' (field name) not 'u.isActive'
    @Query("SELECT u FROM User u WHERE u.active = true AND u.lastLogin >= :date")
    List<User> findActiveUsersSince(@Param("date") LocalDateTime date);
}