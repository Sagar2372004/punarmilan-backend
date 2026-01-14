package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.MatchCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchCategoryRepository extends JpaRepository<MatchCategory, Long> {
    
    Optional<MatchCategory> findBySlug(String slug);
    
    List<MatchCategory> findByActiveTrueOrderBySortOrderAsc();
    
    List<MatchCategory> findByActiveTrueAndShowCountTrueOrderBySortOrderAsc();
    
    boolean existsBySlug(String slug);
}