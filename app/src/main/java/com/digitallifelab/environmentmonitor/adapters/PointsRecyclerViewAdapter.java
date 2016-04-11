package com.digitallifelab.environmentmonitor.adapters;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.digitallifelab.environmentmonitor.Data.DatabaseHelper;
import com.digitallifelab.environmentmonitor.Data.EnvironmentMonitorContract;
import com.digitallifelab.environmentmonitor.Data.PicturesStore;
import com.digitallifelab.environmentmonitor.Data.PointsStore;
import com.digitallifelab.environmentmonitor.R;
import com.digitallifelab.environmentmonitor.Utils.DbInstance;
import com.digitallifelab.environmentmonitor.Utils.Utility;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


public class PointsRecyclerViewAdapter extends OrmliteCursorRecyclerViewAdapter<PointsStore, PointsRecyclerViewAdapter.PointsViewHolder> {

    final private PointsAdapterOnClickHandler mClickHandler;
    final private View mEmptyView;

    private DbInstance                                  dbInstance;
    private SelectArg                                   selectArg;
    private PreparedQuery<PicturesStore>                preparedQuery;
    private RuntimeExceptionDao<PicturesStore, Long>    picturesDao = null;
    private QueryBuilder<PicturesStore, Long>           qb;

    private double longitude, latitude;

    public String units;
    public String units_name;

    public class PointsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView ivImageOfPoint;
        public TextView tvHeadline, tvDescription, tvCreateDateTime, tvAuthor, tvCreateTime, tvDistance;

        public PointsViewHolder(View itemView) {
            super(itemView);

            ivImageOfPoint   = (ImageView) itemView.findViewById(R.id.id_point_list_item_icon);

            tvHeadline              = (TextView) itemView.findViewById(R.id.id_point_list_item_headline);
            tvDescription           = (TextView) itemView.findViewById(R.id.id_list_item_part_article_textview);
            tvCreateDateTime        = (TextView) itemView.findViewById(R.id.id_list_item_date_textview);
            tvCreateTime            = (TextView) itemView.findViewById(R.id.id_list_item_time_textview);
            tvAuthor                = (TextView) itemView.findViewById(R.id.idAuthor);
            tvDistance              = (TextView) itemView.findViewById(R.id.id_distance_text_view);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();

            getCursor().moveToPosition(adapterPosition);
            int dateColumnIndex = getCursor().getColumnIndex(EnvironmentMonitorContract.Points.LOCAL_ID);
            mClickHandler.onClick(getCursor().getLong(dateColumnIndex), this);
        }
    }

    public void SetCoordinates(double latitude, double longitude){
        this.longitude  = longitude;
        this.latitude   = latitude;
    }

    public void setUnits(){

        SharedPreferences mPreference = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultValue = context.getResources().getString(R.string.default_units_prefs_value);
        units = mPreference.getString("units_prefs", defaultValue);

        if(units.equals("K")){
            units_name = "km";
        }else{
            units_name = "m";
        }
    }

    public static interface PointsAdapterOnClickHandler {
        void onClick(Long pId, PointsViewHolder vh);
    }

    public PointsRecyclerViewAdapter(Context context, PointsAdapterOnClickHandler dh, View emptyView, DbInstance dbInstance, double longitude, double latitude) {
        super(context);

        this.dbInstance = dbInstance;

        DatabaseHelper helper = this.dbInstance.getDatabaseHelper();

        if(helper != null) {

            selectArg = new SelectArg();

            picturesDao = helper.getPicturesDataDao();
            qb = picturesDao.queryBuilder();

            try {

                qb.where().eq(PicturesStore.POINT_ID, selectArg)
                        .and()
                        .eq(PicturesStore.IS_DELETED, 0);

                preparedQuery = qb.prepare();

            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

        mClickHandler   = dh;
        mEmptyView      = emptyView;
        this.longitude  = longitude;
        this.latitude   = latitude;

        setUnits();
    }

    @Override
    public PointsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        PointsViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View vElementItem = inflater.inflate(R.layout.point_list_item, parent, false);
        viewHolder = new PointsViewHolder(vElementItem);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(PointsViewHolder holder, PointsStore pointOfInterest) {

        holder.ivImageOfPoint.setImageResource(R.drawable.ic_photo_camera_white_24dp);

        if(picturesDao != null) {

                selectArg.setValue(pointOfInterest.getLocal_id());
                List<PicturesStore> list = picturesDao.query(preparedQuery);

                String path_to_image = "";

                if (list != null && list.size() > 0) {

                    if (list.get(0).getPhoto_was_uploaded() == 0) {
                        path_to_image = Uri.fromFile(new File(list.get(0).getLocal_photo_path())).toString();
                    } else {
                        path_to_image = list.get(0).getFull_photo_url();
                    }

                    Picasso.with(context)
                            .load(path_to_image)
                            .placeholder(R.drawable.ic_photo_camera_white_24dp)
                            .error(R.drawable.ic_error_outline_white_48dp)
                            .into(holder.ivImageOfPoint);
                }else{
                    holder.ivImageOfPoint.setImageResource(R.drawable.ic_photo_camera_white_24dp);
                }
        }

        holder.tvHeadline.setText(pointOfInterest.getHeadline());
        holder.tvDescription.setText(pointOfInterest.getFull_description());

        holder.tvCreateDateTime.setText(Utility.getFriendlyDayString(context, pointOfInterest.getCreated_at(), false));

        holder.tvAuthor.setText(pointOfInterest.getFirst_name() + " " + pointOfInterest.getLast_name());

        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("HH:mm");
        holder.tvCreateTime.setText(shortenedDateFormat.format(pointOfInterest.getCreated_at()));

        if(latitude > 0 && longitude > 0 ) {
            double dist = Utility.distance(latitude, longitude, pointOfInterest.getLatitude(), pointOfInterest.getLongitude(), units);
            holder.tvDistance.setText(String.format("%1$3.2s %2$s", dist, units_name));
        }else{
            holder.tvDistance.setText(String.format("%1$3.2s %2$s", 0, units_name));
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor cur = super.swapCursor(newCursor);
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
        return cur;
    }
}