package com.cvr.bookhouse.commands;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.service.BookService;

@ShellComponent
public class AdminCommands {

    private final BookService bookService;
    private final MessageFormatter messageFormatter;
    private static final Logger log = LoggerFactory.getLogger(AdminCommands.class);
    
    public AdminCommands(BookService bookService, MessageFormatter messageFormatter) {
        this.bookService = bookService;
        this.messageFormatter = messageFormatter;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @ShellMethod(key = "add-book", value = "Add a book. Usage: add-book <bookId> [--copies <n>]")
    public String addBook(
            @ShellOption(help = "Book id") String bookId,
            @ShellOption(help = "Number of copies", defaultValue = "1") Integer copies) {
        log.info("addBook bookId,copies = {},{}", bookId,copies);
        return messageFormatter.format(bookService.addBook(bookId, copies));
    }

}
