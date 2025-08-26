package com.cvr.bookhouse.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.service.AuthService;

@ShellComponent
public class AuthCommands {

    private final AuthService authService;
    private final MessageFormatter messageFormatter;

    public AuthCommands(AuthService authService, MessageFormatter messageFormatter) {
        this.authService = authService;
        this.messageFormatter = messageFormatter;
    }

    @ShellMethod(key = "login",value = "Login with a user id. Usage: login <userId>")
    public String login(@ShellOption(help = "User id") String userId) {
        return messageFormatter.format(authService.login(userId));
    }
    @ShellMethod(key = "logout", value = "Logout the current user.")
    public String logout() {
        return messageFormatter.format(authService.logout());
    }
}
