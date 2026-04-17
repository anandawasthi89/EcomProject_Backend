package com.project.ecomapp.ecommerce_Project.bean;

import com.project.ecomapp.ecommerce_Project.Bean.CreateUserRequest;
import com.project.ecomapp.ecommerce_Project.Bean.CustomUserDetails;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserJWTRequest;
import com.project.ecomapp.ecommerce_Project.Bean.UserJWTResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserUpsertRequest;
import com.project.ecomapp.ecommerce_Project.EcommerceProjectApplication;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertEquals(11, user.getId());
        assertEquals("Alice", user.getName());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertTrue(user.toString().contains("Alice"));
        assertTrue(user.toString().contains("alice@example.com"));
    }

    @Test
    void userConstructorAndResponseFactoryWork() {
        User user = new User("Bob", "bob@example.com", "encoded");
        user.setId(12);

        UserResponse response = UserResponse.from(user);

        assertEquals(12, response.id());
        assertEquals("Bob", response.name());
        assertEquals("bob@example.com", response.email());
    }

    @Test
    void customUserDetailsExposeUserFields() {
        User user = new User("Cara", "cara@example.com", "encoded");
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Collection<?> authorities = userDetails.getAuthorities();

        assertEquals("encoded", userDetails.getPassword());
        assertEquals("cara@example.com", userDetails.getUsername());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
        assertEquals(1, authorities.size());
        assertTrue(authorities.iterator().next().toString().contains("ROLE_USER"));
        assertSame(user, userDetails.getUser());
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
