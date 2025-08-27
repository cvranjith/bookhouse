package com.cvr.bookhouse.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.core.Msg;
import com.cvr.bookhouse.core.PrintUtil;
import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Book;
import com.cvr.bookhouse.model.Loan;
//import com.cvr.bookhouse.model.Session;
import com.cvr.bookhouse.model.Waitlist;

@Service
public class BookService {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Loan> loans = new HashMap<>();

    private final Map<Long, Waitlist> waitlists = new HashMap<>();

    private int loanReferenceNumber =0;
    private long waitlistReferenceNumber = 0;

    private final UserService userService;
    //private final Session session;
    //private final LoanService loanService;

    /*public BookService(Session session, UserService users) {
        this.session = session;
    }*/

    public BookService(UserService userService)
    {
        this.userService=userService;
    }
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

private boolean removeFromWaitlistIfPresent(String bookId, String userId) {
    Long toRemove = waitlists.entrySet().stream()
        .filter(e -> Objects.equals(e.getValue().getBookId(), bookId)
                  && Objects.equals(e.getValue().getUserId(), userId))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);

    if (toRemove == null) return false;
    waitlists.remove(toRemove);
    return true;
}

private int waitlistCount(String bookId) {
    return (int) waitlists.values().stream()
        .filter(w -> Objects.equals(w.getBookId(), bookId))
        .count();
}

public OptionalInt getWaitlistPosition(String bookId, String userId) {
    List<Waitlist> entries = waitlists.values().stream()
        .filter(w -> Objects.equals(w.getBookId(), bookId))
        .sorted(Comparator.comparingLong(Waitlist::getId)) // FIFO by id
        .toList();

    for (int i = 0; i < entries.size(); i++) {
        if (Objects.equals(entries.get(i).getUserId(), userId)) return OptionalInt.of(i + 1);
    }
    return OptionalInt.empty();
}

