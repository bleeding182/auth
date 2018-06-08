package com.davidmedenjak.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 * A basic OAuth account manager wrapper that lets you login/logout a single user and store String
 * values.
 *
 * <p>You need to provide a {@link AuthService} to link your app and api.
 *
 * @see AuthService
 */
@SuppressWarnings("unused")
public class OAuthAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = "OAuthAuthenticator";

    private final AuthService service;
    private final AccountManager accountManager;

    private boolean loggingEnabled = false;

    private boolean fetchingToken;
    private List<AccountAuthenticatorResponse> queue = null;

    @Inject
    public OAuthAuthenticator(Context context, AuthService service) {
        super(context);
        this.service = service;
        this.accountManager = AccountManager.get(context);
    }

    @Override
    public Bundle editProperties(
            @NonNull AccountAuthenticatorResponse response, @NonNull String accountType) {
        log("editProperties for %s", accountType);
        return null;
    }

    @Override
    public Bundle addAccount(
            @NonNull AccountAuthenticatorResponse response,
            @NonNull String accountType,
            @Nullable String authTokenType,
            @Nullable String[] requiredFeatures,
            @Nullable Bundle options)
            throws NetworkErrorException {
        log(
                "addAccount for %s as %s with features %s and options %s",
                accountType,
                authTokenType,
                Arrays.toString(requiredFeatures),
                BundleUtil.toString(options));

        final Intent intent = service.getLoginIntent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle confirmCredentials(
            @NonNull AccountAuthenticatorResponse response,
            @NonNull Account account,
            @Nullable Bundle options)
            throws NetworkErrorException {
        log("confirmCredentials for %s with options %s", account, BundleUtil.toString(options));
        return null;
    }

    @Override
    public Bundle getAuthToken(
            @NonNull final AccountAuthenticatorResponse response,
            @NonNull final Account account,
            @NonNull final String authTokenType,
            @Nullable final Bundle options)
            throws NetworkErrorException {
        log(
                "getAuthToken for %s as %s with options %s",
                account, authTokenType, BundleUtil.toString(options));

        if (isAnotherThreadHandlingIt(response)) return null;

        final String authToken = accountManager.peekAuthToken(account, authTokenType);

        if (TextUtils.isEmpty(authToken)) {
            synchronized (this) {
                // queue as well
                isAnotherThreadHandlingIt(response);
            }

            final String refreshToken = accountManager.getPassword(account);
            service.authenticate(refreshToken, new AuthCallback(account, authTokenType));
        } else {
            final Bundle resultBundle = createResultBundle(account, authToken);
            returnResultToQueuedResponses((r) -> r.onResult(resultBundle));
            return resultBundle;
        }

        // return result via response async
        return null;
    }

    private synchronized boolean isAnotherThreadHandlingIt(
            @NonNull AccountAuthenticatorResponse response) {
        if (fetchingToken) {
            // another thread is already working on it, register for callback
            List<AccountAuthenticatorResponse> q = queue;
            if (q == null) {
                q = new ArrayList<>();
                queue = q;
            }
            q.add(response);
            // we return null, the result will be sent with the `response`
            return true;
        }
        // we have to fetch the token, and return the result other threads
        fetchingToken = true;
        return false;
    }

    @NonNull
    private Bundle createResultBundle(@NonNull Account account, String authToken) {
        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        return result;
    }

    @Override
    public String getAuthTokenLabel(@NonNull String authTokenType) {
        log("getAuthTokenLabel for %s", authTokenType);
        return authTokenType;
    }

    @Override
    public Bundle updateCredentials(
            @NonNull AccountAuthenticatorResponse response,
            @NonNull Account account,
            @Nullable String authTokenType,
            @Nullable Bundle options)
            throws NetworkErrorException {
        log(
                "updateCredentials for %s as %s with options %s",
                account, authTokenType, BundleUtil.toString(options));
        return null;
    }

    @Override
    public Bundle hasFeatures(
            @NonNull AccountAuthenticatorResponse response,
            @NonNull Account account,
            @NonNull String[] features)
            throws NetworkErrorException {
        log("hasFeatures for %s and %s", account, Arrays.toString(features));
        return null;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    private void log(String format, Object... args) {
        if (loggingEnabled) {
            Log.d(TAG, String.format(format, args));
        }
    }

    private void returnResultToQueuedResponses(ResponseCallback callback) {
        for (; ; ) {
            List<AccountAuthenticatorResponse> q;
            synchronized (this) {
                q = queue;
                if (q == null) {
                    fetchingToken = false;
                    return;
                }
                queue = null;
            }
            for (AccountAuthenticatorResponse r : q) {
                callback.returnResult(r);
            }
        }
    }

    private interface ResponseCallback {
        void returnResult(AccountAuthenticatorResponse response);
    }

    private class AuthCallback implements AuthService.Callback {

        private final Account account;
        private final String authTokenType;

        private AuthCallback(Account account, String authTokenType) {
            this.account = account;
            this.authTokenType = authTokenType;
        }

        @Override
        public void onAuthenticated(@NonNull TokenPair tokenPair) {
            accountManager.setPassword(account, tokenPair.refreshToken);
            accountManager.setAuthToken(account, authTokenType, tokenPair.accessToken);

            final Bundle bundle = createResultBundle(account, tokenPair.accessToken);
            returnResultToQueuedResponses((r) -> r.onResult(bundle));
        }

        @Override
        public void onError(@NonNull Throwable error) {
            int code = AccountManager.ERROR_CODE_NETWORK_ERROR;
            returnResultToQueuedResponses((r) -> r.onError(code, error.getMessage()));
        }
    }
}
