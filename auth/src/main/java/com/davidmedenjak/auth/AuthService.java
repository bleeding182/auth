package com.davidmedenjak.auth;

import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A service to link your app with {@link OAuthAuthenticator}. This is a callback to refresh your
 * users access tokens or start a login flow.
 */
public interface AuthService {

    /**
     * Create an Intent to start your Login flow. This will be used if a user selects `Add Account`
     * in the Account Settings, or if you call {@link
     * android.accounts.AccountManager#addAccount(String, String, String[], Bundle, Activity,
     * AccountManagerCallback, Handler)} from your code. If you return `null` nothing will happen.
     *
     * @return an Intent that starts the flow to add an account, or {@code null}
     */
    @Nullable
    Intent getLoginIntent();

    /**
     * Re-authenticate the user with the previously stored refresh token. After success or error you
     * must call either {@link Callback#onAuthenticated(TokenPair)} or {@link
     * Callback#onError(Throwable)}, otherwise your application might end up in a deadlock.
     *
     * @param refreshToken the refresh token stored from {@link TokenPair#refreshToken} at the last
     *     login or refresh
     * @param callback callback to the authenticator waiting for a new token pair. Either {@link
     *     Callback#onAuthenticated(TokenPair)} or {@link Callback#onError(Throwable)} must be
     *     called in any case to notify any waiting threads.
     */
    void authenticate(@NonNull final String refreshToken, @NonNull final Callback callback);

    /** A callback that notifies the Authenticator of an authentication success or failure. */
    interface Callback {
        /**
         * Called after a token was successfully refreshed. This or {@link #onError(Throwable)} must
         * be called after {@link AuthService#authenticate(String, Callback)} was called.
         *
         * @param tokenPair the pair of a new access and refresh token
         * @see #onError(Throwable)
         */
        void onAuthenticated(@NonNull TokenPair tokenPair);

        /**
         * Called after the token refresh initiated by {@link AuthService#authenticate(String,
         * Callback)} failed. This or {@link #onAuthenticated(TokenPair)} must be called to notify
         * waiting threads.
         *
         * @param error the error encountered
         * @see #onAuthenticated(TokenPair)
         */
        void onError(@NonNull Throwable error);
    }
}