/** Null means allowed; non-null is the failure Result to return */
private Result checkBorrowGate(String bookId, String userId, Book book) {
    int copies = book.getCopies();
    int borrowed = borrowedCount(bookId);
    int available = copies - borrowed;                // e.g., 1 - 0 = 1

    if (available <= 0) {
        return Result.failure().add("book.unavailable", bookId);
    }

    int queue = waitlistCount(bookId);                // e.g., 1 (admin)
    OptionalInt posOpt = getWaitlistPosition(bookId, userId); // empty for "user"

    if (posOpt.isEmpty()) {
        // user NOT on waitlist → only allowed if available > queue
        if (available <= queue) {
            return Result.failure().add("waitlist.priority", bookId);
        }
        return null; // allowed
    } else {
        int pos = posOpt.getAsInt();
        // user IS on waitlist → allowed if pos <= available
        if (pos > available) {
            return Result.failure().add("waitlist.ahead", pos);
        }
        return null; // allowed
    }
}


    public Result borrowBook(String bookId) {
        Book book=getBook(bookId);
        if (book==null){
            return Result.failure().add("invalid.book",bookId); //error("Invalid BookId "+bookId);
        }
        /*if (book.getCopies()<= borrowedCount(bookId)){
            return Result.failure().add("book.unavailable",bookId);//("Book is not available. Please waitlist if you want.");
        }*/

        Result gate = checkBorrowGate(bookId, Global.userId(), book);
        if (gate != null) return gate; // BLOCK here if queue has priority

        List<Msg> msgs = new ArrayList<>();
    

        /*
        if (book.getWaitlist().contains(Global.userId())) {
            book.cancelWaitlist(Global.userId());
            msgs.add(new Msg("waitlist.removed",bookId));
        }*/
        //TODO

        if (removeFromWaitlistIfPresent(bookId, Global.userId())) {
            msgs.add(new Msg("waitlist.removed", bookId));
        }


        String loanId = nextLoanId();//UUID.randomUUID().toString();
        Instant borrowedDate=Instant.now();
        loans.put(loanId, new Loan(loanId, bookId, Global.userId(), borrowedDate));

        msgs.add(new Msg("borrow.success",bookId,borrowedDate));
        msgs.add(new Msg("loan.reference",loanId));
        return Result.success(msgs);
        //return Result.ok("Successuflly borrowed the book \""+bookId+"\" at "+MessageFormatter.formatDateTime(borrowedDate)+".\n"
        //+ " Loan Reference "+loanId);
    }

    /*public Result cancelWaitlist(String bookId){
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
    }*/
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

    /*
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
*/
/*public int getWaitlistPosition(String bookId, String userId) {
    // collect all waitlist entries for this book, sorted by id (FIFO)
    List<Waitlist> entries = waitlists.values().stream()
        .filter(w -> Objects.equals(w.getBookId(), bookId))
        .sorted(Comparator.comparingLong(Waitlist::getId))
        .toList();

    for (int i = 0; i < entries.size(); i++) {
        if (Objects.equals(entries.get(i).getUserId(), userId)) {
            return (i + 1); // 1-based
        }
    }
    return -1;
}*/
    public Result addToWaitlist(String bookId){//, String userId) {

        Book book=getBook(bookId);
        if (book==null){
            return Result.failure().add("invalid.book",bookId); //error("Invalid BookId "+bookId);
        }
        // ensure not already waitlisted for this book
        boolean exists = waitlists.values().stream()
            .anyMatch(w -> Objects.equals(w.getBookId(), bookId)
                        && Objects.equals(w.getUserId(), Global.userId()));
        if (exists) {
            return Result.failure().add("already.waitlisted", bookId);
        }

        long id = ++waitlistReferenceNumber;
        Waitlist w = new Waitlist(id, bookId, Global.userId(), java.time.Instant.now());
        waitlists.put(id, w);
        int position=getWaitlistPosition(bookId, Global.userId()).orElse(-1);
        return Result.success().add("waitlist.success", bookId,position);
    }
    public Result removeFromWaitlist(String bookId) {
        Long toRemove = waitlists.entrySet().stream()
            .filter(e -> Objects.equals(e.getValue().getBookId(), bookId)
                      && Objects.equals(e.getValue().getUserId(), Global.userId()))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);

        if (toRemove == null) return Result.failure().add("not.waitlisted", bookId);
        waitlists.remove(toRemove);
        return Result.success();
    }

    //private boolean isAdmin() {
    //    return "ADMIN".equalsIgnoreCase(Global.userId());
    //}

    private String resolveUserFilter(String requestedUserId, boolean isAdmin) {
        if (isAdmin) {
            // admin w/ null or blank -> wildcard all users
            return (requestedUserId == null || requestedUserId.isBlank()) ? "*" : requestedUserId;
        }
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
    if (items == null || items.isEmpty()) return "(no loans)";

    List<Loan> sorted = items.stream()
        .sorted(Comparator
            .comparing(Loan::getUserId, Comparator.nullsFirst(String::compareTo))
            .thenComparing(Loan::getId,   Comparator.nullsFirst(String::compareTo)))
        .toList();

    return PrintUtil.renderTable(
        List.of("User", "Loan Id", "Book Id", "Borrowed On"),
        sorted,
        l -> List.of(
            nvl(l.getUserId()),
            nvl(l.getId()),
            nvl(l.getBookId()),
            l.getBorrowedDate() == null
                ? ""
                : java.time.format.DateTimeFormatter.ISO_INSTANT.format(l.getBorrowedDate())
        )
    );
}


    private String renderBooksTable(List<Book> items) {
    if (items == null || items.isEmpty()) return "(no books)";

    return PrintUtil.renderTable(
        List.of("Book", "Total Copies", "Available Copies"),
        items,
        b -> {
            int total = b.getCopies();
            int available = Math.max(0, total - borrowedCount(b.getBookId())); // or availableCopies(b)
            return List.of(b.getBookId(), String.valueOf(total), String.valueOf(available));
        }
    );
    }


    public Result status(String userId){
        //UserService userService=new UserService();
        final boolean isAdmin = userService.isAdmin();
        if (!isAdmin && userId != null && !userId.isBlank() 
                && !Objects.equals(userId, Global.userId())) {
            return Result.failure().add("not.authorized");
        }

        final String queryUser = resolveUserFilter(userId, isAdmin);

        List<Loan> filtered = loans.values().stream()
            .filter(l -> wildcardMatch(l.getUserId(), queryUser))
            .sorted(Comparator
                .comparing(Loan::getUserId, Comparator.nullsFirst(String::compareTo))
                .thenComparing(Loan::getId, Comparator.nullsFirst(String::compareTo)))
            .collect(Collectors.toList());

        String table = renderLoansTable(filtered);

        return Result.success()
            .add("table", table);
    }


