package com.davidmedenjak.redditsample.app;

import android.app.Application;

import com.davidmedenjak.auth.manager.OAuthAccountManager;

public class App extends Application {

    private OAuthAccountManager accountManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // register the util to remove splash screen after loading
        registerActivityLifecycleCallbacks(new SplashScreenHelper());

        this.accountManager = OAuthAccountManager.fromContext(this);
    }

    public OAuthAccountManager getAccountManager() {
        return accountManager;
    }
}
