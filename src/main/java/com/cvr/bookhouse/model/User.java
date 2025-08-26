package com.cvr.bookhouse.model;

import java.time.Instant;

public class User {
String userId;
Instant lastLoginDate;

public User(String userId) {
        this.userId = userId;
}

public String getUserId() {
    return userId;
}
public void setUserId(String userId) {
    this.userId = userId;
}
public Instant getLastLoginDate() {
    return lastLoginDate;
}
public void setLastLoginDate(Instant lastLoginDate) {
    this.lastLoginDate = lastLoginDate;
}

}
