package com.cvr.bookhouse.core;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class Global {

    private Global() {}

    public static String userId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal()))
                ? ""
                : a.getName();
    }
}
