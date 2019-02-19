package com.davidmedenjak.auth.okhttp;

/**
 * HTTP header constants used for OAuth headers.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6750#section-2.1">OAuth 2.0 Authorization Framework:
 *     Bearer Token Usage</a>
 */
public final class Headers {
    /** HTTP {@code Authorization} header. */
    public static final String AUTHORIZATION = "Authorization";

    /** {@code Bearer} to be used within the {@link #AUTHORIZATION} header. */
    public static final String AUTH_BEARER = "Bearer ";

    private Headers() {}
}
