package com.cvr.bookhouse.service;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Session;

@Service
public class AuthService {
//String userId="";
    private final Session session;

    public AuthService(Session session) {
        this.session = session;
    }
    public boolean isLoggedIn() {   
        //return !userId.isEmpty();
        return !session.getUserId().isEmpty();
    }
    public Result login(String newUserId) {
        if (newUserId.equals(session.getUserId())) {
            return Result.error("Nothing to do; you are already logged in as: " + session.getUserId());
        }
        if (isLoggedIn()) {
            // force logout first
            Result logoutResult = logout();
            if (!logoutResult.ok()) {
                return logoutResult;
            }
        }
        session.setUserId(newUserId);
        return Result.ok("Logged in as: " + session.getUserId());
    }

    public Result logout() {
        if (!isLoggedIn()) {
            return Result.error("Nothing to do; you are not logged in.");
        }
        String oldUser = session.getUserId();
        session.setUserId("");
        return Result.ok("User " + oldUser + " has been logged out.");
    }
}
