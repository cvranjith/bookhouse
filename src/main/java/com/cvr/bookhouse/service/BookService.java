package com.cvr.bookhouse.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Book;
import com.cvr.bookhouse.model.Session;

@Service
public class BookService {
    private final Map<String, Book> books = new HashMap<>();
 
    private final Session session;

    /*public BookService(Session session, UserService users) {
        this.session = session;
    }*/

    public BookService(Session session) {
        this.session = session;
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
        Result adminCheck = isUserAdmin();
        if (!adminCheck.ok()) return adminCheck;

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

private String pad(String s, int n, boolean left, String padChar) {
    if (s == null) s = "";
    if (padChar == null || padChar.isEmpty()) padChar = " ";

    // truncate with ellipsis if too long
    if (s.length() > (n - 3)) {
        s = s.substring(0, n - 3) + "...";
    }

    int padLen = Math.max(0, n - s.length());
    String filler = padChar.repeat(padLen);

    return left ? filler + s : s + filler;
}

private String lpad(String s, int n, String padChar) {
    return pad(s, n, true, padChar);
}

private String rpad(String s, int n, String padChar) {
    return pad(s, n, false, padChar);
}

private StringBuilder formatLine(String sl,String bookId, String tot, String avl, String padChar){
    StringBuilder sb = new StringBuilder();
    sb.append(lpad(sl, 5,padChar)).append(" | ")
      .append(rpad(bookId, 40,padChar)).append(" | ")
      .append(lpad(tot, 20,padChar)).append(" | ")
      .append(lpad(avl, 20,padChar)).append("\n");
    return sb;
}
private String renderBooksTable(List<Book> items) {
    if (items.isEmpty()) return "(no books)";

    StringBuilder sb = formatLine("sl","Book","Total Copies","Available Copies"," ");
    sb.append(formatLine("-","-","-","-","-"));

    int i = 1;
    for (Book b : items) {
        sb.append( formatLine(String.valueOf(i++), b.getBookId(), String.valueOf(b.getCopies()), "todo"," "));
    }

    return sb.toString();
}
}