package com.project.ecomapp.ecommerce_Project.Controller;

import com.project.ecomapp.ecommerce_Project.Bean.CreateUserRequest;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserUpsertRequest;
import com.project.ecomapp.ecommerce_Project.Services.CustomUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(path = {"/api/users", "/Users"})
@Validated
public class UserController {

    private final CustomUserDetailsService userService;

    public UserController(CustomUserDetailsService userService) {
        this.userService = userService;
    }

    @GetMapping(path = {"", "/AllUsers"})
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping(path = {"/me", "/currentuser"})
    public UserResponse getUserDetails(Principal principal) {
        return userService.getUserByEmail(principal.getName());
    }

    @PostMapping(path = {"", "/addUser"})
    public ResponseEntity<UserResponse> addNewUser(@Valid @RequestBody CreateUserRequest user) {
        UserResponse createdUser = userService.addNewUser(user.email(), user.password(), user.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PostMapping(path = "/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserUpsertRequest userRequest) {
        UserResponse createdUser = userService.addNewUser(userRequest.email(), userRequest.password(), userRequest.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping(path = {"/{id}", "/UpdateExistingUser"})
    public UserResponse updateExistingUser(
            @PathVariable(name = "id", required = false) Integer id,
            @Valid @RequestBody UserUpsertRequest userRequest
    ) {
        return userService.updateExistingUser(id, userRequest);
    }

    @DeleteMapping(path = {"/{id}", "/deleteUser/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExistingUser(@PathVariable Integer id) {
        userService.deleteExistingUser(id);
    }

}
