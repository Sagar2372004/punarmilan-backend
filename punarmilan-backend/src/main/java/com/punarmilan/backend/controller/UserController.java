package com.punarmilan.backend.controller;

import com.punarmilan.backend.dto.AuthResponse;
import com.punarmilan.backend.dto.UserLoginRequest;
import com.punarmilan.backend.dto.UserRegisterRequest;
import com.punarmilan.backend.dto.UserResponse;
import com.punarmilan.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //  REGISTER
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody UserRegisterRequest request) {

        UserResponse response = userService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    //  LOGIN (JWT)
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody UserLoginRequest request) {

        AuthResponse response = userService.loginUser(request);
        return ResponseEntity.ok(response);
    }
}
