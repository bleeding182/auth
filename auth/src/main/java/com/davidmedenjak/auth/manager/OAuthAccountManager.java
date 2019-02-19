package com.davidmedenjak.auth.manager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.davidmedenjak.auth.AccountAuthenticator;
import com.davidmedenjak.auth.TokenPair;

import java.io.IOException;
import java.util.Set;

/** A basic implementation to handle user login states. */
public class OAuthAccountManager implements AccountAuthenticator {

    private static final String META_DATA_ACCOUNT_TYPE = "oauth-account.type";
    private final AccountManager accountManager;

    private final String accountType;
    private Account account;

    public OAuthAccountManager(
            @NonNull String accountType, @NonNull AccountManager accountManager) {
        this.accountType = accountType;
        this.accountManager = accountManager;

        final Account[] accounts = accountManager.getAccountsByType(accountType);
        if (accounts.length > 0) {
            account = accounts[0];
        }
    }

    public static OAuthAccountManager fromContext(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        try {
            ApplicationInfo info =
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle metaData = info.metaData;
            String accountType = metaData.getString(META_DATA_ACCOUNT_TYPE);

            if (accountType == null) {
                String errorMessage =
                        String.format(
                                "`%1$s` is not set in your manifest. Please add "
                                        + "<meta-data android:name=\"%1$s\" android:value=\"your.account.type\" /> "
                                        + "to your manifest or use the constructor.",
                                META_DATA_ACCOUNT_TYPE);
                throw new IllegalArgumentException(errorMessage);
            }

            return new OAuthAccountManager(accountType, AccountManager.get(context));
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("application doesn't exist?!", e);
        }
    }

    @NonNull
    public String getAccountType() {
        return accountType;
    }

    public void login(
            @NonNull String name, @NonNull TokenPair token, @NonNull AccountData accountData) {
        account = new Account(name, accountType);

        final String refreshToken = token.refreshToken;
        if (!accountManager.addAccountExplicitly(account, refreshToken, accountData.bundle)) {
            // account already exists, update refresh token
            accountManager.setPassword(account, refreshToken);
        }

        final String accessToken = token.accessToken;
        accountManager.setAuthToken(account, TokenType.BEARER, accessToken);
    }

    public void logout() {
        if (account == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            accountManager.removeAccount(account, null, null, null);
        } else {
            accountManager.removeAccount(account, null, null);
        }
        account = null;
    }

    public boolean isLoggedIn() {
        return account != null;
    }

    @Nullable
    public Account getAccount() {
        return account;
    }

    public void setAccountData(@NonNull String key, @Nullable String value) {
        if (!isLoggedIn()) return;

        accountManager.setUserData(account, key, value);
    }

    public void setAccountData(@NonNull AccountData accountData) {
        if (!isLoggedIn()) return;

        Bundle bundle = accountData.bundle;
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            accountManager.setUserData(account, key, bundle.getString(key));
        }
    }

    @NonNull
    public String getAccountData(@NonNull String key) {
        if (!isLoggedIn()) return "";

        String data = accountManager.getUserData(account, key);

        if (data == null) return "";
        return data;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void renameAccount(@NonNull String username) {
        if (!isLoggedIn()) return;

        accountManager.renameAccount(
                account,
                username,
                future -> {
                    try {
                        account = future.getResult();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                null);
    }

    @Override
    @NonNull
    public String getAccessToken() throws IOException {
        if (!isLoggedIn()) return "";

        try {
            return accountManager.blockingGetAuthToken(account, TokenType.BEARER, false);
        } catch (OperationCanceledException | AuthenticatorException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    @NonNull
    public String getNewAccessToken(@Nullable String invalidAccessToken) throws IOException {
        if (!isLoggedIn()) return "";

        accountManager.invalidateAuthToken(account.type, invalidAccessToken);
        return getAccessToken();
    }
}
