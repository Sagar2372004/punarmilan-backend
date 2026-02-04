package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDto {
    private String url;
    private boolean blurred;
    private String restrictionReason; // PREMIUM_ONLY, LIKE_REQUIRED
}
