package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}