package com.punarmilan.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationSession implements Serializable {
    private UserRegisterRequest registerRequest;
    private String emailToken;
    private boolean emailVerified;
    private long createdAt;
}
