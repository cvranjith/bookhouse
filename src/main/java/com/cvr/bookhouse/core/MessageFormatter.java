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
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final Map<String, String> TAGS = Map.ofEntries(
            Map.entry("RESET", "\u001B[0m"),
            Map.entry("BOLD", "\u001B[1m"),
            Map.entry("DIM", "\u001B[2m"),
            Map.entry("UNDER", "\u001B[4m"),
            Map.entry("BLINK", "\u001B[5m"),
            Map.entry("REV", "\u001B[7m"),
            Map.entry("BLACK", "\u001B[30m"),
            Map.entry("RED", "\u001B[31m"),
            Map.entry("GREEN", "\u001B[32m"),
            Map.entry("YELLOW", "\u001B[33m"),
            Map.entry("BLUE", "\u001B[34m"),
            Map.entry("MAGENTA", "\u001B[35m"),
            Map.entry("CYAN", "\u001B[36m"),
            Map.entry("WHITE", "\u001B[37m"));

    private static final Pattern TAG_PATTERN = Pattern.compile("\\[\\[([A-Z_]+)\\]\\]");

    private static final Path LOG = Paths.get("session.log");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[;\\d]*m");

    private final MessageSource messages;

    private static final boolean NO_COLOR = Boolean.parseBoolean(System.getenv().getOrDefault("NO_COLOR", "false"));

    public MessageFormatter(ObjectProvider<LineReader> lineReaderProvider,
            MessageSource messages) {
        this.messages = messages;
    }

    public String format(Result r) {
        final var loc = LocaleContextHolder.getLocale();
        final String base = (NO_COLOR ? "" : (r.ok() ? GREEN : RED));
        String out = r.messages().stream()
                .map(m -> resolve(m.code(), m.args(), loc))
                .map(msg -> applyInlineTags(msg, base))
                .map(line -> base + line + RESET)
                .collect(Collectors.joining("\n"));
        try {
            String plain = stripAnsi(out);
            String ts = TS.format(Instant.now());
            String who = Optional.ofNullable(Global.userId()).orElse("");
            Files.writeString(LOG, ts + " | " + who + " | " + plain + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignore) {
        }

        return out;
    }

    private String resolve(String code, Object[] args, Locale loc) {
        try {
            final String BOLD_ON = "\u001B[1m";
            final String BOLD_OFF = "\u001B[22m";
            Object[] styledArgs = null;
            if (args != null) {
                styledArgs = Arrays.stream(args)
                        .map(a -> {
                            String s = (a == null) ? "" : a.toString();
                            if (NO_COLOR || s.isEmpty())
                                return s;
                            return BOLD_ON + s + BOLD_OFF;
                        })
                        .toArray();
            }

            return messages.getMessage(code, styledArgs, "??" + code + "??", loc);
        } catch (Exception e) {
            return "??" + code + "??";
        }
    }

    private String applyInlineTags(String s, String base) {
        if (NO_COLOR) {
            return TAG_PATTERN.matcher(s).replaceAll("");
        }
        return TAG_PATTERN.matcher(s).replaceAll(match -> {
            String tag = match.group(1);
            if ("RESET".equals(tag)) {
                return RESET + base;
            }
            return TAGS.getOrDefault(tag, "");
        });
    }

    private String stripAnsi(String s) {
        return ANSI.matcher(s).replaceAll("");
    }

}
