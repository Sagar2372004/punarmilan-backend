package com.punarmilan.backend.repository;

import com.punarmilan.backend.entity.PartnerPreference;
import com.punarmilan.backend.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnerPreferenceRepository extends JpaRepository<PartnerPreference, Long> {
	Optional<PartnerPreference> findByProfile(Profile profile);

	boolean existsByProfile(Profile profile);

	void deleteByProfile(Profile profile); // Add this method
}