/*public Result waitlistStatusOld(String userId){
    final boolean isAdmin = userService.isAdmin();
    if (!isAdmin && userId != null && !userId.isBlank() 
            && !Objects.equals(userId, Global.userId())) {
        return Result.failure().add("not.authorized");
    }
    final String queryUser = resolveUserFilter(userId, isAdmin);
    
    // Query books table where the waitlisted user is available
    List<WaitlistEntry> filtered = books.values().stream()
        .filter(book -> book.getWaitlist() != null && !book.getWaitlist().isEmpty())
        .flatMap(book -> book.getWaitlist().stream()
            .filter(waitlistedUserId -> wildcardMatch(waitlistedUserId, queryUser))
            .map(waitlistedUserId -> new WaitlistEntry(
                book.getBookId(), 
                waitlistedUserId, 
                book.getWaitlist().indexOf(waitlistedUserId) + 1//, // position in waitlist (1-based)
                //book.getTitle(), // assuming you have a title field
                //book.getCopies()
            )))
        .sorted(Comparator
        .comparing(WaitlistEntry::getBookId, Comparator.nullsFirst(String::compareTo))
        .thenComparingInt(WaitlistEntry::getPosition)
        .thenComparing(WaitlistEntry::getUserId, Comparator.nullsFirst(String::compareTo))
        )
        .collect(Collectors.toList());
    
    String table = renderWaitlistTableOld(filtered);
    
    return Result.success()
        .add("table",table);
}*/

// Helper class to represent waitlist entries for display
private static class WaitlistEntry {
    private String bookId;
    private String userId;
    private int position;
    //private int totalCopies;
    
    public WaitlistEntry(String bookId, String userId, int position) {
        this.bookId = bookId;
        this.userId = userId;
        this.position = position;
        //this.totalCopies = totalCopies;
    }
    
    // Getters
    public String getBookId() { return bookId; }
    public String getUserId() { return userId; }
    public int getPosition() { return position; }
    //public int getTotalCopies() { return totalCopies; }
}
private String renderWaitlistTableOld(List<WaitlistEntry> items) {
    if (items == null || items.isEmpty()) return "(no waitlist)";

    return PrintUtil.renderTable(
        List.of("User", "Book Id", "Position"),
        items,
        w -> List.of(
            nvl(w.getUserId()),
            nvl(w.getBookId()),
            String.valueOf(w.getPosition())
        )
    );
}

public Result waitlistStatus(String userId) {
        final boolean isAdmin = userService.isAdmin();
        if (!isAdmin && userId != null && !userId.isBlank()
                && !Objects.equals(userId, Global.userId())) {
            return Result.failure().add("not.authorized");
        }

        final String queryUser = resolveUserFilter(userId, isAdmin);

        List<Waitlist> filtered = waitlists.values().stream()
            .filter(w -> wildcardMatch(w.getUserId(), queryUser))
            .sorted(Comparator
                .comparing(Waitlist::getBookId, Comparator.nullsFirst(String::compareTo))
                .thenComparingLong(Waitlist::getId))   // position comes from id order
            .toList();

        String table = renderWaitlistTable(filtered);
        return Result.success().add("table", table);
    }

    private String renderWaitlistTable(List<Waitlist> items) {
        if (items == null || items.isEmpty()) return "(no waitlist)";

        // compute per-book positions as we go
        Map<String, Integer> counters = new HashMap<>();
        return PrintUtil.renderTable(
            List.of("User", "Book Id", "Position", "Added On"),
            items,
            w -> {
                int pos = counters.merge(w.getBookId(), 1, Integer::sum);
                return List.of(
                    nvl(w.getUserId()),
                    nvl(w.getBookId()),
                    //String.valueOf(pos),
                    //String.valueOf(getWaitlistPosition(w.getBookId(), w.getUserId())),
                    String.valueOf(getWaitlistPosition(w.getBookId(), w.getUserId()).orElse(-1)),
                    java.time.format.DateTimeFormatter.ISO_INSTANT.format(w.getCreatedAt())
                );
            }
        );
    }

    //private static String nvl(String s) { return s == null ? "" : s; }
}