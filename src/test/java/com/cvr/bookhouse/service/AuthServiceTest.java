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
  @BeforeEach
  void setup() {
    AuthenticationManager authenticationManager = authentication ->
        new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), null, List.of());
    UserService userService = new UserService();       // uses your in-memory map/array
    BookService bookService = new BookService(userService);
    auth = new AuthService(authenticationManager, bookService, userService);
  }

    @AfterEach
    void tearDown() {
    if (!Global.userId().isEmpty()) {
        var r = auth.logout();
    }
    }

  private static List<String> codes(Result r) {
    return r.messages().stream().map(Msg::code).toList();
  }


  @Test
void testFirstLogin() {
    var r = auth.login("cv");
    assertTrue(r.ok());
    assertEquals("cv", Global.userId());
    var codes = codes(r);
    assertTrue(codes.contains("login.success"));
    assertTrue(codes.contains("login.first"));
}

@Test
  void testReLogin() {

    assertTrue(auth.login("cv").ok());
    
    var r = auth.login("cv");

    assertFalse(r.ok());
    assertEquals("cv", Global.userId());

    var codes = codes(r);
    assertTrue(codes.contains("already.loggedin"));
    
  }

  @Test
  void testReLoginNewUser() {
    assertTrue(auth.login("cv").ok());

    var r = auth.login("cv2");

    assertTrue(r.ok());
    assertEquals("cv2", Global.userId());
    assertTrue(codes(r).contains("login.success"));
  }

  
  @Test
  void testReLoginSameUser() {
    assertTrue(auth.login("cv").ok());
    assertTrue(auth.login("cv2").ok());

    var r = auth.login("cv");

    assertTrue(r.ok());

    var codes = codes(r);
    assertTrue(codes.contains("login.success"));
    assertTrue(codes.contains("login.last"));
  }

  @Test
  void testLogout() {
    auth.login("cv");
    assertEquals("cv", Global.userId());

    var r = auth.logout();

    assertTrue(r.ok());
    assertEquals("", Global.userId());

    var codes = codes(r);
    assertTrue(codes.contains("logged.out"));
    // no size or position assertions
  }

}
