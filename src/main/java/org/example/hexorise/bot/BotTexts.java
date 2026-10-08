package org.example.hexorise.bot;
import java.util.*;
import java.text.MessageFormat;
public final class BotTexts {
    private static final ResourceBundle TEXTS = ResourceBundle.getBundle("bot-messages", Locale.ROOT);
    private BotTexts() {}
    public static String text(String key, Object... arguments) { return arguments.length == 0 ? TEXTS.getString(key) : MessageFormat.format(TEXTS.getString(key), arguments); }
    public static String command(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        for (String key : TEXTS.keySet()) {
            if (key.startsWith("alias.") && TEXTS.getString(key).equals(normalized)) return key.substring(6);
        }
        return normalized;
    }
}
