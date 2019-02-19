package com.davidmedenjak.auth.manager;

/** Constants for different token types that get persisted. */
public final class TokenType {
    private TokenType() {}

    /**
     * Basic {@code bearer} token type that gets used to store access tokens with {@link
     * com.davidmedenjak.auth.manager.OAuthAccountManager}.
     */
    public static String BEARER = "bearer";
}
