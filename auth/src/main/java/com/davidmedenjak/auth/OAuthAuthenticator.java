package com.davidmedenjak.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

/**
 * A basic implementation of an {@link AbstractAccountAuthenticator} to support OAuth use cases,
 * where accounts get persisted with a refresh token as the {@code password}.
 *
 * <p>Token refreshes will always be done <i>once</i>. Even if multiple threads request a new access
 * token simultaneously only one thread will refresh the token via {@link
 * com.davidmedenjak.auth.AuthCallback#authenticate(String)} and propagate the result to the others.
 * This is to prevent problems with APIs that only allow one usage of refresh tokens and to reduce
 * load.
 *
 * <p><b>Usage</b>
 *
 * <p>To get started you can use {@link com.davidmedenjak.auth.manager.OAuthAccountManager
 * OAuthAccountManager} that will wrap the framework {@link AccountManager} and provide a basic tool
 * for login / logout and accessToken handling with a single account.
 *
 * @see CallbackListener
 */
@SuppressWarnings("unused")
public class OAuthAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = "OAuthAuthenticator";

    private final AuthCallback service;
    private final AccountManager accountManager;

    private boolean loggingEnabled = false;

    private HashMap<Account, FetchingAuthModel> activeLookups = new HashMap<>();

    @Inject
    public OAuthAuthenticator(Context context, AuthCallback service) {
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
            @NonNull Bundle options)
            throws NetworkErrorException {
        log(
                "addAccount for %s as %s with features %s and options %s",
                accountType,
                authTokenType,
                Arrays.toString(requiredFeatures),
                BundleUtil.toString(options));

        final int uid = options.getInt(AccountManager.KEY_CALLER_UID);
        if (isUidBlocked(uid)) {
            return createErrorBundleAccessDenied();
        }

        final Bundle bundle = new Bundle();
        final Intent intent = service.getLoginIntent();
        if (intent != null) {
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        }

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
            @NonNull final Bundle options)
            throws NetworkErrorException {
        log(
                "getAuthToken for %s as %s with options %s",
                account, authTokenType, BundleUtil.toString(options));

        final int uid = options.getInt(AccountManager.KEY_CALLER_UID);
        if (isUidBlocked(uid)) {
            return createErrorBundleAccessDenied();
        }

        if (isAnotherThreadHandlingIt(account, response)) return null;

        final String authToken = accountManager.peekAuthToken(account, authTokenType);

        if (TextUtils.isEmpty(authToken)) {
            synchronized (this) {
                // queue as well
                isAnotherThreadHandlingIt(account, response);
            }

            final String refreshToken = accountManager.getPassword(account);
            CallbackListener listener = new CallbackListener(account, authTokenType, service);
            listener.refresh(refreshToken);
        } else {
            final Bundle resultBundle = createResultBundle(account, authToken);
            returnResultToQueuedResponses(account, (r) -> r.onResult(resultBundle));
            return resultBundle;
        }

        // return result via response async
        return null;
    }

    @NonNull
    private Bundle createErrorBundleAccessDenied() {
        final Bundle result = new Bundle();
        result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_CANCELED);
        result.putString(AccountManager.KEY_ERROR_MESSAGE, "Access denied");
        return result;
    }

    private boolean isUidBlocked(int uid) {
        return uid != Process.myUid();
    }

    private synchronized boolean isAnotherThreadHandlingIt(
            Account account, @NonNull AccountAuthenticatorResponse response) {

        if (!activeLookups.containsKey(account)) {
            activeLookups.put(account, new FetchingAuthModel());
        }
        final FetchingAuthModel authModel = activeLookups.get(account);

        if (authModel.fetchingToken) {
            // another thread is already working on it, register for callback
            List<AccountAuthenticatorResponse> q = authModel.queue;
            if (q == null) {
                q = new ArrayList<>();
                authModel.queue = q;
            }
            q.add(response);
            // we return null, the result will be sent with the `response`
            return true;
        }
        // we have to fetch the token, and return the result other threads
        authModel.fetchingToken = true;
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

    private void returnResultToQueuedResponses(Account account, ResponseCallback callback) {
        for (; ; ) {
            List<AccountAuthenticatorResponse> q;
            synchronized (this) {
                final FetchingAuthModel authModel = activeLookups.get(account);
                q = authModel.queue;
                if (q == null) {
                    authModel.fetchingToken = false;
                    return;
                }
                authModel.queue = null;
            }
            for (AccountAuthenticatorResponse r : q) {
                callback.returnResult(r);
            }
        }
    }

    private interface ResponseCallback {
        void returnResult(AccountAuthenticatorResponse response);
    }

    private class FetchingAuthModel {
        private boolean fetchingToken = false;
        private List<AccountAuthenticatorResponse> queue;
    }

    private class CallbackListener {

        private final Account account;
        private final String authTokenType;
        private AuthCallback service;

        private CallbackListener(Account account, String authTokenType, AuthCallback service) {
            this.account = account;
            this.authTokenType = authTokenType;
            this.service = service;
        }

        private void refresh(String refreshToken) {
            try {
                TokenPair result = service.authenticate(refreshToken);
                onAuthenticated(result);
            } catch (Exception e) {
                onError(e);
            }
        }

        private void onAuthenticated(@NonNull TokenPair tokenPair) {
            accountManager.setPassword(account, tokenPair.refreshToken);
            accountManager.setAuthToken(account, authTokenType, tokenPair.accessToken);

            final Bundle bundle = createResultBundle(account, tokenPair.accessToken);
            returnResultToQueuedResponses(account, (r) -> r.onResult(bundle));
        }

        private void onError(@NonNull Throwable error) {
            int code = AccountManager.ERROR_CODE_NETWORK_ERROR;
            returnResultToQueuedResponses(account, (r) -> r.onError(code, error.getMessage()));
        }
    }
}
