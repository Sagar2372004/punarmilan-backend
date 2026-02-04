package com.punarmilan.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserActionRequest {
    @NotNull(message = "UserId is required")
    private Long userId;
    
    @NotBlank(message = "Action is required")
    private String action; // BLOCK, UNBLOCK, DELETE, MAKE_PREMIUM, REMOVE_PREMIUM
    
    private String reason;
}