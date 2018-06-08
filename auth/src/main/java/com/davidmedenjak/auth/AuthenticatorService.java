package com.davidmedenjak.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/** A basic Service implementation to use with {@link OAuthAuthenticator}. */
public abstract class AuthenticatorService extends Service {

    private static final String TAG = "AuthenticatorService";

    private AbstractAccountAuthenticator authenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        Log.v(TAG, "AuthenticatorService created");
        authenticator = new OAuthAuthenticator(this, getAuthenticatorService());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind " + intent.toString());
        return authenticator.getIBinder();
    }

    /**
     * Provide an AuthService to be used with the {@link OAuthAuthenticator}
     *
     * @return the authService
     * @see AuthService
     */
    public abstract AuthService getAuthenticatorService();
}
