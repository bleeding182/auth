package com.davidmedenjak.auth;

import android.support.annotation.NonNull;

import com.davidmedenjak.auth.manager.OAuthAccountManager;

import java.io.IOException;

/**
 * Provides access tokens to use for network requests.
 *
 * <p>You can use {@link OAuthAccountManager} for a basic implementation.
 */
public interface AccountAuthenticator {

    /**
     * Get an access token for the current user.
     *
     * @return the access token
     * @throws IOException if there was an error retrieving the token
     */
    @NonNull
    String getAccessToken() throws IOException;

    /**
     * Get a new access token that does not match {@code invalidAccessToken}. Use this method to ask
     * for a new AccessToken after a failed HTTP request.
     *
     * @param invalidAccessToken the invalid access token previously used
     * @return a new access token != {@code invalidAccessToken}
     * @throws IOException if there was an error retrieving the token
     */
    @NonNull
    String getNewAccessToken(String invalidAccessToken) throws IOException;
}
