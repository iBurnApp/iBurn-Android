package com.gaiagps.iburn.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.gaiagps.iburn.R;

/**
 * A ListView header presenting sorting options of "All" and "Favorites". "All" is repoted as a
 * {@link com.gaiagps.iburn.view.PlayaListViewHeader.PlayaListViewHeaderReceiver.SORT.DISTANCE} sort
 *
 * Clients register for feedback with
 * {@link #setReceiver(com.gaiagps.iburn.view.PlayaFavoriteFilterListViewHeader.PlayaListViewHeaderReceiver)}
 *
 * Created by davidbrodsky on 8/2/14.
 */
public class PlayaFavoriteFilterListViewHeader extends PlayaListViewHeader {

    private PlayaListViewHeaderReceiver mReceiver;

    public PlayaFavoriteFilterListViewHeader(Context context) {
        super(context);
        init(context);
    }

    public PlayaFavoriteFilterListViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayaFavoriteFilterListViewHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    protected void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.list_view_header_two, this, false);
        mDistance = (TextView) v.findViewById(R.id.all);
        mFavorite = (TextView) v.findViewById(R.id.favorites);
        mDistance.setSelected(true);
        setupTouchListeners();
        addView(v);
    }

    @Override
    protected void setupTouchListeners() {
        mDistance.setTag(PlayaListViewHeaderReceiver.SORT.DISTANCE);
        mFavorite.setTag(PlayaListViewHeader.PlayaListViewHeaderReceiver.SORT.FAVORITE);

        mDistance.setOnClickListener(mOnClickListener);
        mFavorite.setOnClickListener(mOnClickListener);
    }

}
