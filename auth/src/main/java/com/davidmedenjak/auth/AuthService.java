package com.davidmedenjak.auth;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A service to link your app with {@link OAuthAuthenticator}. Once one or multiple users are logged
 * in this will be used to refresh access tokens when they get invalidated.
 *
 * <pre>{@code
 * private static class MyAuthService implements AuthService {
 *     private Context context;
 *     private MyAuthApi myAuthApi;
 *     @Override
 *     public Intent getLoginIntent() {
 *         return new Intent(context, LoginActivity.class);
 *     }
 *     @Override
 *     public void authenticate(
 *             @NonNull String refreshToken,
 *             @NonNull Callback callback) {
 *         myAuthApi.authenticate("refresh_token", refreshToken)
 *             .map((it) -> new TokenPair(it.accessToken, it.refreshToken))
 *             .subscribe(callback::onAuthenticated, callback::onError);
 *     }
 * }
 * }</pre>
 */
public interface AuthService {

    /**
     * Fetch an Intent to start your Login flow. This is used in the case that a user selects `Add
     * Account` in the Account Settings. If `null` nothing will happen.
     *
     * @return e.g. new Intent(context, LoginActivity.class);
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
