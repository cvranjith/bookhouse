package com.cvr.bookhouse.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.service.BookService;
//import com.cvr.bookhouse.service.LoanService;

@ShellComponent
public class BookHouseCommands {

    private final BookService bookService;
    //private final LoanService loanService;
    private final MessageFormatter messageFormatter;

    public BookHouseCommands(BookService bookService, /*LoanService loanService,*/ MessageFormatter messageFormatter) {
        this.bookService = bookService;
        //this.loanService = loanService;
        this.messageFormatter = messageFormatter;
    }

    @ShellMethod(key = "ok",value = "I will return ok")
    public String getOk() {
        return "ok";
    }
    @ShellMethod(key = "borrow", value = "Borrow a book. Usage: borrow <bookId>")
        public String borrowBook(
            @ShellOption(help = "Book id") String bookId) {
        return messageFormatter.format(bookService.borrowBook(bookId));
    }
    @ShellMethod(key = "list", value = "List books. Usage: list [bookId]")
        public String addBook(
            @ShellOption(help = "Book id", defaultValue="") String bookId) {
        return messageFormatter.format(bookService.list(bookId));
    }
}
