package com.cvr.bookhouse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.core.Msg;
import com.cvr.bookhouse.core.Result;

public class AuthServiceTest {

  private AuthService auth;

  private final String ADMIN = "admin";
  private final String ADMIN2 = "admin2";
  private final String USER1 = "user1";
  private final String USER2 = "user2";

  @BeforeEach
  void setup() {
    AuthenticationManager authenticationManager = authentication -> new UsernamePasswordAuthenticationToken(
        authentication.getPrincipal(), null, List.of());
    UserService userService = new UserService();
    BookService bookService = new BookService(userService);
    auth = new AuthService(authenticationManager, bookService, userService);
  }

  @AfterEach
  void tearDown() {
    if (!Global.userId().isEmpty()) {
      auth.logout();
    }
  }

  private static List<String> codes(Result r) {
    return r.messages().stream().map(Msg::code).toList();
  }

  @Test
  void testFirstLogin() {
    var r = auth.login(USER1);
    assertTrue(r.ok());
    assertEquals(USER1, Global.userId());
    var codes = codes(r);
    assertTrue(codes.contains("login.success"));
    assertTrue(codes.contains("login.first"));
  }

  @Test
  void testReLogin() {

    assertTrue(auth.login(USER1).ok());

    Result r = auth.login(USER1);

    assertFalse(r.ok());
    assertEquals(USER1, Global.userId());

    var codes = codes(r);
    assertTrue(codes.contains("already.loggedin"));

  }

  @Test
  void testAdminLogin() {
    Result r = auth.login(ADMIN);
    assertTrue(r.ok());
    var codes = codes(r);
    assertTrue(codes.contains("admin.login"));

    r = auth.login(ADMIN2);
    assertTrue(r.ok());
    codes = codes(r);
    assertTrue(codes.contains("admin.login"));

  }

  @Test
  void testReLoginNewUser() {
    assertTrue(auth.login(USER1).ok());

    var r = auth.login(USER2);

    assertTrue(r.ok());
    assertEquals(USER2, Global.userId());
    assertTrue(codes(r).contains("login.success"));
  }

  @Test
  void testReLoginSameUser() {
    assertTrue(auth.login(USER1).ok());
    assertTrue(auth.login(USER2).ok());

    var r = auth.login(USER1);

    assertTrue(r.ok());

    var codes = codes(r);
    assertTrue(codes.contains("login.success"));
    assertTrue(codes.contains("login.last"));
  }

  @Test
  void testLogout() {
    auth.login(USER1);
    assertEquals(USER1, Global.userId());

    var r = auth.logout();

    assertTrue(r.ok());
    assertEquals("", Global.userId());

    var codes = codes(r);
    assertTrue(codes.contains("logged.out"));
    // no size or position assertions
  }

}
