package com.davidmedenjak.auth;

import android.accounts.AccountManager;

import androidx.annotation.Nullable;

/**
 * Error to report failure when trying to refresh a token. We are limited by {@code AccountManager}
 * to return an error code and errorMessage only.
 *
 * @see #TokenRefreshError(int, String)
 */
public class TokenRefreshError extends Exception {

    public static final TokenRefreshError NETWORK =
            new TokenRefreshError(AccountManager.ERROR_CODE_NETWORK_ERROR, null);

    private final int code;
    private final String errorMessage;

    /**
     * Construct a new error using an error code and message to return as a result from the token
     * refresh operation.
     *
     * @param code the error code. May be one of the predefined error codes from {@link
     *     android.accounts.AccountManager AccountManager}
     *     <ul>
     *       <li>{@link android.accounts.AccountManager#ERROR_CODE_REMOTE_EXCEPTION
     *           ERROR_CODE_REMOTE_EXCEPTION},
     *       <li>{@link android.accounts.AccountManager#ERROR_CODE_NETWORK_ERROR
     *           ERROR_CODE_NETWORK_ERROR},
     *       <li>{@link android.accounts.AccountManager#ERROR_CODE_CANCELED ERROR_CODE_CANCELED},
     *       <li>{@link android.accounts.AccountManager#ERROR_CODE_INVALID_RESPONSE
     *           ERROR_CODE_INVALID_RESPONSE},
     *       <li>{@link android.accounts.AccountManager#ERROR_CODE_UNSUPPORTED_OPERATION
     *           ERROR_CODE_UNSUPPORTED_OPERATION},
     *       <li>{@link android.accounts.AccountManager#ERROR_CODE_BAD_ARGUMENTS
     *           ERROR_CODE_BAD_ARGUMENTS},
     *       <li>{@link android.accounts.AccountManager#ERROR_CODE_BAD_REQUEST
     *           ERROR_CODE_BAD_REQUEST},
     *       <li>{@link android.accounts.AccountManager#ERROR_CODE_BAD_AUTHENTICATION
     *           ERROR_CODE_BAD_AUTHENTICATION}
     *     </ul>
     *
     * @param errorMessage an optional errorMessage
     */
    public TokenRefreshError(int code, @Nullable String errorMessage) {
        this.code = code;
        this.errorMessage = errorMessage;
    }

    public int getCode() {
        return code;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
