package com.punarmilan.backend.config;

import com.punarmilan.backend.entity.MatchCategory;
import com.punarmilan.backend.repository.MatchCategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Order(1)
public class MatchCategorySeeder {
    
    private final MatchCategoryRepository matchCategoryRepository;
    
    @PostConstruct
    public void seedCategories() {
        if (matchCategoryRepository.count() == 0) {
            List<MatchCategory> categories = Arrays.asList(
                MatchCategory.builder()
                    .name("New Matches")
                    .slug("new")
                    .description("Recently joined users")
                    .icon("mdi-account-plus")
                    .color("#4CAF50")
                    .sortOrder(1)
                    .showCount(true)
                    .active(true)
                    .build(),
                    
                MatchCategory.builder()
                    .name("Today's Matches")
                    .slug("today")
                    .description("Active users today")
                    .icon("mdi-calendar-today")
                    .color("#2196F3")
                    .sortOrder(2)
                    .showCount(true)
                    .active(true)
                    .build(),
                    
                MatchCategory.builder()
                    .name("My Matches")
                    .slug("my")
                    .description("Your confirmed matches")
                    .icon("mdi-heart")
                    .color("#E91E63")
                    .sortOrder(3)
                    .showCount(true)
                    .active(true)
                    .build(),
                    
                MatchCategory.builder()
                    .name("Near Me")
                    .slug("near")
                    .description("Matches in your city")
                    .icon("mdi-map-marker")
                    .color("#FF9800")
                    .sortOrder(4)
                    .showCount(true)
                    .active(true)
                    .build(),
                    
                MatchCategory.builder()
                    .name("More Matches")
                    .slug("more")
                    .description("Discover more matches")
                    .icon("mdi-account-group")
                    .color("#9C27B0")
                    .sortOrder(5)
                    .showCount(true)
                    .active(true)
                    .build(),
                    
                MatchCategory.builder()
                    .name("Premium Matches")
                    .slug("premium")
                    .description("Exclusive premium members")
                    .icon("mdi-crown")
                    .color("#FFD700")
                    .sortOrder(6)
                    .showCount(false)
                    .active(true)
                    .build(),
                    
                MatchCategory.builder()
                    .name("Verified Profiles")
                    .slug("verified")
                    .description("Verified users only")
                    .icon("mdi-shield-check")
                    .color("#00BCD4")
                    .sortOrder(7)
                    .showCount(false)
                    .active(true)
                    .build(),
                    
                MatchCategory.builder()
                    .name("Recently Active")
                    .slug("recent")
                    .description("Recently active users")
                    .icon("mdi-clock")
                    .color("#607D8B")
                    .sortOrder(8)
                    .showCount(false)
                    .active(true)
                    .build()
            );
            
            matchCategoryRepository.saveAll(categories);
        }
    }
}