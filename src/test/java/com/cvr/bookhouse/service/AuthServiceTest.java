package com.cvr.bookhouse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cvr.bookhouse.model.Session;

public class AuthServiceTest {

    private Session session;
    private UserService userService;
    private AuthService auth;

    @BeforeEach
    void setup() {
        session = new Session();
        userService = new UserService();
        auth = new AuthService(session, userService);
    }

    @Test
    void testFirstLogin() {
        var r = auth.login("cv");
        assertTrue(r.ok());
        assertEquals("cv", session.getUserId());
        assertTrue(r.msg().contains("Welcome, cv"));
        assertTrue(r.msg().contains("first login"));
    }

    @Test
    void testReLogin() {
        assertTrue(auth.login("cv").ok());
        var r = auth.login("cv");
        assertFalse(r.ok());
        assertTrue(r.msg().contains("already logged in"));
        assertEquals("cv", session.getUserId());
    }

    @Test
    void testReLoginNewUser() {
        assertTrue(auth.login("cv").ok());
        var r = auth.login("cv2");
        assertTrue(r.ok());
        assertTrue(r.msg().contains("Welcome, cv2"));
        assertEquals("cv2", session.getUserId());
    }

    @Test
    void testReLoginSameUser() {
        assertTrue(auth.login("cv").ok());
        assertTrue(auth.login("cv2").ok());
        var r = auth.login("cv");
        assertTrue(r.ok());
        assertTrue(r.msg().contains("Your last login was at"));
    }

    @Test
    void testLogout() {
        auth.login("cv");
        assertEquals("cv", session.getUserId());
        var r = auth.logout();
        assertTrue(r.ok());
        assertEquals("", session.getUserId());
    }

    @Test
    void testInvalidLogout() {
        var r = auth.logout();
        assertFalse(r.ok());
        assertEquals("", session.getUserId());
    }
}
