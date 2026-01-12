package com.punarmilan.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {
	@Email(message=" invalid email format ")
	@NotBlank(message ="email is required ")
	private String email;
	
	@NotBlank(message ="password is required")
	@Size(min = 6, message = "Password must be at least 6 characters")
	private String password;
	
	@NotBlank(message="mobileNumber is required")
	private String mobileNumber;
	 
}
	
