package com.cvr.bookhouse.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.service.AuthService;

@ShellComponent
public class AuthCommands {

    private final AuthService auth;
    private final MessageFormatter fmt;

    public AuthCommands(AuthService auth, MessageFormatter fmt) {
        this.auth = auth;
        this.fmt = fmt;
    }

    @ShellMethod(key = "login",value = "Login with a user id. Usage: login <userId>")
    public String login(@ShellOption(help = "User id") String userId) {
        return fmt.format(auth.login(userId));
    }
    @ShellMethod(key = "logout", value = "Logout the current user.")
    public String logout() {
        return fmt.format(auth.logout());
    }
}
