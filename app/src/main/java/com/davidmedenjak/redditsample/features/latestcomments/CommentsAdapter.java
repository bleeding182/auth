package com.davidmedenjak.redditsample.features.latestcomments;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.davidmedenjak.redditsample.R;
import com.davidmedenjak.redditsample.networking.model.Comment;

import java.util.List;

class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments;

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_comment_view, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        holder.text.setText(comment.body);

        double created_utc = comment.created_utc;
        String dateTime =
                DateUtils.formatDateTime(holder.itemView.getContext(), (long) created_utc, 0);
        holder.date.setText(dateTime);
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        public final TextView text;
        public final TextView date;

        public CommentViewHolder(View view) {
            super(view);
            text = view.findViewById(R.id.text);
            date = view.findViewById(R.id.date);
        }
    }
}
