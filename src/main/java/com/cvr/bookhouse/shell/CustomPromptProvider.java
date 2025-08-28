package com.cvr.bookhouse.shell;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import com.cvr.bookhouse.core.Global;

@Component
public class CustomPromptProvider implements PromptProvider {

    public CustomPromptProvider() {
    }

    @Override
    public AttributedString getPrompt() {
        String user = Global.userId();
        String prompt = (user == null || user.isEmpty()) ? ":>" : user + ":>";
        return new AttributedString(prompt + " ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
    }
}
