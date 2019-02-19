package com.davidmedenjak.redditsample.features.latestcomments;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.davidmedenjak.redditsample.R;
import com.davidmedenjak.redditsample.app.App;
import com.davidmedenjak.redditsample.networking.RedditApi;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LatestCommentsActivity extends AppCompatActivity {

    private static final String EXTRA_ACCOUNT = "extra_account";
    private CommentsAdapter adapter;
    private RedditApi service;

    public static Intent newIntent(Context context, Account account) {
        Intent intent = new Intent(context, LatestCommentsActivity.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        inject();

        adapter = new CommentsAdapter();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        Account account = getIntent().getParcelableExtra(EXTRA_ACCOUNT);

        service.fetchComments(account.name)
                .observeOn(AndroidSchedulers.mainThread())
                .map(r -> r.data)
                .flatMap(
                        l ->
                                Observable.fromIterable(l.children)
                                        .map(c -> c.data)
                                        .toList()
                                        .toObservable())
                .subscribe(
                        r -> adapter.setComments(r),
                        e -> {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
    }

    private void inject() {
        App app = (App) getApplication();
        service = app.getApiService();
    }
}
