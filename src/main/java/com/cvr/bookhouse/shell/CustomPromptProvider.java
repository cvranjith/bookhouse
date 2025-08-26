package com.cvr.bookhouse.shell;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.Order;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import com.cvr.bookhouse.model.Session;
//import com.cvr.bookhouse.service.AuthService;

@Component
public class CustomPromptProvider implements PromptProvider {

    private final Session session;

    public CustomPromptProvider(Session session) {
        this.session = session;
    }

    @Override
    public AttributedString getPrompt() {
        String user = session.getUserId();
        String prompt = (user == null || user.isEmpty()) ? ":>" : user + ":>";
        return new AttributedString(prompt + " ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
    }
}
