package com.cvr.bookhouse.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cvr.bookhouse.model.User;

@Service
public class UserService {
    private final Map<String, User> users = new HashMap<>();

    public User upsertUser(String userId) {
        return users.computeIfAbsent(userId, id -> new User(id));
    }
    public void updateLoginDate(String userId) {
        User u = upsertUser(userId);
        u.setLastLoginDate(Instant.now());
    }
    public Optional<User> findUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }
}