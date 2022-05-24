package com.davidmedenjak.redditsample.features.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.davidmedenjak.auth.manager.OAuthAccountManager;
import com.davidmedenjak.redditsample.R;
import com.davidmedenjak.redditsample.app.App;
import com.davidmedenjak.redditsample.auth.login.LoginActivity;
import com.davidmedenjak.redditsample.features.latestcomments.LatestCommentsActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements OnAccountsUpdateListener {

    private OAuthAccountManager oauthAccountManager;
    private AccountManager accountManager;
    private RedditAccountAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        inject();

        adapter =
                new RedditAccountAdapter(
                        accountManager,
                        account -> startActivity(LatestCommentsActivity.newIntent(this, account)));

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void inject() {
        this.accountManager = AccountManager.get(this);
        App app = (App) getApplication();
        this.oauthAccountManager = app.getAccountManager();
    }

    @Override
    protected void onStart() {
        super.onStart();
        accountManager.addOnAccountsUpdatedListener(this, null, true);

        // not logged in - start login flow
        if (!oauthAccountManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        accountManager.removeOnAccountsUpdatedListener(this);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        final List<Account> filteredAccounts = new ArrayList<>();
        final String type = getString(R.string.account_type);
        for (Account account : accounts) {
            if (account.type.equals(type)) {
                filteredAccounts.add(account);
            }
        }
        adapter.updateAccounts(filteredAccounts.toArray(new Account[0]));
    }
}
