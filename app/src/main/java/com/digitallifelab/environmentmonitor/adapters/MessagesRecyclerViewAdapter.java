package com.digitallifelab.environmentmonitor.adapters;


import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.digitallifelab.environmentmonitor.Data.AccountsStore;
import com.digitallifelab.environmentmonitor.Data.MessagesStore;
import com.digitallifelab.environmentmonitor.R;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.squareup.picasso.Picasso;


public class MessagesRecyclerViewAdapter extends OrmliteCursorRecyclerViewAdapter<MessagesStore, MessagesRecyclerViewAdapter.MessagesViewHolder> {

    final private View mEmptyView;
    AccountsStore acc;

    public class MessagesViewHolder extends RecyclerView.ViewHolder {

        public ImageView ivAvatar;
        public TextView tvMessageBody, tvAuthorName, tvCreateDate;

        public MessagesViewHolder(View itemView) {
            super(itemView);

            ivAvatar   = (ImageView) itemView.findViewById(R.id.ivAvatar);

            tvMessageBody             = (TextView) itemView.findViewById(R.id.tvMessageBody);
            tvAuthorName              = (TextView) itemView.findViewById(R.id.tvAuthorName);
            tvCreateDate              = (TextView) itemView.findViewById(R.id.tvCreateDate);
        }
    }

    public MessagesRecyclerViewAdapter(Context context, View emptyView) {
        super(context);

        acc = AccountsStore.getActiveUser();
        mEmptyView = emptyView;
    }

    @Override
    public MessagesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MessagesViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View vElementItem;
        if(viewType != 1)
            vElementItem = inflater.inflate(R.layout.message_list_item, parent, false);
        else
            vElementItem = inflater.inflate(R.layout.message_list_item_inverse, parent, false);

        viewHolder = new MessagesViewHolder(vElementItem);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MessagesViewHolder holder, MessagesStore message) {


        holder.ivAvatar.setImageResource(R.drawable.ic_account_circle_white_24dp);

        if(!message.getUser_photo_url().isEmpty()) {
            Picasso.with(context).load(message.getUser_photo_url())
                    .placeholder(R.drawable.ic_account_circle_white_24dp)
                    .error(R.drawable.ic_account_circle_white_24dp)
                    .into(holder.ivAvatar);
        }

        holder.tvAuthorName.setText(message.getFirst_name() + " " + message.getLast_name());
        holder.tvMessageBody.setText(message.getComment());
        holder.tvCreateDate.setText( Utility.MillSecToString(message.getCreated_at()));
    }

    @Override
    public int getItemViewType(int position) {

        Cursor mCursor = getCursor();
        if(mCursor != null && mCursor.getCount() > 0){
            if(mCursor.moveToPosition(position)) {

                String ownerMessage = mCursor.getString(mCursor.getColumnIndex(MessagesStore.USER_EMAIL));

                if(acc.getEmail().equals(ownerMessage))
                    return -1;
            }
        }

        return 1;
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor cur = super.swapCursor(newCursor);
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
        return cur;
    }
}