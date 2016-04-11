package com.digitallifelab.environmentmonitor.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class ImagesAdapter extends ArrayAdapter<PicturesStore> {

    private Context context;
    private final ArrayList<PicturesStore> picturesList;
    private int layoutId;

    public ImagesAdapter(Context context, ArrayList<PicturesStore> objects, int resourceLayoutId) {
        super(context, resourceLayoutId, objects);

        this.layoutId           = resourceLayoutId;
        this.context            = context;
        this.picturesList       = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position
        PicturesStore reviewItem = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(layoutId, parent, false);
        }

        ImageView ivImage = (ImageView) convertView.findViewById(R.id.id_image_icon);

        Picasso.with(context).load(reviewItem.getFull_photo_url())
                .placeholder(R.drawable.ic_photo_camera_white_24dp)
                .error(R.drawable.ic_error_outline_white_48dp)
                .into(ivImage);

        return convertView;
    }

    @Override
    public int getCount() {
        return picturesList.toArray().length;
    }

    @Override
    public PicturesStore getItem(int position) {
        return picturesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
