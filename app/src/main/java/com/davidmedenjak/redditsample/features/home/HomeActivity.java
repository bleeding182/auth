package com.davidmedenjak.redditsample.features.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.davidmedenjak.redditsample.R;
import com.davidmedenjak.redditsample.auth.login.LoginActivity;
import com.davidmedenjak.redditsample.common.BaseActivity;
import com.davidmedenjak.redditsample.features.latestcomments.LatestCommentsActivity;

public class HomeActivity extends BaseActivity implements OnAccountsUpdateListener {

    private AccountManager accountManager;
    private RedditAccountAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.accountManager = AccountManager.get(this);

        setContentView(R.layout.activity_home);

        adapter =
                new RedditAccountAdapter(
                        accountManager,
                        account -> startActivity(LatestCommentsActivity.newIntent(this, account)));

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        accountManager.addOnAccountsUpdatedListener(this, null, true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        accountManager.removeOnAccountsUpdatedListener(this);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        if(accounts.length == 0) {
            startActivity(new Intent(this, LoginActivity.class));
        }
        adapter.updateAccounts(accounts);
    }
}
