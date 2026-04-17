package com.project.ecomapp.ecommerce_Project.Controller;

import com.project.ecomapp.ecommerce_Project.Bean.UserJWTRequest;
import com.project.ecomapp.ecommerce_Project.Bean.UserJWTResponse;
import com.project.ecomapp.ecommerce_Project.Config.JWTUtils;
import com.project.ecomapp.ecommerce_Project.Services.CustomUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Validated
public class AuthenticatorController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JWTUtils jwtUtils;

    public AuthenticatorController(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService customUserDetailsService,
            JWTUtils jwtUtils
    ) {
        this.authenticationManager = authenticationManager;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping({"/api/auth/token", "/generateToken"})
    public ResponseEntity<UserJWTResponse> generateToken(@Valid @RequestBody UserJWTRequest jwtRequest) {
        authenticate(jwtRequest.getEmail(), jwtRequest.getPassword());
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(jwtRequest.getEmail());
        String token = jwtUtils.generateToken(userDetails);
        return ResponseEntity.ok(new UserJWTResponse(token));
    }

    private void authenticate(String username, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password",
                    exception
            );
        }
    }
}
