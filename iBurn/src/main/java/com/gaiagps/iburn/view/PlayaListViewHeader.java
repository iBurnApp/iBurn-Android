package com.gaiagps.iburn.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterUtils;

import java.util.ArrayList;

/**
 * A ListView header presenting sorting options of "Name", "Distance", and "Favorite".
 *
 * Clients register for feedback with
 * {@link #setReceiver(com.gaiagps.iburn.view.PlayaListViewHeader.PlayaListViewHeaderReceiver)}
 *
 * Created by davidbrodsky on 8/2/14.
 */
public class PlayaListViewHeader extends RelativeLayout {

    protected TextView mDistance;
    protected TextView mFavorite;
    protected TextView mTypeFilter;
    protected TextView mDayFilter;

    protected PlayaListViewHeaderReceiver.SORT mSort = PlayaListViewHeaderReceiver.SORT.DISTANCE;
    protected String mDaySelection;
    protected ArrayList<String> mTypeSelection = new ArrayList<>();
    protected int mDaySelectionIndex;
    protected boolean[] mTypeSelectionIndexes = new boolean[100];


    private PlayaListViewHeaderReceiver mReceiver;

    public PlayaListViewHeader(Context context) {
        super(context);
        init(context);
    }

    public PlayaListViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayaListViewHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /** Interface for users to receive feedback from this view */
    public static interface PlayaListViewHeaderReceiver {
        public static enum SORT { NAME, DISTANCE, FAVORITE }

        public void onSelectionChanged(SORT sort, String day, ArrayList<String> types);
    }

    /** Click listener for Sort buttons */
    protected OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (!v.isSelected()) {
                v.setSelected(true);
                if (v.getTag() instanceof PlayaListViewHeaderReceiver.SORT) {
                    if (mDistance != null)  mDistance.setSelected(false);
                    if (mFavorite != null)  mFavorite.setSelected(false);
                    v.setSelected(true);
                    mSort = (PlayaListViewHeaderReceiver.SORT) v.getTag();
                    dispatchSelection();
                } else {
                    if (v.getTag().equals("type") ) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(getContext().getString(R.string.filter_by_type));
                        builder.setMultiChoiceItems(AdapterUtils.mEventTypeNames.toArray(new CharSequence[AdapterUtils.mEventTypeNames.size()]),
                               mTypeSelectionIndexes, new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                        CharSequence selection = AdapterUtils.mEventTypeAbbreviations.toArray(new CharSequence[AdapterUtils.mEventTypeAbbreviations.size()])[which];
                                        if (isChecked) {
                                            mTypeSelectionIndexes[which] = true;
                                            mTypeSelection.add( (selection == null) ? null : selection.toString());
                                        } else {
                                            mTypeSelectionIndexes[which] = false;
                                            mTypeSelection.remove( (selection == null) ? null : selection.toString());
                                        }
                                        String tabTitle = (selection == null) ? getResources().getString(R.string.any_type) : AdapterUtils.mEventTypeNames.toArray(new CharSequence[AdapterUtils.mEventTypeNames.size()])[which].toString();
                                        if (mTypeSelection.size() > 1) tabTitle += "+";
                                        ((TextView) v).setText(tabTitle.toUpperCase());
                                        dispatchSelection();
                                    }
                                });
                        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mTypeSelection.size() == 0) {
                                    ((TextView) v).setText(getResources().getString(R.string.any_type));
                                }
                            }
                        });
                        builder.show();
                    } else if (v.getTag().equals("day")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle(getContext().getString(R.string.filter_by_day));
                        builder.setSingleChoiceItems(AdapterUtils.mDayNames.toArray(new CharSequence[AdapterUtils.mDayNames.size()]),
                                mDaySelectionIndex,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mDaySelectionIndex = which;
                                        CharSequence selection = AdapterUtils.mDayAbbreviations.toArray(new CharSequence[AdapterUtils.mDayAbbreviations.size()])[which];
                                        mDaySelection = (selection == null) ? null : selection.toString();
                                        String tabTitle = (selection == null) ? getResources().getString(R.string.any_day) : AdapterUtils.mDayNames.toArray(new CharSequence[AdapterUtils.mDayNames.size()])[which].toString();
                                        ((TextView) v).setText(tabTitle.toUpperCase());
                                        dispatchSelection();
                                    }
                                }
                        );
                        builder.setPositiveButton("Done", null);
                        builder.show();
                    }
                    v.setSelected(false);
                }
            }
        }
    };

    protected void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.list_view_header_three, this, false);
        mDistance   = (TextView) v.findViewById(R.id.distance);
        mFavorite   = (TextView) v.findViewById(R.id.favorites);
        mTypeFilter = (TextView) v.findViewById(R.id.typeFilter);
        mDayFilter  = (TextView) v.findViewById(R.id.dateFilter);
        mDistance.setSelected(true);
        setupTouchListeners();
        addView(v);
    }

    public void setReceiver(PlayaListViewHeaderReceiver receiver) {
        mReceiver = receiver;
    }

    protected void setupTouchListeners() {
        mDistance   .setTag(PlayaListViewHeaderReceiver.SORT.DISTANCE);
        mFavorite   .setTag(PlayaListViewHeaderReceiver.SORT.FAVORITE);

        mDistance   .setOnClickListener(mOnClickListener);
        mFavorite   .setOnClickListener(mOnClickListener);

        mTypeFilter .setTag("type");
        mDayFilter  .setTag("day");

        mTypeFilter .setOnClickListener(mOnClickListener);
        mDayFilter  .setOnClickListener(mOnClickListener);
    }

    protected void dispatchSelection() {
        if (mReceiver != null) {
            mReceiver.onSelectionChanged(mSort, mDaySelection, mTypeSelection);
        }
    }

}
