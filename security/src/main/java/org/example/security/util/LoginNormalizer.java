package org.example.security.util;

import java.util.Locale;

public final class LoginNormalizer {

    private LoginNormalizer() {
    }

    public static String normalize(String login) {
        if (login == null) {
            return null;
        }
        return login.trim().toLowerCase(Locale.ROOT);
    }
}
