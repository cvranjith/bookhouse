package com.cvr.bookhouse.service;

import java.time.Instant;
import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.Session;
import com.cvr.bookhouse.model.User;

@Service
public class AuthService {
//String userId="";
    private final Session session;
    private final UserService userService;

    public AuthService(Session session,UserService userService) {
        this.session = session;
        this.userService=userService;
    }
    private boolean isLoggedIn() {   
        //return !userId.isEmpty();
        return !session.getUserId().isEmpty();
    }
    String loginBanner(String newUserId, Instant lastLoginDate ){
        String banner = "Welcome, "+ newUserId +"\n";
        if (lastLoginDate == null) {
            banner += "This is your first login.";
        } else {
            banner += "Your last login was at " + MessageFormatter.formatDateTime(lastLoginDate);
        }
        return banner;
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
        User u = userService.upsertUser(newUserId);
        Instant lastLoginDate = u.getLastLoginDate();
        session.setUserId(newUserId);
        userService.updateLoginDate(newUserId);
        return Result.ok(loginBanner(session.getUserId(),lastLoginDate));
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
