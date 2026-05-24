package com.project.ecomapp.ecommerce_Project.user.controller;

import com.project.ecomapp.ecommerce_Project.Bean.CreateUserRequest;
import com.project.ecomapp.ecommerce_Project.Bean.SelfUserUpdateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserUpsertRequest;
import com.project.ecomapp.ecommerce_Project.user.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import org.springframework.security.core.Authentication;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(path = {"/api/users", "/Users"})
@Validated
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final CustomUserDetailsService userService;

    public UserController(CustomUserDetailsService userService) {
        this.userService = userService;
    }

    @GetMapping(path = {"", "/AllUsers"})
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        LOGGER.debug("Admin requested full user list");
        return userService.getAllUsers();
    }

    @GetMapping(path = "/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UserResponse getUserById(@PathVariable Integer id, Authentication authentication) {
        LOGGER.debug("User lookup requested targetId={} actor={}", id, authentication.getName());
        return userService.getUserById(id, authentication.getAuthorities());
    }

    @GetMapping(path = {"/me", "/currentuser"})
    @PreAuthorize("isAuthenticated()")
    public UserResponse getUserDetails(Principal principal) {
        LOGGER.debug("Current user profile requested email={}", principal.getName());
        return userService.getUserByEmail(principal.getName());
    }

    @PostMapping(path = {"", "/addUser"})
    public ResponseEntity<UserResponse> addNewUser(@Valid @RequestBody CreateUserRequest user) {
        LOGGER.info("Public registration request received email={}", user.email());
        UserResponse createdUser = userService.addNewUser(user.email(), user.password(), user.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PostMapping(path = "/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserUpsertRequest userRequest) {
        LOGGER.info("Registration alias request received email={}", userRequest.email());
        UserResponse createdUser = userService.addNewUser(userRequest.email(), userRequest.password(), userRequest.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping(path = "/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateCurrentUser(@Valid @RequestBody SelfUserUpdateRequest request, Principal principal) {
        LOGGER.info("Self-service user update requested email={}", principal.getName());
        return userService.updateCurrentUser(principal.getName(), request);
    }

    @PutMapping(path = {"/{id}", "/UpdateExistingUser"})
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UserResponse updateExistingUser(
            @PathVariable(name = "id", required = false) Integer id,
            @Valid @RequestBody UserUpsertRequest userRequest,
            Authentication authentication
    ) {
        LOGGER.info("Managed user update requested targetId={} actor={}", id, authentication.getName());
        return userService.updateExistingUser(id, userRequest, authentication.getAuthorities());
    }

    @DeleteMapping(path = {"/{id}", "/deleteUser/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void deleteExistingUser(@PathVariable Integer id, Authentication authentication) {
        LOGGER.warn("Managed user delete requested targetId={} actor={}", id, authentication.getName());
        userService.deleteExistingUser(id, authentication.getAuthorities());
    }

}
