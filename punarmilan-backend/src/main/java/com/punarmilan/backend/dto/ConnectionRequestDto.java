package com.punarmilan.backend.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConnectionRequestDto {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;  // Receiver's user ID

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}