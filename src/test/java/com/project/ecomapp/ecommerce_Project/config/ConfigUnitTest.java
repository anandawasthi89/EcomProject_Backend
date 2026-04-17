package com.project.ecomapp.ecommerce_Project.config;

import com.project.ecomapp.ecommerce_Project.Bean.CustomUserDetails;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Config.JWTAuthenticationEntryPoint;
import com.project.ecomapp.ecommerce_Project.Config.JWTAuthenticationFilter;
import com.project.ecomapp.ecommerce_Project.Config.JWTUtils;
import com.project.ecomapp.ecommerce_Project.Config.PasswordConfiguration;
import com.project.ecomapp.ecommerce_Project.Config.SecurityConfiguration;
import com.project.ecomapp.ecommerce_Project.Services.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigUnitTest {

    @Test
    void passwordEncoderEncodesAndMatches() {
        PasswordConfiguration configuration = new PasswordConfiguration();
        PasswordEncoder encoder = configuration.passwordEncoder(4);

        String encoded = encoder.encode("password123");

        assertNotNull(encoded);
        assertTrue(encoder.matches("password123", encoded));
    }

    @Test
    void jwtUtilsGeneratesAndValidatesToken() {
        String secret = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());
        JWTUtils jwtUtils = new JWTUtils(secret, 60000);
        User user = new User("Alice", "alice@example.com", "encoded");
        UserDetails userDetails = new CustomUserDetails(user);

        String token = jwtUtils.generateToken(userDetails);

        assertEquals("alice@example.com", jwtUtils.extractUsername(token));
        assertTrue(jwtUtils.validateToken(token, userDetails));
        assertFalse(jwtUtils.extractExpiration(token).before(new java.util.Date()));
        assertFalse(jwtUtils.validateToken(token, new CustomUserDetails(new User("Other", "other@example.com", "encoded"))));
    }

    @Test
    void authenticationEntryPointWritesUnauthorizedJson() throws IOException {
        JWTAuthenticationEntryPoint entryPoint = new JWTAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, mock(org.springframework.security.core.AuthenticationException.class));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"message\":\"Unauthorized\"}", response.getContentAsString());
    }

    @Test
    void jwtFilterSkipsWhenAuthorizationHeaderMissing() throws ServletException, IOException {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(userDetailsService, jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        SecurityContextHolder.clearContext();
        filter.doFilter(request, response, filterChain);

        verify(jwtUtils, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void jwtFilterAuthenticatesWhenTokenIsValid() throws ServletException, IOException {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(userDetailsService, jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        User user = new User("Alice", "alice@example.com", "encoded");
        UserDetails userDetails = new CustomUserDetails(user);

        when(jwtUtils.extractUsername("valid-token")).thenReturn("alice@example.com");
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(userDetails);
        when(jwtUtils.validateToken("valid-token", userDetails)).thenReturn(true);

        SecurityContextHolder.clearContext();
        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtFilterIgnoresInvalidToken() throws ServletException, IOException {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(userDetailsService, jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(jwtUtils.extractUsername("bad-token")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

        SecurityContextHolder.clearContext();
        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void jwtFilterIgnoresExpiredToken() throws ServletException, IOException {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(userDetailsService, jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(jwtUtils.extractUsername("expired-token")).thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "expired"));

        SecurityContextHolder.clearContext();
        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void jwtFilterLeavesExistingAuthenticationUntouched() throws ServletException, IOException {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        JWTAuthenticationFilter filter = new JWTAuthenticationFilter(userDetailsService, jwtUtils);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("existing", null));
        when(jwtUtils.extractUsername("valid-token")).thenReturn("alice@example.com");

        filter.doFilter(request, response, filterChain);

        assertEquals("existing", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(userDetailsService, never()).loadUserByUsername(any());
        SecurityContextHolder.clearContext();
    }

    @Test
    void securityConfigurationBuildsProviderAndAuthenticationManager() throws Exception {
        JWTAuthenticationEntryPoint entryPoint = mock(JWTAuthenticationEntryPoint.class);
        JWTAuthenticationFilter filter = mock(JWTAuthenticationFilter.class);
        SecurityConfiguration configuration = new SecurityConfiguration(entryPoint, filter);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

        DaoAuthenticationProvider provider = configuration.authenticationProvider(userDetailsService, passwordEncoder);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        assertSame(authenticationManager, configuration.authenticationManager(authenticationConfiguration));
        assertNotNull(provider);

        ArgumentCaptor<org.springframework.security.authentication.UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class);
        when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(
                new CustomUserDetails(new User("Alice", "alice@example.com", "encoded"))
        );
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);

        provider.authenticate(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "alice@example.com", "password123"
        ));

        verify(userDetailsService).loadUserByUsername("alice@example.com");
        verify(passwordEncoder).matches("password123", "encoded");
        assertNotNull(captor);
    }

}
