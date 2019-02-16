package com.davidmedenjak.redditsample.app;

import androidx.annotation.NonNull;

import com.davidmedenjak.auth.manager.OAuthAccountManager;
import com.davidmedenjak.auth.okhttp.RequestAuthInterceptor;
import com.davidmedenjak.auth.okhttp.RequestRetryAuthenticator;
import com.davidmedenjak.redditsample.auth.api.RedditAuthApi;
import com.davidmedenjak.redditsample.networking.RedditApi;
import com.squareup.moshi.Moshi;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class HttpModule {

    private final App app;

    private OkHttpClient client;
    private Moshi moshi;

    private RedditAuthApi authService;
    private RedditApi apiService;

    public HttpModule(App app) {
        this.app = app;
    }

    @NonNull
    public OkHttpClient provideOkHttp() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
                    logger.setLevel(HttpLoggingInterceptor.Level.BODY);
                    client = new OkHttpClient.Builder().addNetworkInterceptor(logger).build();
                }
            }
        }
        return client;
    }

    @NonNull
    public Moshi provideMoshi() {
        if (moshi == null) {
            synchronized (this) {
                if (moshi == null) {
                    moshi = new Moshi.Builder().build();
                }
            }
        }
        return moshi;
    }

    // we need 2 different api services - one for login & authentication (that doesn't try to add
    // `Authorization` headers) and one for our authenticated calls.

    // adding the interceptors to the auth api service as well would result in a deadlock as they
    // would
    // try to fetch an access token while fetching an access token.

    public RedditAuthApi getAuthApiService() {
        if (authService == null) {
            synchronized (this) {
                if (authService == null) {
                    MoshiConverterFactory converterFactory =
                            MoshiConverterFactory.create(provideMoshi());
                    authService =
                            new Retrofit.Builder()
                                    .client(provideOkHttp())
                                    .addConverterFactory(converterFactory)
                                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
                                    .baseUrl("https://www.reddit.com/api/")
                                    .build()
                                    .create(RedditAuthApi.class);
                }
            }
        }
        return authService;
    }

    public RedditApi getApiService() {
        if (apiService == null) {
            synchronized (this) {
                if (apiService == null) {
                    OAuthAccountManager authenticator = app.getAccountManager();
                    final OkHttpClient okHttpClient =
                            provideOkHttp()
                                    .newBuilder()
                                    // add authenticators only here to prevent deadlocks when
                                    // (re-)authenticating
                                    .authenticator(new RequestRetryAuthenticator(authenticator))
                                    .addInterceptor(new RequestAuthInterceptor(authenticator))
                                    .build();
                    MoshiConverterFactory converterFactory =
                            MoshiConverterFactory.create(provideMoshi());
                    apiService =
                            new Retrofit.Builder()
                                    .client(okHttpClient)
                                    .addConverterFactory(converterFactory)
                                    .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
                                    .baseUrl("https://oauth.reddit.com/api/")
                                    .build()
                                    .create(RedditApi.class);
                }
            }
        }
        return apiService;
    }
}
