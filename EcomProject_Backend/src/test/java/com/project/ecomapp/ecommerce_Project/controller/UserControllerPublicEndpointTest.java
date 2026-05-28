package com.project.ecomapp.ecommerce_Project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Config.JWTAuthenticationEntryPoint;
import com.project.ecomapp.ecommerce_Project.Config.JWTAuthenticationFilter;
import com.project.ecomapp.ecommerce_Project.Config.JWTUtils;
import com.project.ecomapp.ecommerce_Project.Config.PasswordConfiguration;
import com.project.ecomapp.ecommerce_Project.Config.SecurityConfiguration;
import com.project.ecomapp.ecommerce_Project.Controller.ApiExceptionHandler;
import com.project.ecomapp.ecommerce_Project.user.controller.UserController;
import com.project.ecomapp.ecommerce_Project.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfiguration.class,
        PasswordConfiguration.class,
        JWTAuthenticationFilter.class,
        JWTAuthenticationEntryPoint.class
})
class UserControllerPublicEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JWTUtils jwtUtils;

    @Test
    void getAllUsersReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void addNewUserReturnsCreatedUser() throws Exception {
        UserResponse createdUser = userResponse(1, "Alice", "alice@example.com", UserRole.CUSTOMER);
        when(customUserDetailsService.addNewUser("alice@example.com", "password123", "Alice"))
                .thenReturn(createdUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserPayload("Alice", "alice@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(customUserDetailsService).addNewUser("alice@example.com", "password123", "Alice");
    }

    @Test
    void registerUserReturnsCreatedUser() throws Exception {
        UserResponse createdUser = userResponse(2, "Bob", "bob@example.com", UserRole.CUSTOMER);
        when(customUserDetailsService.addNewUser("bob@example.com", "password123", "Bob"))
                .thenReturn(createdUser);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpsertUserPayload(null, "Bob", "bob@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void addNewUserReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserPayload("Alice", "bad-email", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 8 characters"));
    }

    @Test
    void addNewUserReturnsConflictWhenEmailAlreadyExists() throws Exception {
        when(customUserDetailsService.addNewUser(eq("alice@example.com"), eq("password123"), eq("Alice")))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserPayload("Alice", "alice@example.com", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void addUserAliasEndpointAlsoCreatesUser() throws Exception {
        UserResponse createdUser = userResponse(3, "Cara", "cara@example.com", UserRole.CUSTOMER);
        when(customUserDetailsService.addNewUser(any(), any(), any())).thenReturn(createdUser);

        mockMvc.perform(post("/Users/addUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserPayload("Cara", "cara@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.email").value("cara@example.com"));
    }

    private record CreateUserPayload(String name, String email, String password) {
    }

    private record UpsertUserPayload(Integer id, String name, String email, String password) {
    }

    private static UserResponse userResponse(int id, String name, String email, UserRole role) {
        return new UserResponse(id, name, email, role, false, true);
    }
}
