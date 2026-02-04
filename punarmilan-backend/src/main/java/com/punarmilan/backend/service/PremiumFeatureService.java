package com.punarmilan.backend.service;

import com.punarmilan.backend.entity.User;
import com.punarmilan.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PremiumFeatureService {

    private final UserRepository userRepository;

    public boolean canViewProfile(User viewer, User profileOwner) {
        // Premium users can view unlimited profiles
        if (Boolean.TRUE.equals(viewer.getPremium())) {
            return true;
        }
        
        // Free users have limited views
        return checkDailyViewLimit(viewer);
    }

    public boolean canSendMessage(User sender, User receiver) {
        // Premium users can send unlimited messages
        if (Boolean.TRUE.equals(sender.getPremium())) {
            return true;
        }
        
        // Free users have message limits
        return checkMessageLimit(sender);
    }

    public boolean canUseAdvancedSearch(User user) {
        // Only premium users can use advanced search
        return Boolean.TRUE.equals(user.getPremium());
    }

    public boolean canSeeWhoViewedProfile(User user) {
        // Only premium users can see who viewed their profile
        return Boolean.TRUE.equals(user.getPremium());
    }

    public int getDailyViewLimit(User user) {
        if (Boolean.TRUE.equals(user.getPremium())) {
            return Integer.MAX_VALUE; // Unlimited for premium
        }
        return 10; // 10 profiles per day for free users
    }

    public int getDailyMessageLimit(User user) {
        if (Boolean.TRUE.equals(user.getPremium())) {
            return Integer.MAX_VALUE; // Unlimited for premium
        }
        return 5; // 5 messages per day for free users
    }

    public List<String> getPremiumFeatures(User user) {
        List<String> premiumFeatures = List.of(
            "Unlimited profile views",
            "Unlimited messages",
            "Advanced search filters",
            "See who viewed your profile",
            "Priority customer support",
            "Increased daily matches",
            "Profile highlighting in search results",
            "Read receipts for messages"
        );
        
        List<String> freeFeatures = List.of(
            "Limited profile views (10/day)",
            "Limited messages (5/day)",
            "Basic search filters",
            "Standard customer support",
            "Create profile and upload photos",
            "Send connection requests",
            "Chat with connections"
        );
        
        if (Boolean.TRUE.equals(user.getPremium())) {
            return premiumFeatures;
        }
        
        return freeFeatures;
    }

    public String getCurrentPlan(User user) {
        if (Boolean.TRUE.equals(user.getPremium())) {
            return "PREMIUM";
        }
        return "FREE";
    }

    public boolean isPremiumUser(User user) {
        return Boolean.TRUE.equals(user.getPremium());
    }

    private boolean checkDailyViewLimit(User user) {
        // TODO: Implement logic to check daily view count from database
        // For now, return true (placeholder)
        return true;
    }

    private boolean checkMessageLimit(User user) {
        // TODO: Implement logic to check daily message count from database
        // For now, return true (placeholder)
        return true;
    }
}