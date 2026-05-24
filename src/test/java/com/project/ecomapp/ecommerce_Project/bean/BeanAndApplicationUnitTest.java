package com.project.ecomapp.ecommerce_Project.bean;

import com.project.ecomapp.ecommerce_Project.Bean.Book;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistory;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistoryResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessStatus;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessType;
import com.project.ecomapp.ecommerce_Project.Bean.BookCatalogResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookFavorite;
import com.project.ecomapp.ecommerce_Project.Bean.BookLike;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservation;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservationResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservationStatus;
import com.project.ecomapp.ecommerce_Project.Bean.CreateUserRequest;
import com.project.ecomapp.ecommerce_Project.Bean.CustomUserDetails;
import com.project.ecomapp.ecommerce_Project.Bean.IdentityResponse;
import com.project.ecomapp.ecommerce_Project.Bean.ManagedUserCreateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.RoleOptionResponse;
import com.project.ecomapp.ecommerce_Project.Bean.RoleAssignmentRequest;
import com.project.ecomapp.ecommerce_Project.Bean.SelfUserUpdateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserJWTRequest;
import com.project.ecomapp.ecommerce_Project.Bean.UserJWTResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserUpsertRequest;
import com.project.ecomapp.ecommerce_Project.EcommerceProjectApplication;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanAndApplicationUnitTest {

    @Test
    void userBeanGettersSettersAndToStringWork() {
        User user = new User();
        user.setId(11);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("secret");
        user.setRole(UserRole.ADMIN);
        user.setFlagged(true);
        user.setActive(false);
        user.setPhoneNumber("1234567890");
        user.setAddressLine1("Line 1");
        user.setCity("Noida");
        user.setState("UP");
        user.setPostalCode("201301");

        assertEquals(11, user.getId());
        assertEquals("Alice", user.getName());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertTrue(user.isFlagged());
        assertFalse(user.isActive());
        assertEquals("1234567890", user.getPhoneNumber());
        assertEquals("Line 1", user.getAddressLine1());
        assertEquals("Noida", user.getCity());
        assertEquals("UP", user.getState());
        assertEquals("201301", user.getPostalCode());
        assertTrue(user.toString().contains("Alice"));
        assertTrue(user.toString().contains("alice@example.com"));
        assertTrue(user.toString().contains("ADMIN"));
        assertTrue(user.toString().contains("flagged=true"));
        assertTrue(user.toString().contains("active=false"));
    }

    @Test
    void userConstructorAndResponseFactoryWork() {
        User user = new User("Bob", "bob@example.com", "encoded");
        user.setId(12);

        UserResponse response = UserResponse.from(user);

        assertEquals(12, response.id());
        assertEquals("Bob", response.name());
        assertEquals("bob@example.com", response.email());
        assertEquals(UserRole.CUSTOMER, response.role());
        assertTrue(!user.isFlagged());
        assertFalse(response.flagged());
        assertTrue(response.active());
    }

    @Test
    void customUserDetailsExposeUserFields() {
        User user = new User("Cara", "cara@example.com", "encoded", UserRole.MANAGER);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Collection<?> authorities = userDetails.getAuthorities();

        assertEquals("encoded", userDetails.getPassword());
        assertEquals("cara@example.com", userDetails.getUsername());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
        assertEquals(5, authorities.size());
        assertTrue(authorities.stream().anyMatch(authority -> authority.toString().contains("ROLE_MANAGER")));
        assertTrue(authorities.stream().anyMatch(authority -> authority.toString().contains("users:read")));
        assertSame(user, userDetails.getUser());

        user.setActive(false);
        assertFalse(new CustomUserDetails(user).isEnabled());
    }

    @Test
    void jwtRequestResponseAndRecordsBehaveAsExpected() {
        UserJWTRequest jwtRequest = new UserJWTRequest();
        jwtRequest.setEmail("dave@example.com");
        jwtRequest.setPassword("password123");

        UserJWTRequest jwtRequestWithConstructor = new UserJWTRequest("erin@example.com", "password456");
        UserJWTResponse defaultJwtResponse = new UserJWTResponse();
        UserJWTResponse jwtResponse = new UserJWTResponse("token-1");
        jwtResponse.setToken("token-2");

        CreateUserRequest createUserRequest = new CreateUserRequest("Dave", "dave@example.com", "password123");
        UserUpsertRequest userUpsertRequest = new UserUpsertRequest(9, "Dave", "dave@example.com", "password123");
        ManagedUserCreateRequest managedUserCreateRequest = new ManagedUserCreateRequest(
                "Eve", "eve@example.com", "password123"
        );
        SelfUserUpdateRequest selfUserUpdateRequest = new SelfUserUpdateRequest("Eve", "password123");
        RoleAssignmentRequest roleAssignmentRequest = new RoleAssignmentRequest(UserRole.ADMIN);

        assertEquals("dave@example.com", jwtRequest.getEmail());
        assertEquals("password123", jwtRequest.getPassword());
        assertTrue(jwtRequest.toString().contains("dave@example.com"));
        assertEquals("erin@example.com", jwtRequestWithConstructor.getEmail());
        assertEquals("password456", jwtRequestWithConstructor.getPassword());
        assertEquals(null, defaultJwtResponse.getToken());
        assertEquals("token-2", jwtResponse.getToken());
        assertTrue(jwtResponse.toString().contains("token-2"));
        assertEquals("Dave", createUserRequest.name());
        assertEquals("dave@example.com", createUserRequest.email());
        assertEquals("password123", createUserRequest.password());
        assertEquals(9, userUpsertRequest.id());
        assertEquals("Dave", userUpsertRequest.name());
        assertEquals("eve@example.com", managedUserCreateRequest.email());
        assertEquals("Eve", selfUserUpdateRequest.name());
        assertEquals(UserRole.ADMIN, roleAssignmentRequest.role());
    }

    @Test
    void libraryBeanFactoriesMapFields() {
        User user = new User("Reader", "reader@example.com", "encoded", UserRole.CUSTOMER_WITH_READ_ALLOWED);
        user.setId(5);

        Book book = new Book();
        book.setId(7);
        book.setTitle("Clean Architecture");
        book.setAuthor("Robert C. Martin");
        book.setGenre("Technology");
        book.setPublisher("Pearson");
        book.setPublishedDate(LocalDate.of(2024, 1, 1));
        book.setIsbn("isbn-123");
        book.setDescription("description");
        book.setPhysicalStockQuantity(3);
        book.setEbookAvailable(true);
        book.setInStoreReadAvailable(true);
        book.setLanguage("English");
        book.setPageCount(320);
        book.setArchived(true);

        BookCatalogResponse catalogResponse = BookCatalogResponse.from(book, 2, 1, true, false);
        assertEquals("Clean Architecture", catalogResponse.title());
        assertEquals(3, catalogResponse.physicalStockQuantity());
        assertTrue(catalogResponse.ebookAvailable());
        assertTrue(book.isArchived());

        BookAccessHistory history = new BookAccessHistory();
        history.setId(8);
        history.setBook(book);
        history.setUser(user);
        history.setAccessType(BookAccessType.PHYSICAL_TAKE_HOME);
        history.setStatus(BookAccessStatus.ACTIVE);
        history.setStartedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        history.setDueAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        history.setCompletedAt(LocalDateTime.of(2026, 4, 2, 10, 0));

        BookAccessHistoryResponse historyResponse = BookAccessHistoryResponse.from(history);
        assertEquals(8, historyResponse.id());
        assertEquals("reader@example.com", historyResponse.userEmail());
        assertEquals(BookAccessType.PHYSICAL_TAKE_HOME, historyResponse.accessType());

        BookReservation reservation = new BookReservation();
        reservation.setId(9);
        reservation.setBook(book);
        reservation.setUser(user);
        reservation.setStatus(BookReservationStatus.ACTIVE);
        reservation.setReservedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        reservation.setCheckedOutAt(LocalDateTime.of(2026, 4, 1, 11, 0));
        reservation.setCompletedAt(LocalDateTime.of(2026, 4, 10, 12, 0));

        BookReservationResponse reservationResponse = BookReservationResponse.from(reservation);
        assertEquals(9, reservationResponse.id());
        assertEquals(BookReservationStatus.ACTIVE, reservationResponse.status());
        assertNotNull(reservationResponse.checkedOutAt());
        assertNotNull(reservationResponse.completedAt());
    }

    @Test
    void identityRoleAndPreferenceBeansMapCorrectly() {
        User user = new User("Alice", "alice@example.com", "encoded", UserRole.ADMIN);
        user.setId(15);
        user.setFlagged(true);
        user.setActive(false);

        IdentityResponse identityResponse = IdentityResponse.from(user, UserRole.ADMIN.asAuthorities());
        assertEquals(15, identityResponse.id());
        assertTrue(identityResponse.flagged());
        assertFalse(identityResponse.active());
        assertTrue(identityResponse.authorities().contains("ROLE_ADMIN"));

        RoleOptionResponse roleOptionResponse = RoleOptionResponse.from(UserRole.MANAGER);
        assertEquals(UserRole.MANAGER, roleOptionResponse.role());
        assertTrue(roleOptionResponse.authorities().contains("users:read"));

        BookLike bookLike = new BookLike();
        bookLike.setId(1);
        bookLike.setUser(user);
        bookLike.setBook(new Book());
        bookLike.setCreatedAt(LocalDateTime.now());
        assertEquals(1, bookLike.getId());
        assertSame(user, bookLike.getUser());
        assertNotNull(bookLike.getCreatedAt());

        BookFavorite bookFavorite = new BookFavorite();
        bookFavorite.setId(2);
        bookFavorite.setUser(user);
        bookFavorite.setBook(new Book());
        bookFavorite.setCreatedAt(LocalDateTime.now());
        assertEquals(2, bookFavorite.getId());
        assertSame(user, bookFavorite.getUser());
        assertNotNull(bookFavorite.getCreatedAt());
    }

    @Test
    void corsConfigurerRegistersConfiguredOriginsAndMethods() {
        EcommerceProjectApplication application = new EcommerceProjectApplication();
        WebMvcConfigurer configurer = application.corsConfigurer("http://localhost:4200,http://example.com");
        TestCorsRegistry registry = new TestCorsRegistry();

        configurer.addCorsMappings(registry);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users");
        request.addHeader("Origin", "http://example.com");
        request.addHeader("Access-Control-Request-Method", "GET");

        Map<String, org.springframework.web.cors.CorsConfiguration> configurations = registry.configurations();
        assertEquals(1, configurations.size());
        assertTrue(configurations.containsKey("/**"));
        assertNotNull(configurations.get("/**"));
        assertTrue(configurations.get("/**").checkOrigin(request.getHeader("Origin")) != null);
        assertTrue(configurations.get("/**").getAllowedMethods().contains("GET"));
    }

    private static class TestCorsRegistry extends CorsRegistry {
        Map<String, org.springframework.web.cors.CorsConfiguration> configurations() {
            return super.getCorsConfigurations();
        }
    }
}
