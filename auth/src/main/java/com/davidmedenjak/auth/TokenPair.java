package com.davidmedenjak.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class TokenPair {

    @NonNull public final String accessToken;
    @Nullable public final String refreshToken;

    public TokenPair(@NonNull String accessToken, @Nullable String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
