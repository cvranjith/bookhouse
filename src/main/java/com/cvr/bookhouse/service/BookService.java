package com.cvr.bookhouse.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.core.Msg;
import com.cvr.bookhouse.core.PrintUtil;
import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Book;
import com.cvr.bookhouse.model.Loan;
import com.cvr.bookhouse.model.Waitlist;

@Service
public class BookService {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Loan> loans = new HashMap<>();

    private final Map<Long, Waitlist> waitlists = new HashMap<>();

    private int loanReferenceNumber =0;
    private long waitlistReferenceNumber = 0;

    private final UserService userService;

    public BookService(UserService userService)
    {
        this.userService=userService;
    }

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

        Result gate = checkBorrowGate(bookId, Global.userId(), book);
        if (gate != null) return gate; // BLOCK here if queue has priority

        List<Msg> msgs = new ArrayList<>();


        if (removeFromWaitlistIfPresent(bookId, Global.userId())) {
            msgs.add(new Msg("waitlist.removed", bookId));
        }


        String loanId = nextLoanId();//UUID.randomUUID().toString();
        Instant borrowedDate=Instant.now();
        loans.put(loanId, new Loan(loanId, bookId, Global.userId(), borrowedDate));

        msgs.add(new Msg("borrow.success",bookId,borrowedDate));
        msgs.add(new Msg("loan.reference",loanId));
        return Result.success(msgs);
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
        Result loanStatus= loanStatus(userId);
        Result waitlistStatus=waitlistStatus(userId);

        List<Msg> msgs = new ArrayList<>();

        msgs.add(new Msg("table", "[[MAGENTA]] Loan Status: [[RESET]]"));
        msgs.addAll(loanStatus.messages());
        msgs.add(new Msg("table", "[[MAGENTA]] Waitlist Status: [[RESET]]"));
        msgs.addAll(waitlistStatus.messages());
        
        return Result.success(msgs);
    }


    public Result loanStatus(String userId){
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
                .thenComparingLong(Waitlist::getId))
            .toList();

        String table = renderWaitlistTable(filtered);
        return Result.success().add("table", table);
    }

    private String renderWaitlistTable(List<Waitlist> items) {
        if (items == null || items.isEmpty()) return "(no waitlist)";
        return PrintUtil.renderTable(
            List.of("User", "Book Id", "Position", "Added On"),
            items,
            w -> {
                return List.of(
                    nvl(w.getUserId()),
                    nvl(w.getBookId()),
                    String.valueOf(getWaitlistPosition(w.getBookId(), w.getUserId()).orElse(-1)),
                    java.time.format.DateTimeFormatter.ISO_INSTANT.format(w.getCreatedAt())
                );
            }
        );
    }

public Result areBooksAvailableNow(String userId) {
    if (userId == null || userId.isBlank()) {
        return Result.success();
    }

    // All bookIds the user is waitlisted on (deduped)
    Set<String> myBookIds = waitlists.values().stream()
        .filter(w -> Objects.equals(w.getUserId(), userId))
        .map(Waitlist::getBookId)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    List<Msg> msgs = new ArrayList<>();

    for (String bookId : myBookIds) {
        Book book = books.get(bookId);
        if (book == null) continue;

        int copies   = book.getCopies();
        int borrowed = borrowedCount(bookId);
        int available = copies - borrowed;
        if (available <= 0) continue; // no copies to allocate

        OptionalInt posOpt = getWaitlistPosition(bookId, userId);
        // user is on the waitlist & their position is within the available copies
        if (posOpt.isPresent() && posOpt.getAsInt() <= available) {
            msgs.add(new Msg("book.available", bookId));
        }
    }

    return Result.success(msgs);//.add("msgs", msgs);
}
public boolean anyBooksAvailableNow(String userId) {
    return !areBooksAvailableNow(userId).messages().isEmpty();
}
}