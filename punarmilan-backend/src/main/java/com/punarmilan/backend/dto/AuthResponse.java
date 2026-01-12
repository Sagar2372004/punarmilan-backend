package com.punarmilan.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
	
	private String token;
//	
//    private String type;    // "Bearer" 
//    private String email;   // user email
//    private String roles;   // user role (USER/ADMIN)

}
