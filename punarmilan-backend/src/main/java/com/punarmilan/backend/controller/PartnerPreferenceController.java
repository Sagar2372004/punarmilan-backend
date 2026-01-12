package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.MatchResultDto;
import com.punarmilan.backend.dto.PartnerPreferenceRequestDto;
import com.punarmilan.backend.dto.PartnerPreferenceResponseDto;
import com.punarmilan.backend.service.PartnerPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PartnerPreferenceController {

    private final PartnerPreferenceService preferenceService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PartnerPreferenceResponseDto> saveOrUpdatePreferences(
            @RequestBody PartnerPreferenceRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(preferenceService.saveOrUpdatePreferences(requestDto));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PartnerPreferenceResponseDto> getMyPreferences() {
        return ResponseEntity.ok(preferenceService.getMyPreferences());
    }

    @GetMapping("/matches")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<MatchResultDto>> findMatches() {
        return ResponseEntity.ok(preferenceService.findMatches());
    }

    @GetMapping("/match-stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getMatchStats() {
        return ResponseEntity.ok(preferenceService.getMatchStats());
    }
}