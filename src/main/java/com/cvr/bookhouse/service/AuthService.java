package com.cvr.bookhouse.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.security.authentication.*;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.core.MessageFormatter;
import com.cvr.bookhouse.core.Msg;
import com.cvr.bookhouse.core.Result;
//import com.cvr.bookhouse.model.Session;
import com.cvr.bookhouse.model.User;

@Service
public class AuthService {
//String userId="";
    //private final Session session;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthService(AuthenticationManager authenticationManager,
        //Session session,
        UserService userService) {
        //this.session = session;
        this.userService=userService;
        this.authenticationManager=authenticationManager;
    }
    private boolean isLoggedIn() {   
        //return !userId.isEmpty();
        //TODO***
        return true;//!session.getUserId().isEmpty();
    }
    private Authentication current() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    public Result login(String newUserId) {
        if (newUserId.equals(Global.userId())) {
            return Result.failure()
                .add("already.loggedin",Global.userId()); // "Nothing to do; you are already logged in as: " + session.getUserId());
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
        //session.setUserId(newUserId);
        userService.updateLoginDate(newUserId);

        UsernamePasswordAuthenticationToken req =
        UsernamePasswordAuthenticationToken.unauthenticated(newUserId, "");
        Authentication auth = authenticationManager.authenticate(req);
        SecurityContextHolder.getContext().setAuthentication(auth);

        List<Msg> msgs = new ArrayList<>();
        msgs.add(new Msg("login.success",Global.userId()));
        if (lastLoginDate == null) {
            msgs.add(new Msg("login.first"));
        } else {
            msgs.add(new Msg("login.last", lastLoginDate));
        }
        if (u.getRoles().contains("ADMIN")) {
            msgs.add(new Msg("admin.login"));
        }
        return Result.success(msgs);
    }

    public Result logout() {
        if (!isLoggedIn()) {
            return Result.failure()
                    .add("already.loggedin",Global.userId());
        }
        String oldUser = Global.userId();
        //session.setUserId("");
        SecurityContextHolder.clearContext();
        return Result.success()
            .add("logged.out",oldUser);
    }


}
