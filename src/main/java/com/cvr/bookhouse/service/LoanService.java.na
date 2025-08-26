package com.cvr.bookhouse.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Book;
import com.cvr.bookhouse.model.Loan;
import com.cvr.bookhouse.model.Session;

@Service
public class LoanService {
    private final Map<String, Loan> loans = new HashMap<>();
 
    private final Session session;
    private final BookService bookService;

    public LoanService(Session session,BookService bookService) {
        this.session = session;
        this.bookService = bookService;
    }

    public int borrowedCount(String bookId) {
        long cnt = loans.values().stream()
                .filter(l -> Objects.equals(l.getBookId(), bookId))
                .count();
        return (int) cnt;
    }
    public Result borrow(String bookId) {
        Book book=bookService.getBook(bookId);
        if (book==null){
            return Result.error("Invalid BookId "+bookId);
        }
        if (book.getCopies()<= borrowedCount(bookId)){
            return Result.error("Book is not available. Please waitlist if you want.");
        }
        String loanId = UUID.randomUUID().toString();
        Instant borrowedDate=Instant.now();
        loans.put(loanId, new Loan(bookId, session.getUserId(), borrowedDate));

        return Result.ok("Successuflly borrowed the book \""+bookId+"\" at "+MessageFormatter.formatDateTime(borrowedDate)+".\n"
        + " Loan Reference "+loanId);
    }
}