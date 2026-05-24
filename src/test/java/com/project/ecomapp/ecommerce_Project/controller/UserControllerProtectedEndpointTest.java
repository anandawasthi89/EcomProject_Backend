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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class UserControllerProtectedEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JWTUtils jwtUtils;

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getAllUsersReturnsUsersForAdmin() throws Exception {
        when(customUserDetailsService.getAllUsers()).thenReturn(List.of(
                userResponse(1, "Alice", "alice@example.com", UserRole.CUSTOMER),
                userResponse(2, "Bob", "bob@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED)
        ));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$[1].email").value("bob@example.com"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void getAllUsersReturnsForbiddenForManagerRole() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "CUSTOMER")
    void getAllUsersReturnsForbiddenForCustomerRole() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCurrentUserReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void getCurrentUserReturnsAuthenticatedUser() throws Exception {
        when(customUserDetailsService.getUserByEmail("alice@example.com"))
                .thenReturn(userResponse(1, "Alice", "alice@example.com", UserRole.CUSTOMER));

        mockMvc.perform(get("/api/users/me").with(user("alice@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void getUserByIdReturnsManagedUserForManager() throws Exception {
        when(customUserDetailsService.getUserById(eq(5), any()))
                .thenReturn(userResponse(5, "Managed User", "managed@example.com", UserRole.CUSTOMER));

        mockMvc.perform(get("/api/users/5").with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void updateExistingUserReturnsUpdatedUser() throws Exception {
        UserResponse updatedUser = userResponse(5, "Alice Updated", "alice.updated@example.com", UserRole.CUSTOMER);
        when(customUserDetailsService.updateExistingUser(eq(5), any(), any()))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/5")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpsertUserPayload(5, "Alice Updated", "alice.updated@example.com", "newpassword123")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.email").value("alice.updated@example.com"));

        verify(customUserDetailsService).updateExistingUser(eq(5), any(), any());
    }

    @Test
    void updateExistingUserReturnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(put("/api/users/5")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpsertUserPayload(5, "", "bad-email", "short")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email must be valid"))
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 8 characters"));
    }

    @Test
    void updateExistingUserReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        when(customUserDetailsService.updateExistingUser(eq(99), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(put("/api/users/99")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpsertUserPayload(99, "Missing User", "missing@example.com", "password123")
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void updateCurrentUserAllowsSelfServiceUpdate() throws Exception {
        when(customUserDetailsService.updateCurrentUser(eq("alice@example.com"), any()))
                .thenReturn(userResponse(1, "Alice Self", "alice@example.com", UserRole.CUSTOMER));

        mockMvc.perform(put("/api/users/me")
                        .with(user("alice@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SelfUpdatePayload("Alice Self", "newpassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Self"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void deleteExistingUserReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/5").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(customUserDetailsService).deleteExistingUser(eq(5), any());
    }

    @Test
    void deleteExistingUserAllowsManager() throws Exception {
        mockMvc.perform(delete("/api/users/5").with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isNoContent());

        verify(customUserDetailsService).deleteExistingUser(eq(5), any());
    }

    @Test
    void deleteExistingUserReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(customUserDetailsService)
                .deleteExistingUser(eq(999), any());

        mockMvc.perform(delete("/api/users/999").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    private record UpsertUserPayload(Integer id, String name, String email, String password) {
    }

    private record SelfUpdatePayload(String name, String password) {
    }

    private static UserResponse userResponse(int id, String name, String email, UserRole role) {
        return new UserResponse(id, name, email, role, false, true);
    }
}
