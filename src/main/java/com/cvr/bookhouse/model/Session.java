package com.cvr.bookhouse.model;

import org.springframework.stereotype.Component;

@Component
public class Session {
    private final java.util.concurrent.atomic.AtomicReference<String> userId =
        new java.util.concurrent.atomic.AtomicReference<>("");
public void setUserId(String userId) {
    this.userId.set(userId);
}
public String getUserId(){
    return userId.get();
}
}
