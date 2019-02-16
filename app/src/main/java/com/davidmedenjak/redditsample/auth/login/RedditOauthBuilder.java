package com.davidmedenjak.redditsample.auth.login;

import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.davidmedenjak.redditsample.BuildConfig;

import java.nio.charset.Charset;

final class RedditOauthBuilder {
    private static final String CLIENT_ID = BuildConfig.REDDIT_API_CLIENT_ID;
    static final String REDIRECT_URI = "redirect://redditsample.davidmedenjak.com";

    @NonNull
    public static String getBasicAuthForClientId() {
        byte[] basicAuthBytes = (CLIENT_ID + ":").getBytes();
        byte[] encodedAuthBytes = Base64.encode(basicAuthBytes, Base64.NO_WRAP);
        String clientAuth = new String(encodedAuthBytes, Charset.forName("UTF-8"));
        return "Basic " + clientAuth;
    }

    static String createAuthUrl(String state, String... scopes) {
        return Uri.parse("https://www.reddit.com/api/v1/authorize.compact")
                .buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("state", state)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("scope", formatScopes(scopes))
                .appendQueryParameter("duration", "permanent")
                .toString();
    }

    private static String formatScopes(@Nullable String... scopes) {
        if (scopes == null || scopes.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(scopes[0]);
        for (int i = 1; i < scopes.length; i++) {
            result.append(" ").append(scopes[i]);
        }
        return result.toString();
    }
}
