package com.davidmedenjak.redditsample.auth.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.davidmedenjak.auth.TokenPair;
import com.davidmedenjak.auth.manager.AccountData;
import com.davidmedenjak.auth.manager.OAuthAccountManager;
import com.davidmedenjak.redditsample.app.App;
import com.davidmedenjak.redditsample.auth.api.RedditAuthApi;
import com.davidmedenjak.redditsample.auth.api.model.TokenResponse;
import com.davidmedenjak.redditsample.auth.api.model.User;
import com.davidmedenjak.redditsample.features.home.HomeActivity;

import java.util.UUID;

import io.reactivex.SingleSource;
import io.reactivex.functions.Function;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_AUTHORIZE = 12;

    private static final String ICICLE_STATE = "icicle_state";

    private OAuthAccountManager accountManager;
    private RedditAuthApi service;

    /** Random string to identify the auth flow and verify the result. */
    private String state;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inject();

        if (savedInstanceState != null) {
            state = savedInstanceState.getString(ICICLE_STATE);
        }

        if (state == null) {
            startAuthorizationFlow();
        }
    }

    private void inject() {
        App app = (App) getApplication();
        accountManager = app.getAccountManager();
        service = app.getAuthApiService();
    }

    /**
     * Finishes the login.
     *
     * <p>Here we add the user to the account manager and finish the oauth flow.
     *
     * @param user the authenticated user we get after finishing the login
     */
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

    // ---------------------------------------------------------------------------------------------
    // region >>> OAuth Login with reddit <<<
    private void startAuthorizationFlow() {
        // create and store random string to verify auth results later
        state = UUID.randomUUID().toString();
        final String authUrl = RedditOauthBuilder.createAuthUrl(state, "identity", "history");

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

    @NonNull
    private Function<TokenResponse, SingleSource<? extends Pair<TokenResponse, User>>>
            mapUserProfileToAuth(RedditAuthApi service) {
        return response ->
                service.fetchMe("Bearer " + response.accessToken)
                        .map(user -> new Pair<>(response, user));
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
            final String basicAuth = RedditOauthBuilder.getBasicAuthForClientId();

            final String redirectUri = RedditOauthBuilder.REDIRECT_URI;
            service.authenticate(basicAuth, "authorization_code", code, redirectUri)
                    .flatMap(mapUserProfileToAuth(service))
                    .subscribe(this::addAccountForUser, Throwable::printStackTrace);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_AUTHORIZE && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "onActivityResult login cancelled");
            finish();
        }
    }
    // endregion
}
