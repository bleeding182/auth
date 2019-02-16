package com.davidmedenjak.redditsample.features.latestcomments;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.davidmedenjak.auth.manager.OAuthAccountManager;
import com.davidmedenjak.auth.okhttp.RequestAuthInterceptor;
import com.davidmedenjak.auth.okhttp.RequestRetryAuthenticator;
import com.davidmedenjak.redditsample.R;
import com.davidmedenjak.redditsample.app.App;
import com.davidmedenjak.redditsample.common.BaseActivity;
import com.davidmedenjak.redditsample.networking.RedditService;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class LatestCommentsActivity extends BaseActivity {

    private static final String EXTRA_ACCOUNT = "extra_account";
    private CommentsAdapter adapter;

    public static Intent newIntent(Context context, Account account) {
        Intent intent = new Intent(context, LatestCommentsActivity.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        adapter = new CommentsAdapter();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        Account account = getIntent().getParcelableExtra(EXTRA_ACCOUNT);

        RedditService service =
                createRetrofit("https://oauth.reddit.com/api/")
                        .create(RedditService.class);

        service.fetchComments(account.name)
                .observeOn(AndroidSchedulers.mainThread())
                .map(r -> r.data)
                .flatMap(
                        l ->
                                Observable.fromIterable(l.children)
                                        .map(c -> c.data)
                                        .toList()
                                        .toObservable())
                .subscribe(r -> adapter.setComments(r));
    }

    @NonNull
    private Retrofit createRetrofit(String baseUrl) {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.setLevel(HttpLoggingInterceptor.Level.BODY);

        OAuthAccountManager authenticator = ((App) getApplication()).getAccountManager();

        final OkHttpClient okHttpClient =
                new OkHttpClient.Builder()
                        .addInterceptor(logger)
                        .authenticator(new RequestRetryAuthenticator(authenticator))
                        .addInterceptor(new RequestAuthInterceptor(authenticator))
                        .build();

        return new Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
                .baseUrl(baseUrl)
                .build();
    }
}
