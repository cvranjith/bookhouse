package com.cvr.bookhouse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.cvr.bookhouse.model.User;

public class UserServiceTest {

    @Test
    void testUpsertUser() {
        UserService svc = new UserService();
        assertTrue(svc.findUser("cv").isEmpty(), "User should not exist before upsert");
        User u1 = svc.upsertUser("cv");
        assertNotNull(u1);
        assertEquals("cv", u1.getUserId());
        User u2 = svc.upsertUser("cv");
        assertSame(u1, u2, "Upsert should return the instance for same id");
    }

    @Test
        void testUpdateLoginDate() throws InterruptedException {
            UserService svc = new UserService();
            User u = svc.upsertUser("cv");
            Instant firstlLastLoginDate = u.getLastLoginDate();
            assertNull(firstlLastLoginDate, "lastLoginDate should be null at start");
            svc.updateLoginDate("cv");
            Instant secondLastLoginDate = svc.findUser("cv").orElseThrow().getLastLoginDate();
            assertNotNull(secondLastLoginDate);
            Thread.sleep(10);
            svc.updateLoginDate("cv");
            Instant thirdLastLoginDate = svc.findUser("cv").orElseThrow().getLastLoginDate();
            assertTrue(thirdLastLoginDate.isAfter(secondLastLoginDate),
                "lastLoginDate should move forward");
    }

}
