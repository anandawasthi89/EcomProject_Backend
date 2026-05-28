package com.project.ecomapp.ecommerce_Project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecomapp.ecommerce_Project.Bean.BatchManagedUserCreateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistoryResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessStatus;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessType;
import com.project.ecomapp.ecommerce_Project.Bean.IdentityResponse;
import com.project.ecomapp.ecommerce_Project.Bean.RoleOptionResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.Config.JWTAuthenticationEntryPoint;
import com.project.ecomapp.ecommerce_Project.Config.JWTAuthenticationFilter;
import com.project.ecomapp.ecommerce_Project.Config.JWTUtils;
import com.project.ecomapp.ecommerce_Project.Config.PasswordConfiguration;
import com.project.ecomapp.ecommerce_Project.Config.SecurityConfiguration;
import com.project.ecomapp.ecommerce_Project.Controller.ApiExceptionHandler;
import com.project.ecomapp.ecommerce_Project.library.service.LibraryService;
import com.project.ecomapp.ecommerce_Project.user.controller.IamController;
import com.project.ecomapp.ecommerce_Project.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IamController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfiguration.class,
        PasswordConfiguration.class,
        JWTAuthenticationFilter.class,
        JWTAuthenticationEntryPoint.class
})
class IamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private LibraryService libraryService;

    @MockBean
    private JWTUtils jwtUtils;

    @Test
    void getCurrentIdentityReturnsIdentityForAuthenticatedUser() throws Exception {
        when(customUserDetailsService.getIdentityByEmail(eq("admin@example.com"), any()))
                .thenReturn(new IdentityResponse(
                        1,
                        "Admin User",
                        "admin@example.com",
                        UserRole.ADMIN,
                        false,
                        true,
                        List.of("ROLE_ADMIN", "iam:write", "users:read")
                ));

        mockMvc.perform(get("/api/iam/me").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getSupportedRolesAllowsManager() throws Exception {
        when(customUserDetailsService.getRoleOptions()).thenReturn(List.of(
                RoleOptionResponse.from(UserRole.ADMIN),
                RoleOptionResponse.from(UserRole.MANAGER)
        ));

        mockMvc.perform(get("/api/iam/roles").with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void createManagedUserAllowsManager() throws Exception {
        when(customUserDetailsService.createManagedUser(any()))
                .thenReturn(userResponse(10, "Reader", "reader@example.com", UserRole.CUSTOMER, false, true));

        mockMvc.perform(post("/api/iam/users")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ManagedUserPayload(
                                "Reader", "reader@example.com", "password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void createManagedUsersAllowsManagerBatchCreation() throws Exception {
        when(customUserDetailsService.createManagedUsers(any())).thenReturn(List.of(
                userResponse(11, "One", "one@example.com", UserRole.CUSTOMER, false, true),
                userResponse(12, "Two", "two@example.com", UserRole.CUSTOMER, false, true)
        ));

        mockMvc.perform(post("/api/iam/users/batch")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BatchManagedUsersPayload(List.of(
                                new ManagedUserPayload("One", "one@example.com", "password123"),
                                new ManagedUserPayload("Two", "two@example.com", "password123")
                        )))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[1].email").value("two@example.com"));
    }

    @Test
    void grantReadAccessAllowsManager() throws Exception {
        when(customUserDetailsService.upgradeCustomerReadAccess(eq(7), any()))
                .thenReturn(userResponse(7, "Managed User", "managed@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED, false, true));

        mockMvc.perform(patch("/api/iam/users/7/grant-read-access")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER_WITH_READ_ALLOWED"));
    }

    @Test
    void getFlaggedUsersAllowsManager() throws Exception {
        when(customUserDetailsService.getFlaggedUsers(any())).thenReturn(List.of(
                userResponse(7, "Managed User", "managed@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED, true, true)
        ));

        mockMvc.perform(get("/api/iam/users/flagged")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flagged").value(true));
    }

    @Test
    void getUserFlagStatusAllowsManager() throws Exception {
        when(customUserDetailsService.getUserFlagStatus(eq(7), any()))
                .thenReturn(userResponse(7, "Managed User", "managed@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED, true, true));

        mockMvc.perform(get("/api/iam/users/7/flag-status")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true));
    }

    @Test
    void scanOverdueFlagsReturnsUsers() throws Exception {
        when(libraryService.scanAndFlagUsersWithOverduePhysicalLoans()).thenReturn(List.of(
                createUser(7, "Managed User", "managed@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED, true, true)
        ));

        mockMvc.perform(post("/api/iam/users/flags/scan-overdue")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("managed@example.com"));
    }

    @Test
    void deactivateFlaggedUserAllowsManager() throws Exception {
        when(customUserDetailsService.deactivateUser(eq(7), any()))
                .thenReturn(userResponse(7, "Managed User", "managed@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED, false, false));

        mockMvc.perform(post("/api/iam/users/7/flags/resolve/deactivate")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.flagged").value(false));
    }

    @Test
    void forceReturnAllowsManager() throws Exception {
        when(libraryService.forceReturnPhysicalBook(eq(7), eq(9), any()))
                .thenReturn(historyResponse(9, "managed@example.com", BookAccessStatus.RETURNED));

        mockMvc.perform(post("/api/iam/users/7/flags/resolve/force-return")
                        .param("bookId", "9")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void extendDurationAllowsManager() throws Exception {
        when(libraryService.extendPhysicalCheckoutDuration(eq(7), eq(9), eq(20), any()))
                .thenReturn(historyResponse(9, "managed@example.com", BookAccessStatus.ACTIVE));

        mockMvc.perform(post("/api/iam/users/7/flags/resolve/extend-duration")
                        .param("bookId", "9")
                        .param("extraDays", "20")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void customerReadAccessAllowsExplicitReadRole() throws Exception {
        mockMvc.perform(get("/api/iam/access/customer-read")
                        .with(user("reader@example.com").roles("CUSTOMER_WITH_READ_ALLOWED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("customer-read"));
    }

    @Test
    void adminConsoleRejectsManager() throws Exception {
        mockMvc.perform(get("/api/iam/access/admin-console")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    private static UserResponse userResponse(
            int id,
            String name,
            String email,
            UserRole role,
            boolean flagged,
            boolean active
    ) {
        return new UserResponse(id, name, email, role, flagged, active);
    }

    private static com.project.ecomapp.ecommerce_Project.Bean.User createUser(
            int id,
            String name,
            String email,
            UserRole role,
            boolean flagged,
            boolean active
    ) {
        com.project.ecomapp.ecommerce_Project.Bean.User user = new com.project.ecomapp.ecommerce_Project.Bean.User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRole(role);
        user.setFlagged(flagged);
        user.setActive(active);
        return user;
    }

    private static BookAccessHistoryResponse historyResponse(int bookId, String userEmail, BookAccessStatus status) {
        return new BookAccessHistoryResponse(
                15,
                bookId,
                "Clean Architecture",
                7,
                userEmail,
                BookAccessType.PHYSICAL_TAKE_HOME,
                status,
                LocalDateTime.of(2026, 4, 19, 12, 0),
                LocalDateTime.of(2026, 5, 19, 12, 0),
                status == BookAccessStatus.RETURNED ? LocalDateTime.of(2026, 4, 20, 12, 0) : null
        );
    }

    private record ManagedUserPayload(String name, String email, String password) {
    }

    private record BatchManagedUsersPayload(List<ManagedUserPayload> users) {
    }
}
