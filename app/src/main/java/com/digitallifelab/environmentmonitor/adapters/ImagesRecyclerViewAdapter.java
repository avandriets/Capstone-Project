package com.digitallifelab.environmentmonitor.adapters;


import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class ImagesRecyclerViewAdapter extends RecyclerView.Adapter<ImagesRecyclerViewAdapter.ImageViewHolders>{

    private List<PicturesStore> itemList;
    private Context context;
    final private View mEmptyView;

    final private ImagesAdapterOnClickHandler mClickHandler;
    final private ImagesAdapterOnMenuItemClickListenerHandler mMenuClickHandler;
    final private ImagesAdapterOnCreateContextMenuHandler mCreateMenuHandler;

    public static interface ImagesAdapterOnClickHandler {
        void onClick(int position, ImageViewHolders vh);
    }

    public static interface ImagesAdapterOnMenuItemClickListenerHandler {
        void onMenuItemClick(int position, MenuItem item);
    }

    public static interface ImagesAdapterOnCreateContextMenuHandler {
        void onCreateContextMenu(int position, ImageViewHolders vh, ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo);
    }

    public ImagesRecyclerViewAdapter(Context context, List<PicturesStore> itemList, View mEmptyView
            , ImagesAdapterOnClickHandler mClickHandler
            , ImagesAdapterOnMenuItemClickListenerHandler mMenuClickHandler
            , ImagesAdapterOnCreateContextMenuHandler mCreateMenuHandler) {

        this.itemList = itemList;
        this.context = context;
        this.mEmptyView = mEmptyView;
        this.mClickHandler = mClickHandler;
        this.mMenuClickHandler = mMenuClickHandler;
        this.mCreateMenuHandler = mCreateMenuHandler;

        if(this.itemList == null || this.itemList.size() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        }else{
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public ImageViewHolders onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_list_item, null);
        ImageViewHolders rcv = new ImageViewHolders(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(ImageViewHolders holder, int position) {

        String path_to_image = "";

        if (itemList.get(position).getPhoto_was_uploaded() == 0) {
            //Uri uri = Uri.fromFile(new File(itemList.get(position).getLocal_photo_path_thumbnail()));
            //path_to_image = itemList.get(position).getLocal_photo_path();
            path_to_image = Uri.fromFile(new File(itemList.get(position).getLocal_photo_path())).toString();
        } else {
            path_to_image = itemList.get(position).getFull_photo_url();
        }

        Picasso.with(context).load(path_to_image)
                .placeholder(R.drawable.ic_photo_camera_white_24dp)
                .error(R.drawable.ic_error_outline_white_48dp)
                .into(holder.countryPhoto);

        if(this.itemList == null || this.itemList.size() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        }else{
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }

    public void ChangeItemsSource(List<PicturesStore> itemList) {
        this.itemList = itemList;
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }


    public class ImageViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener{

        public ImageView countryPhoto;

        public ImageViewHolders(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            countryPhoto = (ImageView)itemView.findViewById(R.id.id_image_icon);

            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View view) {
            int pos =getAdapterPosition();
            mClickHandler.onClick(pos, this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

            int pos =getAdapterPosition();
            mCreateMenuHandler.onCreateContextMenu(pos, this , menu, v, menuInfo);

        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            int pos =getAdapterPosition();
            mMenuClickHandler.onMenuItemClick(pos, item);

            return true;
        }
    }
}
