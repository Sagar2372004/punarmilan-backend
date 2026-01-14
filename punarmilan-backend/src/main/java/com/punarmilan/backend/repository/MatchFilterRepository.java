package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.MatchFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchFilterRepository extends JpaRepository<MatchFilter, Long> {
    
    Optional<MatchFilter> findByUserId(Long userId);
    
    List<MatchFilter> findByUserIdAndCity(Long userId, String city);
    
    void deleteByUserId(Long userId);
}