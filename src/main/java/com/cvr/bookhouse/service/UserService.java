package com.cvr.bookhouse.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.core.Global;
import com.cvr.bookhouse.model.User;

@Service
public class UserService {
    private final Map<String, User> users = new HashMap<>();

    public User upsertUser(String userId) {
        return users.computeIfAbsent(userId, id -> {
            Set<String> roles;
            if (id.toLowerCase().startsWith("admin")) {
                roles = Set.of("ADMIN");
            } else {
                roles = Set.of("USER");
            }
            return new User(id, roles);
        });
    }
    public void updateLoginDate(String userId) {
        User u = upsertUser(userId);
        u.setLastLoginDate(Instant.now());
    }
    public Optional<User> findUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }
    public boolean isAdmin() {
        return users.get(Global.userId()).getRoles().contains("ADMIN");
        //return "ADMIN".equalsIgnoreCase(Global.userId());
    }
}