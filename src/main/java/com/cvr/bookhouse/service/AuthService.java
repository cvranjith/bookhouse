package com.cvr.bookhouse.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.security.authentication.*;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.core.Msg;
import com.cvr.bookhouse.core.Result;
import com.cvr.bookhouse.model.User;

@Service
public class AuthService {
    private final UserService userService;
    private final BookService bookService;
    private final AuthenticationManager authenticationManager;

    public AuthService(AuthenticationManager authenticationManager,
            BookService bookService,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.bookService = bookService;
        this.userService = userService;
    }

    private boolean isLoggedIn() {
        String uid = Global.userId();
        return uid != null && !uid.isBlank();
    }

    public Result login(String newUserId) {
        if (newUserId.equals(Global.userId())) {
            return Result.failure()
                    .add("already.loggedin", Global.userId());
        }
        if (isLoggedIn()) {
            Result logoutResult = logout();
            if (!logoutResult.ok()) {
                return logoutResult;
            }
        }
        User u = userService.upsertUser(newUserId);
        Instant lastLoginDate = u.getLastLoginDate();
        userService.updateLoginDate(newUserId);

        UsernamePasswordAuthenticationToken req = UsernamePasswordAuthenticationToken.unauthenticated(newUserId, "");
        Authentication auth = authenticationManager.authenticate(req);
        SecurityContextHolder.getContext().setAuthentication(auth);

        List<Msg> msgs = new ArrayList<>();
        msgs.add(new Msg("login.success", Global.userId()));
        if (lastLoginDate == null) {
            msgs.add(new Msg("login.first"));
        } else {
            msgs.add(new Msg("login.last", lastLoginDate));
        }
        if (u.getRoles().contains("ADMIN")) {
            msgs.add(new Msg("admin.login"));
        }
        Result status = bookService.status(Global.userId());
        msgs.addAll(status.messages());

        Result avilableBooks = bookService.areBooksAvailableNow(Global.userId());
        if (!avilableBooks.messages().isEmpty()) {
            msgs.add(new Msg("books.available.now"));
            msgs.addAll(avilableBooks.messages());
        }

        return Result.success(msgs);
    }

    public Result logout() {
        if (!isLoggedIn()) {
            return Result.failure()
                    .add("already.loggedin", Global.userId());
        }
        String oldUser = Global.userId();
        SecurityContextHolder.clearContext();
        return Result.success()
                .add("logged.out", oldUser);
    }

}
