package com.davidmedenjak.redditsample.auth.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.davidmedenjak.auth.manager.AccountData;
import com.davidmedenjak.auth.manager.OAuthAccountManager;
import com.davidmedenjak.auth.TokenPair;
import com.davidmedenjak.redditsample.R;
import com.davidmedenjak.redditsample.app.App;
import com.davidmedenjak.redditsample.auth.api.RedditAuthApi;
import com.davidmedenjak.redditsample.auth.api.model.TokenResponse;
import com.davidmedenjak.redditsample.auth.api.model.User;
import com.davidmedenjak.redditsample.common.BaseActivity;
import com.davidmedenjak.redditsample.features.home.HomeActivity;

import java.nio.charset.Charset;
import java.util.UUID;

import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_AUTHORIZE = 12;

    private static final String ICICLE_STATE = "icicle_state";

    private static final String CLIENT_ID = "4tVpFALOLCy1ug";
    private static final String REDIRECT_URI = "redirect://redditsample.davidmedenjak.com";

    private OAuthAccountManager accountManager;

    /** Random string to identify the auth flow and verify the result. */
    private String state;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountManager = ((App) getApplication()).getAccountManager();

        if (savedInstanceState != null) {
            state = savedInstanceState.getString(ICICLE_STATE);
        }

        if (state == null) {
            startAuthorizationFlow();
        }
    }

    private void startAuthorizationFlow() {
        // create and store random string to verify auth results later
        state = UUID.randomUUID().toString();

        String scopes = formatScopes("identity", "history");
        String authUrl =
                Uri.parse("https://www.reddit.com/api/v1/authorize.compact")
                        .buildUpon()
                        .appendQueryParameter("client_id", CLIENT_ID)
                        .appendQueryParameter("response_type", "code")
                        .appendQueryParameter("state", state)
                        .appendQueryParameter("redirect_uri", REDIRECT_URI)
                        .appendQueryParameter("scope", scopes)
                        .appendQueryParameter("duration", "permanent")
                        .toString();

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
        startActivityForResult(intent, RC_AUTHORIZE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ICICLE_STATE, state);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri query = intent.getData();

        // read the state from the redirect uri
        final String redirectState = query.getQueryParameter("state");

        if (this.state == null || !this.state.equals(redirectState)) {
            // we did not start this auth flow
            startAuthorizationFlow();
        } else {
            final String code = query.getQueryParameter("code");
            final String basicAuth = getBasicAuthForClientId();

            final RedditAuthApi service =
                    createRetrofit("https://www.reddit.com/api/").create(RedditAuthApi.class);

            service.authenticate(basicAuth, "authorization_code", code, REDIRECT_URI)
                    .flatMap(mapUserProfileToAuth(service))
                    .subscribe(this::addAccountForUser, Throwable::printStackTrace);
        }
    }

    @NonNull
    private String getBasicAuthForClientId() {
        byte[] basicAuthBytes = (CLIENT_ID + ":").getBytes();
        byte[] encodedAuthBytes = Base64.encode(basicAuthBytes, Base64.NO_WRAP);
        String clientAuth = new String(encodedAuthBytes, Charset.forName("UTF-8"));
        return "Basic " + clientAuth;
    }

    @NonNull
    private Function<TokenResponse, SingleSource<? extends Pair<TokenResponse, User>>>
            mapUserProfileToAuth(RedditAuthApi service) {
        return response ->
                service.fetchMe("Bearer " + response.accessToken)
                        .map(user -> new Pair<>(response, user));
    }

    @NonNull
    private Retrofit createRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
                .baseUrl(baseUrl)
                .build();
    }

    private void addAccountForUser(Pair<TokenResponse, User> user) {
        long commentKarma = user.second.commentKarma;
        long linkKarma = user.second.linkKarma;

        AccountData data =
                AccountData.with("comment_karma", String.valueOf(commentKarma))
                        .and("link_karma", String.valueOf(linkKarma));

        TokenPair tokenPair = new TokenPair(user.first.accessToken, user.first.refreshToken);
        accountManager.login(user.second.name, tokenPair, data);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_AUTHORIZE && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "onActivityResult login cancelled");
            finish();
        }
    }

    private String formatScopes(@Nullable String... scopes) {
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
