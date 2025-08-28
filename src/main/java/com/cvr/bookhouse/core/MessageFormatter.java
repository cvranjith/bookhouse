package com.cvr.bookhouse.core;

import org.jline.reader.LineReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class MessageFormatter {
    private static final String RESET = "\u001B[0m";
    private static final String RED   = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final Map<String, String> TAGS = Map.ofEntries(
        Map.entry("RESET",  "\u001B[0m"),
        Map.entry("BOLD",   "\u001B[1m"),
        Map.entry("DIM",    "\u001B[2m"),
        Map.entry("UNDER",  "\u001B[4m"),
        Map.entry("BLINK",  "\u001B[5m"),
        Map.entry("REV",    "\u001B[7m"),
        Map.entry("BLACK",  "\u001B[30m"),
        Map.entry("RED",    "\u001B[31m"),
        Map.entry("GREEN",  "\u001B[32m"),
        Map.entry("YELLOW", "\u001B[33m"),
        Map.entry("BLUE",   "\u001B[34m"),
        Map.entry("MAGENTA","\u001B[35m"),
        Map.entry("CYAN",   "\u001B[36m"),
        Map.entry("WHITE",  "\u001B[37m")
    );

    private static final Pattern TAG_PATTERN = Pattern.compile("\\[\\[([A-Z_]+)\\]\\]");

    private static final Path LOG = Paths.get("session.log");
    private static final DateTimeFormatter TS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[;\\d]*m");

    //private final ObjectProvider<LineReader> lineReaderProvider; // lazy
    //private final Session session;
    private final MessageSource messages;

    // Optional: allow disabling color via env var NO_COLOR=true
    private static final boolean NO_COLOR = Boolean.parseBoolean(System.getenv().getOrDefault("NO_COLOR", "false"));

    public MessageFormatter(ObjectProvider<LineReader> lineReaderProvider,
                            //Session session,
                            MessageSource messages) {
        //this.lineReaderProvider = lineReaderProvider;
        //this.session = session;
        this.messages = messages;
    }

public String format(Result r) {
    final var loc = LocaleContextHolder.getLocale();
    final String base = (NO_COLOR ? "" : (r.ok() ? GREEN : RED));

    // Resolve codes -> text, apply inline tags with base-aware RESET,
    // then wrap each line with base color and finish with a hard RESET.
    String out = r.messages().stream()
        .map(m -> resolve(m.code(), m.args(), loc)) // safe resolve
        .map(msg -> applyInlineTags(msg, base))     // [[RESET]] -> RESET + base
        .map(line -> base + line + RESET)           // color each line; end with full reset
        .collect(Collectors.joining("\n"));

    // Optional: write de-colored line(s) to session.log
    try {
        String plain = stripAnsi(out);
        String ts = TS.format(Instant.now());
        String who = Optional.ofNullable(Global.userId()).orElse("");
        Files.writeString(LOG, ts + " | " + who + " | " + plain + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (Exception ignore) {}

    return out;
}


private String resolve(String code, Object[] args, Locale loc) {
    try {
        // Bold args without resetting colors:
        // Use BOLD_ON (1m) and BOLD_OFF (22m) so we don't lose the base color.
        final String BOLD_ON  = "\u001B[1m";
        final String BOLD_OFF = "\u001B[22m";

        Object[] styledArgs = null;
        if (args != null) {
            styledArgs = Arrays.stream(args)
                .map(a -> {
                    String s = (a == null) ? "" : a.toString();
                    if (NO_COLOR || s.isEmpty()) return s;
                    return BOLD_ON + s + BOLD_OFF;
                })
                .toArray();
        }

        // If code missing, show ??code??
        return messages.getMessage(code, styledArgs, "??" + code + "??", loc);
    } catch (Exception e) {
        // Any formatting/parsing error → degrade gracefully
        return "??" + code + "??";
    }
}


    /** Replace [[TAG]] with ANSI codes; unknown tags removed. Honors NO_COLOR toggle. */
/** Replace [[TAG]] with ANSI codes; [[RESET]] restores base color. Honors NO_COLOR. */
private String applyInlineTags(String s, String base) {
    if (NO_COLOR) {
        // strip inline tags entirely when NO_COLOR is set
        return TAG_PATTERN.matcher(s).replaceAll("");
    }
    return TAG_PATTERN.matcher(s).replaceAll(match -> {
        String tag = match.group(1);
        if ("RESET".equals(tag)) {
            // reset inline styles but immediately restore the base color (GREEN/RED)
            return RESET + base;
        }
        return TAGS.getOrDefault(tag, "");
    });
}

    private String stripAnsi(String s) {
        return ANSI.matcher(s).replaceAll("");
    }

}
