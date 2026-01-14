package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUser(User user);
    List<Profile> findByProfileCompleteTrue();
    List<Profile> findByGenderAndProfileCompleteTrue(String gender);
    List<Profile> findByCityAndProfileCompleteTrue(String city);
    List<Profile> findByReligionAndProfileCompleteTrue(String religion);
    List<Profile> findAllByProfileVisibility(String profileVisibility);
    List<Profile> findByVerificationStatus(Profile.VerificationStatus verificationStatus);
    
    // Add this method to count total profiles
    long count();  // This method is already provided by JpaRepository
    
    
    // Add these NEW methods:
    @Query("SELECT p FROM Profile p WHERE LOWER(p.city) = LOWER(:city) AND p.user.active = true")
    List<Profile> findByCityIgnoreCaseAndUserActiveTrue(@Param("city") String city);
    
    @Query("SELECT p FROM Profile p WHERE p.user.active = true AND p.profileComplete = true")
    List<Profile> findByActiveUsersWithCompleteProfile();
    
    @Query("SELECT p FROM Profile p WHERE p.gender = :gender AND p.user.active = true")
    List<Profile> findByGenderAndUserActive(@Param("gender") String gender);
    
    @Query("SELECT p FROM Profile p WHERE p.age BETWEEN :minAge AND :maxAge AND p.user.active = true")
    List<Profile> findByAgeRangeAndUserActive(@Param("minAge") int minAge, 
                                              @Param("maxAge") int maxAge);
}