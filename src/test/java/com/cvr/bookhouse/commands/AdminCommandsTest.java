package com.cvr.bookhouse.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.jline.reader.LineReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.service.AuthService;
import com.cvr.bookhouse.service.BookService;
import com.cvr.bookhouse.service.UserService;

@SuppressWarnings("null")
public class AdminCommandsTest {

  private AdminCommands commands;
  private UserService userService;
  private BookService bookService;
  private AuthService auth;
  private final String ADMIN = "admin";
  private final String BOOK = "book";

  @BeforeEach
  void setUp() {
    AuthenticationManager authenticationManager = authentication -> new UsernamePasswordAuthenticationToken(
        authentication.getPrincipal(), null, List.of());

    userService = new UserService();
    bookService = new BookService(userService);
    auth = new AuthService(authenticationManager, bookService, userService);

    ObjectProvider<LineReader> lineReaderProvider = new ObjectProvider<>() {
      @Override
      public LineReader getObject(Object... args) {
        return null;
      }

      @Override
      public LineReader getObject() {
        return null;
      }

      @Override
      public LineReader getIfAvailable() {
        return null;
      }

      @Override
      public LineReader getIfUnique() {
        return null;
      }

      @Override
      public Stream<LineReader> stream() {
        return Stream.empty();
      }

      @Override
      public Stream<LineReader> orderedStream() {
        return Stream.empty();
      }
    };
    ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    messages.setUseCodeAsDefaultMessage(true);

    MessageFormatter formatter = new MessageFormatter(lineReaderProvider, messages);
    commands = new AdminCommands(bookService, formatter);
  }

  @AfterEach
  void tearDown() {
    if (!Global.userId().isEmpty()) {
      auth.logout();
    }
  }

  @Test
  void testAddBook() {
    auth.login(ADMIN);
    String output = commands.addBook(BOOK, 1);
    assertTrue(output.toLowerCase().contains("book.added"));
  }
}
