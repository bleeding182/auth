package com.davidmedenjak.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * A Service used to register {@link OAuthAuthenticator} with the Android framework.
 *
 * <p>You need to provide an {@link AuthCallback} that the authenticator can use for callbacks to
 * your app.
 */
public abstract class AuthenticatorService extends Service {

    private static final String TAG = "AuthenticatorService";

    private AbstractAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        Log.v(TAG, "AuthenticatorService created");
        authenticator = new OAuthAuthenticator(this, getAuthCallback());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind " + intent.toString());
        return authenticator.getIBinder();
    }

    /**
     * Provide an AuthCallback to be used with the {@link OAuthAuthenticator}
     *
     * @return the authCallback
     * @see AuthCallback
     */
    public abstract AuthCallback getAuthCallback();
}
