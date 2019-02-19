package com.davidmedenjak.redditsample.app;

import android.app.Application;

import com.davidmedenjak.auth.manager.OAuthAccountManager;
import com.davidmedenjak.redditsample.auth.api.RedditAuthApi;
import com.davidmedenjak.redditsample.networking.RedditApi;

public class App extends Application {

    private OAuthAccountManager accountManager;
    private HttpModule httpModule;

    @Override
    public void onCreate() {
        super.onCreate();
        this.accountManager = OAuthAccountManager.fromContext(this);
        httpModule = new HttpModule(this);
    }

    public OAuthAccountManager getAccountManager() {
        return accountManager;
    }

    public RedditAuthApi getAuthApiService() {
        return httpModule.getAuthApiService();
    }

    public RedditApi getApiService() {
        return httpModule.getApiService();
    }
}
