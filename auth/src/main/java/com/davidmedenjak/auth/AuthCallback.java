package com.davidmedenjak.auth;

import android.accounts.AccountManagerCallback;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

/**
 * A callback to link your app with {@link OAuthAuthenticator}. This is used to refresh your users
 * access tokens or start a login flow.
 */
public interface AuthCallback {

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
     * Re-authenticate the user with the previously stored refresh token. Return the new refresh
     * token or throw an exception if an error occurs.
     *
     * @param refreshToken the refresh token stored from {@link TokenPair#refreshToken} at the time
     *     of the last login or refresh
     * @throws IOException when there is an error refreshing the token
     * @return the new TokenPair to use for future authentication
     */
    TokenPair authenticate(@NonNull final String refreshToken) throws IOException;
}
