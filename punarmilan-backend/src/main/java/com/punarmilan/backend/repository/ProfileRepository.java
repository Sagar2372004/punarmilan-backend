package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.Profile;
import com.punarmilan.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

        // Add these NEW methods:
        @Query("SELECT p FROM Profile p WHERE LOWER(p.city) = LOWER(:city) AND p.user.active = true")
        List<Profile> findByCityIgnoreCaseAndUserActiveTrue(@Param("city") String city);

        @Query("SELECT p FROM Profile p WHERE p.user.active = true AND p.user.hidden = false AND p.profileComplete = true")
        List<Profile> findByActiveUsersWithCompleteProfile();

        @Query("SELECT p FROM Profile p WHERE p.gender = :gender AND p.user.active = true")
        List<Profile> findByGenderAndUserActive(@Param("gender") String gender);

        @Query("SELECT p FROM Profile p WHERE (YEAR(CURRENT_DATE) - YEAR(p.dateOfBirth)) BETWEEN :minAge AND :maxAge AND p.user.active = true AND p.user.hidden = false")
        List<Profile> findByAgeRangeAndUserActive(@Param("minAge") int minAge, @Param("maxAge") int maxAge);

        @Query("SELECT p FROM Profile p WHERE p.gender = :gender " +
                        "AND p.profileComplete = true " +
                        "AND p.verificationStatus = com.punarmilan.backend.entity.Profile.VerificationStatus.VERIFIED "
                        +
                        "AND p.user.id <> :excludeUserId AND p.user.hidden = false")
        List<Profile> findEligibleProfiles(@Param("gender") String gender, @Param("excludeUserId") Long excludeUserId);

        Optional<Profile> findByUserId(Long userId);

        List<Profile> findByVerificationStatus(Profile.VerificationStatus verificationStatus);

        Page<Profile> findByVerificationStatus(Profile.VerificationStatus verificationStatus, Pageable pageable);

        List<Profile> findByProfileComplete(Boolean complete);

        @Query("SELECT p FROM Profile p WHERE p.fullName LIKE %:name%")
        List<Profile> findByFullNameContaining(@Param("name") String name);

        @Query("SELECT p FROM Profile p WHERE p.idProofNumber = :idProofNumber")
        Optional<Profile> findByIdProofNumber(@Param("idProofNumber") String idProofNumber);

        @Query("SELECT p FROM Profile p WHERE p.verifiedAt BETWEEN :startDate AND :endDate")
        List<Profile> findVerifiedBetweenDates(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(p) FROM Profile p WHERE p.verificationStatus = com.punarmilan.backend.entity.Profile.VerificationStatus.PENDING")
        long countPendingVerifications();

        long countByVerificationStatus(Profile.VerificationStatus status);

        @Query("SELECT p FROM Profile p WHERE p.createdAt >= :startDate")
        List<Profile> findNewProfilesSince(@Param("startDate") LocalDateTime startDate);

        @Query("SELECT p FROM Profile p WHERE p.user.id != :currentUserId " +
                        "AND p.user.active = true " +
                        "AND p.user.hidden = false " +
                        "AND p.user.createdAt >= :startDate " +
                        "ORDER BY p.user.createdAt DESC")
        Page<Profile> findNewRegistrations(@Param("currentUserId") Long currentUserId,
                        @Param("startDate") LocalDateTime startDate,
                        Pageable pageable);

        @Query("SELECT p FROM Profile p WHERE p.verificationStatus = com.punarmilan.backend.entity.Profile.VerificationStatus.PENDING ORDER BY p.createdAt ASC")
        Page<Profile> findPendingVerifications(Pageable pageable);

        Page<Profile> findByUserRoleNot(String role, Pageable pageable);

        Page<Profile> findByGenderIgnoreCase(String gender, Pageable pageable);

        List<Profile> findAllByUserIn(List<User> users);
}
