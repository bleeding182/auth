package com.davidmedenjak.redditsample.auth;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.davidmedenjak.auth.AuthCallback;
import com.davidmedenjak.auth.AuthenticatorService;
import com.davidmedenjak.auth.TokenPair;
import com.davidmedenjak.redditsample.BuildConfig;
import com.davidmedenjak.redditsample.app.App;
import com.davidmedenjak.redditsample.auth.api.RedditAuthApi;
import com.davidmedenjak.redditsample.auth.api.model.TokenResponse;
import com.davidmedenjak.redditsample.auth.login.LoginActivity;

import java.io.IOException;
import java.nio.charset.Charset;

import retrofit2.HttpException;
import retrofit2.Response;

public class RedditAuthenticatorService extends AuthenticatorService {

    private RedditAuthApi authApiService;

    @Override
    public void onCreate() {
        super.onCreate();
        inject();
    }

    private void inject() {
        App app = (App) getApplication();
        authApiService = app.getAuthApiService();
    }

    @Override
    public AuthCallback getAuthCallback() {
        return new RedditAuthCallback(this, authApiService);
    }

    /** A callback that refreshes a users token at the reddit API. */
    private static class RedditAuthCallback implements AuthCallback {
        private static final String CLIENT_ID = BuildConfig.REDDIT_API_CLIENT_ID;

        private final RedditAuthApi service;
        private final Context context;

        public RedditAuthCallback(Context context, RedditAuthApi service) {
            this.context = context;
            this.service = service;
        }

        @Override
        public Intent getLoginIntent() {
            return new Intent(context, LoginActivity.class);
        }

        @Override
        public TokenPair authenticate(@NonNull String refreshToken) throws IOException {
            String clientId = getBasicAuthForClientId();
            String grantType = "refresh_token";

            final Response<TokenResponse> response =
                    service.authenticate(clientId, grantType, refreshToken).execute();

            if (response.isSuccessful() && response.body() != null) {
                final TokenResponse tokenResponse = response.body();
                final String newRefreshToken =
                        tokenResponse.refreshToken != null
                                ? tokenResponse.refreshToken
                                : refreshToken;
                return new TokenPair(tokenResponse.accessToken, newRefreshToken);
            } else {
                throw new HttpException(response);
            }
        }

        @NonNull
        private static String getBasicAuthForClientId() {
            byte[] basicAuthBytes = (CLIENT_ID + ":").getBytes();
            byte[] encodedAuthBytes = Base64.encode(basicAuthBytes, Base64.NO_WRAP);
            String clientAuth = new String(encodedAuthBytes, Charset.forName("UTF-8"));
            return "Basic " + clientAuth;
        }
    }
}
