package com.cvr.bookhouse.commands;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.cvr.bookhouse.core.Global;
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

    //public APIs
    @ShellMethod(key = "whoami",value = "Returns the current User ID")
    public String whoami() {
        return Global.userId();
    }
    
    @ShellMethod(key = "list", value = "List books. Usage: list <bookId>")
    public String list(
            @ShellOption(help = "Book ID", defaultValue="") String bookId) {
        return messageFormatter.format(bookService.list(bookId));
    }

    //Authenticated APIs; only pre-authenticated users can run these
    @PreAuthorize("isAuthenticated()")
    @ShellMethod(key = "borrow", value = "Borrow a book. Usage: borrow <bookId>")
        public String borrowBook(
            @ShellOption(help = "Book ID") String bookId) {
        return messageFormatter.format(bookService.borrowBook(bookId));
    }

    @PreAuthorize("isAuthenticated()")
    @ShellMethod(key = "return",   value = "Return a book by book ID or loan ID.\n" +
          "Usage:\n" +
          "  return-book --book <bookId>\n" +
          "  return-book --loan <loanId>")
        public String returnBook(
            @ShellOption(help = "Book ID", defaultValue="") String bookId,
            @ShellOption(help = "Loan ID", defaultValue="") String loanId
            ) {
        return messageFormatter.format(bookService.returnBook(bookId,loanId));
    }

    @PreAuthorize("isAuthenticated()")
    @ShellMethod(key = "waitlist", value = "Join waitlist. Usage: waitlist <bookId>")
        public String waitlist(
            @ShellOption(help = "Book ID") String bookId) {
        return messageFormatter.format(bookService.joinWaitlist(bookId));
    }

    @PreAuthorize("isAuthenticated()")
    @ShellMethod(key = "cancel-waitlist", value = "Cancel waitlist. Usage: cancel-waitlist <bookId>")
        public String cancelWaitlist(
            @ShellOption(help = "Book ID") String bookId) {
        return messageFormatter.format(bookService.cancelWaitlist(bookId));
    }
    

    @PreAuthorize("isAuthenticated()")
    @ShellMethod(key = "status", value = "Print Loan Status Usage: status [<userId>]")
        public String status(
            @ShellOption(help = "User ID", defaultValue = "") String userId) {
        return messageFormatter.format(bookService.status(userId));
    }

    
}
