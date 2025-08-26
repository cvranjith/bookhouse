// core/MessageFormatter.java
package com.cvr.bookhouse.core;

import org.jline.reader.LineReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.cvr.bookhouse.model.Session;
//import com.cvr.bookhouse.service.AuthService;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class MessageFormatter {
    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final Path LOG = Paths.get("session.log");
    private static final DateTimeFormatter TS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ObjectProvider<LineReader> lineReaderProvider; // LAZY
    private final Session session;

    public MessageFormatter(ObjectProvider<LineReader> lineReaderProvider, Session session) {
        this.lineReaderProvider = lineReaderProvider;
        this.session = session;
    }

    public static String formatDateTime( Instant dateTime) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneId.systemDefault())
                                .format(dateTime);
    }    

    private static String pad(String s, int n, boolean left, String padChar) {
        if (s == null) s = "";
        if (padChar == null || padChar.isEmpty()) padChar = " ";

        // truncate with ellipsis if too long
        if (s.length() > (n - 3)) {
            s = s.substring(0, n - 3) + "...";
        }

        int padLen = Math.max(0, n - s.length());
        String filler = padChar.repeat(padLen);

        return left ? filler + s : s + filler;
    }

    private static String lpad(String s, int n, String padChar) {
        return pad(s, n, true, padChar);
    }

    private static String rpad(String s, int n, String padChar) {
        return pad(s, n, false, padChar);
    }

    public static StringBuilder formatLine(String sl,String bookId, String tot, String avl, String padChar){
        StringBuilder sb = new StringBuilder();
        sb.append(lpad(sl, 5,padChar)).append(" | ")
        .append(rpad(bookId, 40,padChar)).append(" | ")
        .append(lpad(tot, 20,padChar)).append(" | ")
        .append(lpad(avl, 20,padChar)).append("\n");
        return sb;
    }

    public String format(Result r) {
        String colored = (r.ok() ? GREEN : RED) + r.msg() + RESET;

        String user = session.getUserId();
        String cmd  = lastCommand();
        String status = r.ok() ? "OK" : "ERR";

        String line = String.format("%s | user=%s | cmd=%s | status=%s | result=%s%n",
                TS.format(Instant.now()), user, cmd, status, r.msg().replaceAll("\\R", " ↵ "));
        try {
            Files.writeString(LOG, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("session.log write failed: " + e.getMessage());
        }
        return colored;
    }

    //private String currentUser() {
    //    String u = auth.getUserId();
    //    return (u == null || u.isEmpty()) ? "-" : u;
    //}

    private String lastCommand() {
        try {
            LineReader lr = lineReaderProvider.getIfAvailable(); // resolves now, not at startup
            if (lr == null || lr.getHistory() == null || lr.getHistory().size() == 0) return "<unknown>";
            return lr.getHistory().get(lr.getHistory().size() - 1).toString();
        } catch (Exception e) {
            return "<unknown>";
        }
    }
}
