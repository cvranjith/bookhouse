package com.cvr.bookhouse.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.core.Msg;
import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Book;
import com.cvr.bookhouse.model.Loan;
//import com.cvr.bookhouse.model.Session;

@Service
public class BookService {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Loan> loans = new HashMap<>();
    private int loanReferenceNumber =0;
    //private final Session session;
    //private final LoanService loanService;

    /*public BookService(Session session, UserService users) {
        this.session = session;
    }*/

    public BookService(){}
        //Session session){//},LoanService loanService) {
        //this.session = session;
        //this.loanService =loanService;
    //}

    public Book getBook(String bookId){
        return books.get(bookId);
    }
    public Result addBook(String bookId, Integer copies) {
        if (copies == null || copies <= 0) {
            return Result.failure().add("invalid.copies");
        }
        Book b = books.computeIfAbsent(bookId, id -> new Book(id, 0));
        b.setCopies(b.getCopies() + copies);
        return Result.success()
            .add("book.added",copies,bookId)
            .add("available.copies",b.getCopies());
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
            if (b == null) return Result.failure().add("invalid.book",bookId);
            items = List.of(b);
        }

        return Result.success() // ok(renderBooksTable(items));
            .add("table",renderBooksTable(items));
    }

private int availableCopies(Book book) {
    return book.getCopies()- (borrowedCount(book.getBookId()));
}
private String renderBooksTable(List<Book> items) {
    if (items.isEmpty()) return "(no books)";

    StringBuilder sb = MessageFormatter.formatLine
        ("sl","Book","Total Copies","Available Copies"," ")
        .append(MessageFormatter.formatLine("-","-","-","-","-"));
    int i = 1;
    for (Book b : items) {
        int availableCopies=availableCopies(b);
        sb.append( MessageFormatter.formatLine(String.valueOf(i++), b.getBookId(), String.valueOf(b.getCopies()), String.valueOf( availableCopies) ," "));
    }

    return sb.toString();
}

    public int borrowedCount(String bookId) {
        long cnt = loans.values().stream()
                .filter(l -> Objects.equals(l.getBookId(), bookId))
                .count();
        return (int) cnt;
    }
    public String nextLoanId() {
        loanReferenceNumber++;
        return String.format("%03d", loanReferenceNumber);
    }
    public Result borrowBook(String bookId) {
        Book book=getBook(bookId);
        if (book==null){
            return Result.failure().add("invalid.book",bookId); //error("Invalid BookId "+bookId);
        }
        if (book.getCopies()<= borrowedCount(bookId)){
            return Result.failure().add("book.unavailable",bookId);//("Book is not available. Please waitlist if you want.");
        }

        List<Msg> msgs = new ArrayList<>();
    

        if (book.getWaitlist().contains(Global.userId())) {
            book.cancelWaitlist(Global.userId());
            msgs.add(new Msg("waitlist.removed",bookId));
        }

        String loanId = nextLoanId();//UUID.randomUUID().toString();
        Instant borrowedDate=Instant.now();
        loans.put(loanId, new Loan(loanId, bookId, Global.userId(), borrowedDate));

        if (book.getWaitlist().contains(Global.userId())) {
            book.cancelWaitlist(Global.userId());
        }
        msgs.add(new Msg("borrow.success",bookId,borrowedDate));
        msgs.add(new Msg("loan.reference",loanId));
        return Result.success(msgs);
        //return Result.ok("Successuflly borrowed the book \""+bookId+"\" at "+MessageFormatter.formatDateTime(borrowedDate)+".\n"
        //+ " Loan Reference "+loanId);
    }

    public Result cancelWaitlist(String bookId){
        Book book=getBook(bookId);
        if (book==null){
            return Result.failure().add("invalid.book",bookId); //error("Invalid BookId "+bookId);
        }
        if (!book.getWaitlist().contains(Global.userId())) {
            return Result.failure().add("not.waitlisted",bookId);
        } else {
            book.cancelWaitlist(Global.userId());
            return Result.success().add("waitlist.removed",bookId);
        }
    }
    public Result returnBook(String bookId, String loanId) {
         if ((loanId == null || loanId.isBlank()) && (bookId == null || bookId.isBlank())) {
            return Result.failure() //error("Please provide <loanId> or a <bookId>.");
                .add("insufficient.input");
        }

        Loan loan;
        //String bookIdCopy=bookId;
        //Loan loanByBookId;
        if (loanId != null && !loanId.isBlank()) {
            loan = loans.get(loanId);
            if (loan==null) {
                return Result
                //.error("Invalid loanId: " + loanId);
                .failure()
                .add("invalid.loanid",loanId);
            }
            if (bookId != null && !bookId.isBlank()) {
                if (!bookId.equals(loan.getBookId())) {
                    return Result.failure() //error("Inconsistent loanID and booId provided");
                        .add("inconsistent.input");
                }
            }
        }
        if (bookId != null && !bookId.isBlank()){
            List<Loan> matches = loans.values().stream()
                .filter(l -> Objects.equals(l.getUserId(), Global.userId()))
                .filter(l -> Objects.equals(l.getBookId(), bookId))
                .collect(Collectors.toList());
            if (matches.size() > 1) {
                return Result.
                        failure().add("multiple.loans",bookId);
            } else if (matches.size()<=0){
                return Result.
                        failure().add("no.loan",bookId);
            }
            loan=matches.get(0);
            if (loanId != null && !loanId.isBlank()) {
                if (!loanId.equals(loan.getId())) {
                    return Result.failure() //error("Inconsistent loanID and booId provided");
                        .add("inconsistent.input");
                }
            } else {
                loanId=loan.getId();
            }
        }
        loan = loans.get(loanId);
        if (!Global.userId().equals(loan.getUserId())){
            return Result.failure()
            .add("not.borrowed",Global.userId());
        }
        String bookIdCopy=loan.getBookId();
        loans.remove(loanId);
        return Result.success()
            .add("return.success",bookIdCopy,loanId);
    }

    public Result joinWaitlist(String bookId) {
        Book book = getBook(bookId);
        if (book==null){
            return Result.failure()
                .add("invalid.book", bookId);            
        }
        if (book.getWaitlist().contains(Global.userId())) {
            return Result.failure()
                .add("already.waitlisted", bookId);
        }

        int position=book.getWaitlist().size()+1;
        boolean added = book.addToWaitlist(Global.userId());
        if (added) {
            List<Msg> msgs = new ArrayList<>();
            msgs.add(new Msg("waitlist.success",bookId,position));
            int availableCopies=availableCopies(book);
            if (availableCopies>0){
                msgs.add(new Msg("book.available",bookId,position));
            }
            return Result.success(msgs);

        } else {
            return Result.success()
                .add("waitlist.error",bookId);
        }
    }

    private boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(Global.userId());
    }

    private String resolveUserFilter(String requestedUserId, boolean isAdmin) {
        if (isAdmin) {
            // admin w/ null or blank -> wildcard all users
            return (requestedUserId == null || requestedUserId.isBlank()) ? "*" : requestedUserId;
        }
        // non-admins are restricted to themselves
        return Global.userId();
    }
    private static String nvl(String s) { return s == null ? "" : s; }
    private boolean wildcardMatch(String value, String pattern) {
        if (pattern == null || pattern.isEmpty()) return Objects.equals(value, "");
        if ("*".equals(pattern)) return true;

        // escape regex specials then turn '*' into '.*'
        String regex = pattern.replace("*", ".*");
        return value != null && value.matches(regex);
    }

        private String renderLoansTable(List<Loan> items) {
        if (items.isEmpty()) return "(no loans)";

        List<String> headers = List.of("sl", "Loan Id", "User", "Book Id", "Borrowed On");
        return renderTable(
            headers,
            items,
            l -> List.of(
                /* sl */ "", // we'll fill serials in renderTable
                nvl(l.getId()),
                nvl(l.getUserId()),
                nvl(l.getBookId()),
                l.getBorrowedDate() == null
                    ? ""
                    : DateTimeFormatter.ISO_INSTANT.format(l.getBorrowedDate())
            )
        );
    }

    private <T> String renderTable(List<String> headers, List<T> items, Function<T, List<String>> rowMap) {
        StringBuilder sb = new StringBuilder();

        // header
        sb.append(MessageFormatter.formatLine(headers.toArray(new String[0])));
        // separator (same number of columns)
        String[] dashes = new String[headers.size()];
        Arrays.fill(dashes, "-");
        sb.append(MessageFormatter.formatLine(dashes));

        // rows (auto serial in first column if header is "sl" ignoring case)
        boolean hasSerial = !headers.isEmpty() && "sl".equalsIgnoreCase(headers.get(0));
        int i = 1;
        for (T item : items) {
            List<String> cols = new ArrayList<>(rowMap.apply(item));
            if (hasSerial) {
                if (cols.isEmpty()) cols.add(0, String.valueOf(i++));
                else cols.set(0, String.valueOf(i++));
            }
            sb.append(MessageFormatter.formatLine(cols.toArray(new String[0])));
        }

        return sb.toString();
    }



    public Result status(String userId){

        final boolean isAdmin = isAdmin();
        final String effectiveUserFilter = resolveUserFilter(userId, isAdmin);

        // stream -> filter -> sort -> list
        List<Loan> filtered = loans.values().stream()
            .filter(l -> wildcardMatch(l.getUserId(), effectiveUserFilter))
            .sorted(Comparator
                .comparing(Loan::getUserId, Comparator.nullsFirst(String::compareTo))
                .thenComparing(Loan::getId, Comparator.nullsFirst(String::compareTo)))
            .collect(Collectors.toList());

        String table = renderLoansTable(filtered);

        return Result.success()
            .add("table", table);
    }

}