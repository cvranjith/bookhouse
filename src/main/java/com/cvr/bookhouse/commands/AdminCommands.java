package com.cvr.bookhouse.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.service.BookService;

@ShellComponent
public class AdminCommands {

    private final BookService svc;
    private final MessageFormatter fmt;

    public AdminCommands(BookService svc, MessageFormatter fmt) {
        this.svc = svc;
        this.fmt = fmt;
    }

    @ShellMethod(key = "add-book", value = "Add a book. Usage: add-book <bookId> [--copies <n>]")
        public String addBook(
            @ShellOption(help = "Book id") String bookId,
            @ShellOption(help = "Number of copies", defaultValue = "1") Integer copies) {
        return fmt.format(svc.addBook(bookId,copies));
    }
    @ShellMethod(key = "list", value = "List books. Usage: list [bookId]")
        public String addBook(
            @ShellOption(help = "Book id", defaultValue="") String bookId) {
        return fmt.format(svc.list(bookId));
    }
}
