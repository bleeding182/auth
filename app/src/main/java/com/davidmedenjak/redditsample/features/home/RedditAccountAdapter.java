package com.davidmedenjak.redditsample.features.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.davidmedenjak.redditsample.R;

class RedditAccountAdapter extends RecyclerView.Adapter<AccountViewHolder> {

    private Account[] accounts;
    private final AccountManager accountManager;
    private AccountSelectionListener callback;

    public RedditAccountAdapter(AccountManager accountManager, AccountSelectionListener callback) {
        this.accountManager = accountManager;
        this.callback = callback;
    }

    public void updateAccounts(Account[] accounts) {
        this.accounts = accounts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_account_view, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accounts[position];
        holder.name.setText(account.name);
        long linkKarma = Long.parseLong(accountManager.getUserData(account, "link_karma"));
        long commentKarma = Long.parseLong(accountManager.getUserData(account, "comment_karma"));

        holder.itemView.setOnClickListener(__ -> callback.onAccountSelected(account));

        Context context = holder.itemView.getContext();
        holder.linkKarma.setText(context.getString(R.string.link_karma, linkKarma));
        holder.commentKarma.setText(context.getString(R.string.comment_karma, commentKarma));
    }

    @Override
    public int getItemCount() {
        return accounts != null ? accounts.length : 0;
    }

    public interface AccountSelectionListener {
        void onAccountSelected(Account account);
    }
}
