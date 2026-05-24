package com.project.ecomapp.ecommerce_Project.service;

import com.project.ecomapp.ecommerce_Project.Bean.BatchManagedUserCreateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.ManagedUserCreateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.SelfUserUpdateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserUpsertRequest;
import com.project.ecomapp.ecommerce_Project.user.repository.UserRepDAO;
import com.project.ecomapp.ecommerce_Project.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class CustomUserDetailsServiceUnitTest {

    @Test
    void loadUserByUsernameReturnsUserDetails() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);
        User user = createUser(1, "Alice", "alice@example.com", "encoded");

        when(userRepDAO.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        assertEquals("alice@example.com", service.loadUserByUsername("  Alice@Example.com ").getUsername());
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));

        when(userRepDAO.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));
    }

    @Test
    void getAllUsersMapsRepositoryResults() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));

        when(userRepDAO.findAll()).thenReturn(List.of(
                createUser(1, "Alice", "alice@example.com", "encoded"),
                createUser(2, "Bob", "bob@example.com", "encoded")
        ));

        List<UserResponse> response = service.getAllUsers();

        assertEquals(2, response.size());
        assertEquals("bob@example.com", response.get(1).email());
    }

    @Test
    void getFlaggedUsersFiltersByActorScope() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User customer = createUser(1, "Alice", "alice@example.com", "encoded");
        customer.setFlagged(true);
        User admin = createUser(2, "Admin", "admin@example.com", "encoded", UserRole.ADMIN);
        admin.setFlagged(true);

        when(userRepDAO.findAllByFlaggedTrueOrderByIdAsc()).thenReturn(List.of(customer, admin));

        List<UserResponse> managerView = service.getFlaggedUsers(UserRole.MANAGER.asAuthorities());
        List<UserResponse> adminView = service.getFlaggedUsers(UserRole.ADMIN.asAuthorities());

        assertEquals(1, managerView.size());
        assertEquals("alice@example.com", managerView.get(0).email());
        assertEquals(2, adminView.size());
    }

    @Test
    void getUserByEmailReturnsUserWhenPresent() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User user = createUser(1, "Alice", "alice@example.com", "encoded");

        when(userRepDAO.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        assertEquals("Alice", service.getUserByEmail(" Alice@Example.com ").name());
    }

    @Test
    void getUserByEmailThrowsWhenMissing() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));

        when(userRepDAO.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getUserByEmail("missing@example.com"));

        assertEquals(NOT_FOUND, exception.getStatus());
    }

    @Test
    void findUserDelegatesToRepository() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User user = createUser(3, "Cara", "cara@example.com", "encoded");

        when(userRepDAO.findById(3)).thenReturn(Optional.of(user));

        assertTrue(service.findUser(3).isPresent());
    }

    @Test
    void addNewUserCreatesEncodedUser() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);

        when(userRepDAO.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(8);
            return user;
        }).when(userRepDAO).save(any(User.class));

        UserResponse response = service.addNewUser(" Alice@Example.com ", "password123", " Alice ");

        assertEquals(8, response.id());
        assertEquals("Alice", response.name());
        assertEquals("alice@example.com", response.email());
        assertEquals(UserRole.CUSTOMER, response.role());
    }

    @Test
    void addNewUserFallsBackToEmailWhenNameBlank() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);

        when(userRepDAO.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        UserResponse response = service.addNewUser("alice@example.com", "password123", " ");

        assertEquals("alice@example.com", response.name());
    }

    @Test
    void addNewUserFallsBackToEmailWhenNameNull() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);

        when(userRepDAO.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        UserResponse response = service.addNewUser("alice@example.com", "password123", null);

        assertEquals("alice@example.com", response.name());
    }

    @Test
    void addNewUserThrowsWhenEmailAlreadyExists() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));

        when(userRepDAO.existsByEmail("alice@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.addNewUser("alice@example.com", "password123", "Alice"));

        assertEquals(CONFLICT, exception.getStatus());
    }

    @Test
    void createManagedUserCreatesCustomerAccount() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);

        when(userRepDAO.existsByEmail("reader@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        UserResponse response = service.createManagedUser(
                new ManagedUserCreateRequest("Reader", "reader@example.com", "password123")
        );

        assertEquals(UserRole.CUSTOMER, response.role());
    }

    @Test
    void createManagedUsersCreatesBatchOfCustomers() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);

        when(userRepDAO.existsByEmail("one@example.com")).thenReturn(false);
        when(userRepDAO.existsByEmail("two@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        List<UserResponse> response = service.createManagedUsers(new BatchManagedUserCreateRequest(List.of(
                new ManagedUserCreateRequest("One", "one@example.com", "password123"),
                new ManagedUserCreateRequest("Two", "two@example.com", "password123")
        )));

        assertEquals(2, response.size());
        assertEquals(UserRole.CUSTOMER, response.get(0).role());
        assertEquals("two@example.com", response.get(1).email());
    }

    @Test
    void updateExistingUserUsesPathIdAndSavesChanges() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);
        User user = createUser(5, "Old", "old@example.com", "old");

        when(userRepDAO.findById(5)).thenReturn(Optional.of(user));
        when(userRepDAO.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        UserResponse response = service.updateExistingUser(
                5,
                new UserUpsertRequest(null, " New Name ", " New@Example.com ", "newpassword123"),
                UserRole.MANAGER.asAuthorities()
        );

        assertEquals("New Name", response.name());
        assertEquals("new@example.com", response.email());
        assertEquals("encoded", user.getPassword());
    }

    @Test
    void updateExistingUserFallsBackToRequestId() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);
        User user = createUser(6, "Old", "old@example.com", "old");

        when(userRepDAO.findById(6)).thenReturn(Optional.of(user));
        when(userRepDAO.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        UserResponse response = service.updateExistingUser(
                null,
                new UserUpsertRequest(6, "Name", "new@example.com", "newpassword123"),
                UserRole.ADMIN.asAuthorities()
        );

        assertEquals(6, response.id());
    }

    @Test
    void updateExistingUserThrowsWhenIdMissing() {
        CustomUserDetailsService service = new CustomUserDetailsService(mock(UserRepDAO.class), mock(PasswordEncoder.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateExistingUser(
                        null,
                        new UserUpsertRequest(null, "Name", "user@example.com", "password123"),
                        UserRole.ADMIN.asAuthorities()
                ));

        assertEquals(BAD_REQUEST, exception.getStatus());
    }

    @Test
    void updateExistingUserThrowsWhenUserMissing() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));

        when(userRepDAO.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateExistingUser(
                        99,
                        new UserUpsertRequest(99, "Name", "user@example.com", "password123"),
                        UserRole.ADMIN.asAuthorities()
                ));

        assertEquals(NOT_FOUND, exception.getStatus());
    }

    @Test
    void updateExistingUserThrowsWhenEmailBelongsToAnotherUser() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User current = createUser(5, "Current", "current@example.com", "old");
        User other = createUser(7, "Other", "taken@example.com", "other");

        when(userRepDAO.findById(5)).thenReturn(Optional.of(current));
        when(userRepDAO.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateExistingUser(
                        5,
                        new UserUpsertRequest(5, "Name", "taken@example.com", "password123"),
                        UserRole.ADMIN.asAuthorities()
                ));

        assertEquals(CONFLICT, exception.getStatus());
    }

    @Test
    void updateExistingUserRejectsManagerUpdatingAdmin() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User admin = createUser(2, "Admin", "admin@example.com", "encoded", UserRole.ADMIN);

        when(userRepDAO.findById(2)).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.updateExistingUser(
                        2,
                        new UserUpsertRequest(2, "Admin", "admin@example.com", "password123"),
                        UserRole.MANAGER.asAuthorities()
                ));

        assertEquals(FORBIDDEN, exception.getStatus());
    }

    @Test
    void updateCurrentUserUpdatesOnlyCurrentUserFields() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, passwordEncoder);
        User user = createUser(10, "Alice", "alice@example.com", "old", UserRole.CUSTOMER);

        when(userRepDAO.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        UserResponse response = service.updateCurrentUser("alice@example.com", new SelfUserUpdateRequest("Alice Self", "newpassword123"));

        assertEquals("Alice Self", response.name());
        assertEquals(UserRole.CUSTOMER, response.role());
        assertEquals("encoded", user.getPassword());
    }

    @Test
    void upgradeCustomerReadAccessUpdatesUserRole() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User user = createUser(12, "Bob", "bob@example.com", "encoded", UserRole.CUSTOMER);

        when(userRepDAO.findById(12)).thenReturn(Optional.of(user));
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        UserResponse response = service.upgradeCustomerReadAccess(12, UserRole.MANAGER.asAuthorities());

        assertEquals(UserRole.CUSTOMER_WITH_READ_ALLOWED, response.role());
        assertEquals(UserRole.CUSTOMER_WITH_READ_ALLOWED, user.getRole());
    }

    @Test
    void upgradeCustomerReadAccessRejectsNonCustomerTarget() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User user = createUser(12, "Bob", "bob@example.com", "encoded", UserRole.CUSTOMER_WITH_READ_ALLOWED);

        when(userRepDAO.findById(12)).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.upgradeCustomerReadAccess(12, UserRole.ADMIN.asAuthorities()));

        assertEquals(BAD_REQUEST, exception.getStatus());
    }

    @Test
    void deleteExistingUserDeletesWhenFound() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User user = createUser(4, "Alice", "alice@example.com", "encoded");

        when(userRepDAO.findById(4)).thenReturn(Optional.of(user));
        doNothing().when(userRepDAO).delete(user);

        service.deleteExistingUser(4, UserRole.MANAGER.asAuthorities());

        verify(userRepDAO).delete(user);
    }

    @Test
    void deleteExistingUserThrowsWhenMissing() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));

        when(userRepDAO.findById(4)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.deleteExistingUser(4, UserRole.ADMIN.asAuthorities()));

        assertEquals(NOT_FOUND, exception.getStatus());
        verify(userRepDAO, never()).delete(any());
    }

    @Test
    void deleteExistingUserRejectsManagerDeletingAdmin() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User admin = createUser(4, "Admin", "admin@example.com", "encoded", UserRole.ADMIN);

        when(userRepDAO.findById(4)).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.deleteExistingUser(4, UserRole.MANAGER.asAuthorities()));

        assertEquals(FORBIDDEN, exception.getStatus());
        verify(userRepDAO, never()).delete(any());
    }

    @Test
    void getUserFlagStatusRespectsManagerScope() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User user = createUser(4, "Alice", "alice@example.com", "encoded");
        user.setFlagged(true);

        when(userRepDAO.findById(4)).thenReturn(Optional.of(user));

        UserResponse response = service.getUserFlagStatus(4, UserRole.MANAGER.asAuthorities());

        assertTrue(response.flagged());
    }

    @Test
    void deactivateUserDisablesAndClearsFlag() {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        CustomUserDetailsService service = new CustomUserDetailsService(userRepDAO, mock(PasswordEncoder.class));
        User user = createUser(4, "Alice", "alice@example.com", "encoded");
        user.setFlagged(true);
        user.setActive(true);

        when(userRepDAO.findById(4)).thenReturn(Optional.of(user));
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        UserResponse response = service.deactivateUser(4, UserRole.MANAGER.asAuthorities());

        assertFalse(response.flagged());
        assertFalse(response.active());
        assertFalse(user.isActive());
    }

    private static User createUser(int id, String name, String email, String password) {
        return createUser(id, name, email, password, UserRole.CUSTOMER);
    }

    private static User createUser(int id, String name, String email, String password, UserRole role) {
        User user = new User(name, email, password, role);
        user.setId(id);
        return user;
    }
}
