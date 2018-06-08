package com.davidmedenjak.redditsample.auth;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.davidmedenjak.auth.AuthService;
import com.davidmedenjak.auth.AuthenticatorService;
import com.davidmedenjak.auth.TokenPair;
import com.davidmedenjak.redditsample.BuildConfig;
import com.davidmedenjak.redditsample.auth.api.RedditAuthApi;
import com.davidmedenjak.redditsample.auth.login.LoginActivity;

import java.nio.charset.Charset;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class RedditAuthenticatorService extends AuthenticatorService {

    @NonNull
    private static Retrofit createRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public AuthService getAuthenticatorService() {
        final Retrofit retrofit = createRetrofit("https://www.reddit.com/api/");
        final RedditAuthApi service = retrofit.create(RedditAuthApi.class);

        /*
         * We have to construct a `AuthService` that lets the Authenticator refresh expired tokens.
         */
        return new RedditAuthService(this, service);
    }

    /** An AuthService that refreshes a users token at the reddit API. */
    private static class RedditAuthService implements AuthService {
        private static final String CLIENT_ID = BuildConfig.REDDIT_API_CLIENT_ID;

        private final RedditAuthApi service;
        private final Context context;

        public RedditAuthService(Context context, RedditAuthApi service) {
            this.context = context;
            this.service = service;
        }

        @NonNull
        private static String getBasicAuthForClientId() {
            byte[] basicAuthBytes = (CLIENT_ID + ":").getBytes();
            byte[] encodedAuthBytes = Base64.encode(basicAuthBytes, Base64.NO_WRAP);
            String clientAuth = new String(encodedAuthBytes, Charset.forName("UTF-8"));
            return "Basic " + clientAuth;
        }

        @Override
        public Intent getLoginIntent() {
            return new Intent(context, LoginActivity.class);
        }

        @Override
        public void authenticate(@NonNull String refreshToken, @NonNull Callback callback) {
            service.authenticate(getBasicAuthForClientId(), "refresh_token", refreshToken)
                    .map((it) -> new TokenPair(it.accessToken, it.refreshToken))
                    .subscribe(callback::onAuthenticated, callback::onError);
        }
    }
}
