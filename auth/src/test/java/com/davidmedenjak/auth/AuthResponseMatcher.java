package com.davidmedenjak.auth;

import android.accounts.AccountManager;
import android.os.Bundle;

import org.mockito.ArgumentMatcher;

class AuthResponseMatcher implements ArgumentMatcher<Bundle> {
    private String accessToken;

    public AuthResponseMatcher(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public boolean matches(Bundle argument) {
        String token = argument.getString(AccountManager.KEY_AUTHTOKEN, null);
        //noinspection StringEquality
        return token == accessToken || accessToken.equals(token);
    }
}
