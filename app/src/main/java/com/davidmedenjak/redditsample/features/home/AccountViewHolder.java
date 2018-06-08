package com.davidmedenjak.redditsample.features.home;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.davidmedenjak.redditsample.R;

class AccountViewHolder extends RecyclerView.ViewHolder {
    TextView name;
    TextView linkKarma;
    TextView commentKarma;

    public AccountViewHolder(View itemView) {
        super(itemView);
        name = itemView.findViewById(R.id.name);
        linkKarma = itemView.findViewById(R.id.link_karma);
        commentKarma = itemView.findViewById(R.id.comment_karma);
    }
}
