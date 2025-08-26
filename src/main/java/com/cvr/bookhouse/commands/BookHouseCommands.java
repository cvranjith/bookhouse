package com.cvr.bookhouse.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.service.BookService;
import com.cvr.bookhouse.service.LoanService;

@ShellComponent
public class BookHouseCommands {

    private final LoanService svc;
    private final MessageFormatter fmt;

    public BookHouseCommands(LoanService svc, MessageFormatter fmt) {
        this.svc = svc;
        this.fmt = fmt;
    }

    @ShellMethod(key = "ok",value = "I will return ok")
    public String getOk() {
        return "ok";
    }
    @ShellMethod(key = "borrow", value = "Borrow a book. Usage: borrow <bookId>")
        public String borrow(
            @ShellOption(help = "Book id") String bookId) {
        return fmt.format(svc.borrow(bookId));
    }
}
