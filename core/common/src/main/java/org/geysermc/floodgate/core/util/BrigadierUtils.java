package org.geysermc.floodgate.core.util;

/*
Code taken from Brigadier's StringArgumentType and StringReader
 */
public final class BrigadierUtils {
    public static boolean isAllowedInUnquotedString(char c) {
        return c >= '0' && c <= '9' ||
                c >= 'A' && c <= 'Z' ||
                c >= 'a' && c <= 'z' ||
                c == '_' || c == '-' ||
                c == '.' || c == '+';
    }

    public static String escapeIfRequired(String input) {
        for (final char c : input.toCharArray()) {
            if (!isAllowedInUnquotedString(c)) {
                return "\"" + input + "\"";
            }
        }
        return input;
    }

    public static String escapeIfRequired(String input, boolean quoted) {
        if (quoted) {
            return escape(input);
        }

        for (final char c : input.toCharArray()) {
            if (!isAllowedInUnquotedString(c)) {
                return "\"" + input + "\"";
            }
        }
        return input;
    }

    private static String escape(final String input) {
        final StringBuilder result = new StringBuilder("\"");

        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '\\' || c == '"') {
                result.append('\\');
            }
            result.append(c);
        }

        result.append("\"");
        return result.toString();
    }
}
