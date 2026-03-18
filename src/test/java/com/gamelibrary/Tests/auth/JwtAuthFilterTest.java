package com.gamelibrary.Tests.auth;

import com.gamelibrary.stats.auth.AppUserDetailsService;
import com.gamelibrary.stats.auth.jwt.JwtAuthFilter;
import com.gamelibrary.stats.auth.jwt.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jakarta.servlet.FilterChain;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

 class JwtAuthFilterTest {

    private JwtService jwtService;
    private AppUserDetailsService userDetailsService;
    private JwtAuthFilter filter;

    @BeforeEach
    void setup() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(AppUserDetailsService.class);
        filter = new JwtAuthFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("No Authorization header -> chain continues, no auth set")
    void no_header() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Invalid token -> chain continues, no auth set")
    void invalid_token() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer XYZ");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.isValid("XYZ")).thenReturn(false);

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService).isValid("XYZ");
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Valid token -> loads user and sets authentication")
    void valid_token_sets_authentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer GOOD");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.isValid("GOOD")).thenReturn(true);
        when(jwtService.extractUsername("GOOD")).thenReturn("alice");
        User user = new User("alice", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("alice", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("Valid token but user missing -> clears context and continues unauthenticated")
    void valid_token_user_missing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer OLD");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.isValid("OLD")).thenReturn(true);
        when(jwtService.extractUsername("OLD")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("gone"));

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
