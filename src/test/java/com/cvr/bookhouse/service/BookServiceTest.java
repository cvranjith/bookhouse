package com.cvr.bookhouse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.core.Msg;
import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Book;

public class BookServiceTest {

private UserService userService;
  private BookService bookService;
  private AuthService auth;

  @BeforeEach
  void setUp() {
    AuthenticationManager authenticationManager = authentication ->
        new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), null, List.of());

    userService  = new UserService();
    bookService  = new BookService(userService);
    auth         = new AuthService(authenticationManager, bookService, userService);
  }

  @AfterEach
  void tearDown() {
    if (!Global.userId().isEmpty()) {
        auth.logout();
    }
  }

  // ---------- helpers ----------
  private static List<String> codes(Result r) {
    return r.messages().stream().map(Msg::code).toList();
  }
  private void loginAs(String userId) {
    var r = auth.login(userId);
    assertTrue(r.ok(), "login failed for user: " + userId);
  }
    private final String ADMIN="admin";
    private final String USER1="user1";
    private final String USER2="user2";
    private final String BOOK0="book0";
    private final String BOOK1="book1";
    private final String BOOK2="book2";



    @Test
    void testAddBook() {
        loginAs(ADMIN);

        //test invalid copy
        Result r0=bookService.addBook(BOOK0,null);
        assertFalse(r0.ok());

        r0=bookService.addBook(BOOK0,0);
        assertFalse(r0.ok());

        r0=bookService.addBook(BOOK0,-1);
        assertFalse(r0.ok());


        //test add 1 copy
        Result r1=bookService.addBook(BOOK1,1);
        assertTrue(r1.ok());
        assertTrue(codes(r1).contains("book.added"));
        Book b=bookService.getBook(BOOK1);
        assertNotNull(b);
        assertEquals(1, b.getCopies());

        //add another 10 copies
        Result r2=bookService.addBook(BOOK1,10);
        assertTrue(r2.ok());
        assertTrue(codes(r2).contains("book.added"));
        b=bookService.getBook(BOOK1);
        assertNotNull(b);
        assertEquals(11, b.getCopies());

        //add a book directly with > 1 copies
        Result r3=bookService.addBook(BOOK2,10);
        assertTrue(r3.ok());
        assertTrue(codes(r3).contains("book.added"));
        Book b2=bookService.getBook(BOOK2);
        assertNotNull(b2);
        assertEquals(10, b2.getCopies());

    }


    @Test
    void testBorrowBook() {


        //setup- Admin add a book
        loginAs(ADMIN);
        Result r1=bookService.addBook(BOOK1,1);
        assertTrue(r1.ok());
        assertTrue(codes(r1).contains("book.added"));
        Book b=bookService.getBook(BOOK1);
        assertNotNull(b);
        assertEquals(1, b.getCopies());
        int borrowedCount=bookService.borrowedCount(BOOK1);
        assertEquals(0, borrowedCount);


        //USER1 Borrows
        loginAs(USER1);
        Result r2=bookService.borrowBook(BOOK1);
        assertTrue(r2.ok());
        assertTrue(codes(r2).contains("borrow.success"));
        assertTrue(codes(r2).contains("loan.reference"));
        int borrowedCountNew=bookService.borrowedCount(BOOK1);
        assertEquals(1, borrowedCountNew);

        //try borrow again
        Result r3=bookService.borrowBook(BOOK1);
        assertFalse(r3.ok());

        //add another copy as admin;
        loginAs(ADMIN);
        Result r4=bookService.addBook(BOOK1,1);
        assertTrue(r4.ok());

        b=bookService.getBook(BOOK1);
        assertNotNull(b);
        assertEquals(2, b.getCopies());
        borrowedCount=bookService.borrowedCount(BOOK1);
        assertEquals(1, borrowedCount);

        //borrow as another user
        loginAs(USER2);
        Result r5=bookService.borrowBook(BOOK1);
        assertTrue(r5.ok());
        borrowedCount=bookService.borrowedCount(BOOK1);
        assertEquals(2, borrowedCount);

    }

    @Test
    void testReturnBook() {

        //setup- Admin add a book
        loginAs(ADMIN);
        Result r1=bookService.addBook(BOOK1,1);
        assertTrue(r1.ok());
        assertTrue(codes(r1).contains("book.added"));
        Book b=bookService.getBook(BOOK1);
        assertNotNull(b);
        assertEquals(1, b.getCopies());
        int borrowedCount=bookService.borrowedCount(BOOK1);
        assertEquals(0, borrowedCount);


        //USER1 Borrows
        loginAs(USER1);
        Result r2=bookService.borrowBook(BOOK1);
        assertTrue(r2.ok());
        assertTrue(codes(r2).contains("borrow.success"));
        assertTrue(codes(r2).contains("loan.reference"));
        int borrowedCountNew=bookService.borrowedCount(BOOK1);
        assertEquals(1, borrowedCountNew);

        //user returns
        Result r3=bookService.returnBook(BOOK1, "");
        assertTrue(r3.ok());
        int borrowedCountAferReturn=bookService.borrowedCount(BOOK1);
        assertEquals(0, borrowedCountAferReturn);

        //user try to return again
        Result r4=bookService.returnBook(BOOK1, "");
        assertFalse(r4.ok());

    }

    @Test
    void testAddToWaitlist() {

        //setup- Admin add a book
        loginAs(ADMIN);
        assertTrue(bookService.addBook(BOOK1,1).ok());

        loginAs(USER1);
        // borrow invalid book should fail
        Result r=bookService.addToWaitlist("invalid_book");
        assertFalse(r.ok());

        // borrow valid book
        r=bookService.addToWaitlist(BOOK1);
        assertTrue(r.ok());
        assertTrue(codes(r).contains("waitlist.success"));
        
        OptionalInt pos=bookService.getWaitlistPosition(BOOK1,USER1);
        //System.out.println("pos ==="+pos);
        assertEquals(1, pos.getAsInt());

        //try to waitlist again
        r=bookService.addToWaitlist(BOOK1);
        assertFalse(r.ok());

        //borrow with another user
        loginAs(USER2);
        r=bookService.addToWaitlist(BOOK1);
        assertTrue(r.ok());
        assertTrue(codes(r).contains("waitlist.success"));
        pos=bookService.getWaitlistPosition(BOOK1,USER2);
        assertEquals(2, pos.getAsInt()); // position should be 2.


    }


    @Test
    void testRemoveFromWaitlist() {

        //setup- Admin add a book
        loginAs(ADMIN);
        assertTrue(bookService.addBook(BOOK1,1).ok());

        loginAs(USER1);
        // borrow valid book
        Result r=bookService.addToWaitlist(BOOK1);
        assertTrue(r.ok());
        
        OptionalInt pos=bookService.getWaitlistPosition(BOOK1,USER1);
        //System.out.println("pos ==="+pos);
        assertEquals(1, pos.getAsInt());

        //try to waitlist again
        r=bookService.removeFromWaitlist(BOOK1);
        assertTrue(r.ok());

        pos=bookService.getWaitlistPosition(BOOK1,USER1);
        //System.out.println("pos ==="+pos);
        assertTrue(pos.isEmpty());

    }

}
