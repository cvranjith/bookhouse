package com.cvr.bookhouse.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Book;
import com.cvr.bookhouse.model.Loan;
import com.cvr.bookhouse.model.Session;

@Service
public class BookService {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Loan> loans = new HashMap<>();

    private final Session session;
    //private final LoanService loanService;

    /*public BookService(Session session, UserService users) {
        this.session = session;
    }*/

    public BookService(Session session){//},LoanService loanService) {
        this.session = session;
        //this.loanService =loanService;
    }
    Result isUserAdmin(){
        if ("admin".equals(session.getUserId())){
            return Result.ok("User Is Admin");
        } else {
            return Result.error("User is Not Admin");
        }
    }
    public Book getBook(String bookId){
        return books.get(bookId);
    }
    public Result addBook(String bookId, Integer copies) {
        Result adminCheck=isUserAdmin();
        if (!adminCheck.ok()) return adminCheck;

        if (copies == null || copies <= 0) {
            return Result.error("Copies must be a positive whole number greater than 0.");
        }
        Book b = books.computeIfAbsent(bookId, id -> new Book(id, 0));
        b.setCopies(b.getCopies() + copies);
        return Result.ok("Added "+ copies + " copy(ies) of Book \""+ bookId+ "\". \n"+
            "Total copy(ies) available for this book now is "+b.getCopies());
    }
    public Result list(String bookId) {
        //Result adminCheck = isUserAdmin();
        //if (!adminCheck.ok()) return adminCheck;

        List<Book> items;
        if (bookId == null || bookId.isBlank()) {
            items = books.values().stream()
                    .sorted(Comparator.comparing(Book::getBookId))
                    .collect(Collectors.toList());
        } else if (bookId.contains("*")) {
            String regex = bookId.replace("*", ".*");
            items = books.values().stream()
                    .filter(b -> b.getBookId().matches(regex))
                    .sorted(Comparator.comparing(Book::getBookId))
                    .toList();
        } else {
            Book b = getBook(bookId);
            if (b == null) return Result.error("Book not found: " + bookId);
            items = List.of(b);
        }

        return Result.ok(renderBooksTable(items));
    }


private String renderBooksTable(List<Book> items) {
    if (items.isEmpty()) return "(no books)";

    StringBuilder sb = MessageFormatter.formatLine
        ("sl","Book","Total Copies","Available Copies"," ")
        .append(MessageFormatter.formatLine("-","-","-","-","-"));
    int i = 1;
    for (Book b : items) {
        int borrowedCount=borrowedCount(b.getBookId());
        sb.append( MessageFormatter.formatLine(String.valueOf(i++), b.getBookId(), String.valueOf(b.getCopies()), String.valueOf( b.getCopies()-borrowedCount) ," "));
    }

    return sb.toString();
}

    public int borrowedCount(String bookId) {
        long cnt = loans.values().stream()
                .filter(l -> Objects.equals(l.getBookId(), bookId))
                .count();
        return (int) cnt;
    }
    public Result borrowBook(String bookId) {
        Book book=getBook(bookId);
        if (book==null){
            return Result.error("Invalid BookId "+bookId);
        }
        if (book.getCopies()<= borrowedCount(bookId)){
            return Result.error("Book is not available. Please waitlist if you want.");
        }
        String loanId = UUID.randomUUID().toString();
        Instant borrowedDate=Instant.now();
        loans.put(loanId, new Loan(loanId, bookId, session.getUserId(), borrowedDate));

        return Result.ok("Successuflly borrowed the book \""+bookId+"\" at "+MessageFormatter.formatDateTime(borrowedDate)+".\n"
        + " Loan Reference "+loanId);
    }
    public Result returnBook(String bookId, String loanId) {
         if ((loanId == null || loanId.isBlank()) && (bookId == null || bookId.isBlank())) {
            return Result.error("Please provide <loanId> or a <bookId>.");
        }

        Loan loan;
        //Loan loanByBookId;
        if (loanId != null && !loanId.isBlank()) {
            loan = loans.get(loanId);
            if (loan==null) {
                return Result
                .error("Invalid loanId: " + loanId);
            }
            if (!session.getUserId().equals(loan.getUserId())){
                return Result.error("Book is not boorrowed by user \""+session.getUserId()+"\"");
            }
            if (bookId != null && !bookId.isBlank()) {
                if (!bookId.equals(loan.getBookId())) {
                    return Result.error("Inconsistent loanID and booId provided");
                }
            } else {
                bookId=loan.getBookId();
            }
        }
        if (bookId != null && !bookId.isBlank()){
            List<Loan> matches = loans.values().stream()
                .filter(l -> Objects.equals(l.getUserId(), session.getUserId()))
                .filter(l -> Objects.equals(l.getBookId(), bookId))
                .collect(Collectors.toList());
            if (matches.size() > 1) {
                return Result.error("Multiple active loans found for book \"" + bookId +
                        "\". Please specify --loanId");
            }
            loan=matches.get(0);
            if (loanId != null && !loanId.isBlank()) {
                if (!loanId.equals(loan.getId())) {
                return Result.error("Inconsistent loanID and booId provided");
                }
            } else {
                loanId=loan.getId();
            }
        }
        loans.remove(loanId);
        return Result.ok("Successfully Returned Book '"+bookId+"' (loanId '"+loanId+"'");
    }
}