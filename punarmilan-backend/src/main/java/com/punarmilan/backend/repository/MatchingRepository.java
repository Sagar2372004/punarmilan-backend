package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface MatchingRepository extends JpaRepository<Profile, Long> {

        /**
         * Finds top 100 compatible candidate IDs and their scores using optimized
         * native SQL.
         * Excludes users with existing connection requests or those already viewed.
         */
        @Query(value = "SELECT p.user_id as userId, " +
                        " ( (CASE WHEN p.religion = :religion THEN 30 ELSE 0 END) + " +
                        "   (CASE WHEN p.education_level = :edu THEN 20 ELSE 0 END) + " +
                        "   (CASE WHEN p.marital_status = :marital THEN 15 ELSE 0 END) + " +
                        "   (CASE WHEN p.city = :city THEN 15 ELSE 0 END) + " +
                        "   (CASE WHEN p.working_with = :workingWith THEN 15 ELSE 0 END) + " + // NEW
                        "   (CASE WHEN u.is_premium = 1 THEN 20 ELSE 0 END) " +
                        " ) as matchScore " +
                        "FROM profiles p " +
                        "JOIN users u ON p.user_id = u.id " +
                        "WHERE u.is_active = 1 " +
                        "  AND u.id != :myId " +
                        "  AND p.gender = :prefGender " +
                        "  AND (YEAR(CURDATE()) - YEAR(p.date_of_birth)) BETWEEN :minAge AND :maxAge " +
                        "  AND (:religion IS NULL OR p.religion = :religion OR :religion = 'No Preference') " +
                        "  AND (:marital IS NULL OR p.marital_status = :marital OR :marital = 'No Preference') " +
                        "  AND (:workingWith IS NULL OR p.working_with = :workingWith OR :workingWith = 'No Preference') "
                        + // NEW
                        "  AND NOT EXISTS (SELECT 1 FROM connection_requests cr " +
                        "                  WHERE (cr.sender_id = :myId AND cr.receiver_id = p.user_id) " +
                        "                     OR (cr.sender_id = p.user_id AND cr.receiver_id = :myId)) " +
                        "  AND NOT EXISTS (SELECT 1 FROM user_view_history uvh " +
                        "                  WHERE uvh.viewer_id = :myId AND uvh.viewed_user_id = p.user_id) " +
                        "ORDER BY matchScore DESC " +
                        "LIMIT 100", nativeQuery = true)
        List<Map<String, Object>> findTopCompatibleCandidates(
                        @Param("myId") Long myId,
                        @Param("prefGender") String prefGender,
                        @Param("minAge") Integer minAge,
                        @Param("maxAge") Integer maxAge,
                        @Param("religion") String religion,
                        @Param("edu") String edu,
                        @Param("marital") String marital,
                        @Param("city") String city,
                        @Param("workingWith") String workingWith);
}
