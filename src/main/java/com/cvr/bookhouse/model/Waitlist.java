package com.cvr.bookhouse.model;

import java.time.Instant;

public class Waitlist {
    long id;
    String bookId;
    String userId;
    Instant createdAt;

    public Waitlist(long id, String bookId, String userId, java.time.Instant createdAt) {
        this.id = id;
        this.bookId = bookId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getBookId() {
        return bookId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
