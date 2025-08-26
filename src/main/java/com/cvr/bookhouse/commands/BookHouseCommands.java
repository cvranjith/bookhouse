package com.cvr.bookhouse.commands;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class BookHouseCommands {

    @ShellMethod(key = "ok",value = "I will return ok")
    public String getOk() {
        return "ok";
    }
}
