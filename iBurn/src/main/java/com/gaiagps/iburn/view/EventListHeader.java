package com.gaiagps.iburn.view;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A ListView header presenting filter options for day and type
 * <p>
 * Clients register for feedback with
 * {@link #setReceiver(com.gaiagps.iburn.view.EventListHeader.PlayaListViewHeaderReceiver)}
 * <p>
 * Created by davidbrodsky on 8/2/14.
 */
public class EventListHeader extends RelativeLayout {

    protected TextView mTypeFilter;
    protected TextView mDayFilter;
    protected ToggleButton mExpiredFilter;
    protected ToggleButton mTimingFilter;

    protected boolean mIncludeExpiredSelection = false;
    protected String mTimingSelection = "timed";
    protected String mDaySelection = AdapterUtils.getCurrentOrFirstDayAbbreviation();
    protected ArrayList<String> mTypeSelection = new ArrayList<>();
    protected int mDaySelectionIndex =
            AdapterUtils.sDayAbbreviations.indexOf(mDaySelection);
    protected boolean[] mTypeSelectionIndexes = new boolean[AdapterUtils.getEventTypeCount()];


    protected PlayaListViewHeaderReceiver mReceiver;

    public EventListHeader(Context context) {
        super(context);
        init(context);
    }

    public EventListHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EventListHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * Interface for users to receive feedback from this view
     */
    public interface PlayaListViewHeaderReceiver {
        void onSelectionChanged(String day, ArrayList<String> types,
                                boolean includeExpired,
                                String eventTiming);
    }

    /**
     * Click listener for Sort buttons
     */
    protected OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (!v.isSelected()) {
                v.setSelected(true);
                if (v.getTag().equals("type")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.Theme_Iburn_Dialog);
                    builder.setTitle(getContext().getString(R.string.filter_by_type));
                    final java.util.List<String> typeNames = AdapterUtils.getEventTypeNames();
                    final java.util.List<String> typeAbbrevs = AdapterUtils.getEventTypeAbbreviations();
                    builder.setMultiChoiceItems(typeNames.toArray(new CharSequence[0]),
                            mTypeSelectionIndexes,
                            (dialog, which, isChecked) -> {
                                String selection = typeAbbrevs.get(which);
                                String tabTitle;
                                if (isChecked) {
                                    mTypeSelectionIndexes[which] = true;
                                    mTypeSelection.add(selection);
                                    tabTitle = typeNames.get(which);
                                } else {
                                    mTypeSelectionIndexes[which] = false;
                                    mTypeSelection.remove(selection);
                                    tabTitle = (mTypeSelection.isEmpty()) ? getResources().getString(R.string.any_type)
                                            : typeNames.get(typeAbbrevs.indexOf(mTypeSelection.get(mTypeSelection.size() - 1)));
                                }

                                if (mTypeSelection.size() > 1)
                                    tabTitle += "+" + String.valueOf(mTypeSelection.size() - 1);
                                ((TextView) v).setText(tabTitle.toUpperCase());
                                dispatchSelection();
                            });
                    builder.setPositiveButton(getContext().getString(R.string.done), null);
                    builder.show();
                } else if (v.getTag().equals("day")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.Theme_Iburn_Dialog);
                    builder.setTitle(getContext().getString(R.string.filter_by_day));
                    builder.setSingleChoiceItems(
                            AdapterUtils.sDayNames.toArray(
                                    new CharSequence[AdapterUtils.sDayNames.size()]),
                            mDaySelectionIndex,
                            (dialog, which) -> {
                                mDaySelectionIndex = which;
                                CharSequence selection = AdapterUtils.sDayAbbreviations.toArray(
                                        new CharSequence[AdapterUtils.sDayAbbreviations.size()])[which];
                                mDaySelection = (selection == null) ? null : selection.toString();
                                String tabTitle = (selection == null) ?
                                        getResources().getString(R.string.any_day) :
                                        AdapterUtils.sDayNames.toArray(
                                                new CharSequence[AdapterUtils.sDayNames.size()])[which].toString();
                                ((TextView) v).setText(tabTitle.toUpperCase());
                                dispatchSelection();
                                dialog.dismiss();
                            }
                    );
                    builder.setPositiveButton("Cancel", null);
                    builder.show();
                } else if (v.getTag().equals("expired")) {
                    mIncludeExpiredSelection = ((ToggleButton) v).isChecked();
                    dispatchSelection();
                } else if (v.getTag().equals("timing")) {
                    if(((ToggleButton) v).isChecked()){
                        mTimingSelection = "all-day";
                    }
                    else{
                        mTimingSelection = "timed";
                    }
                    dispatchSelection();
                }
                v.setSelected(false);
            }
        }
    };

    protected void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.list_view_header_two, this, false);
        mExpiredFilter = (ToggleButton) v.findViewById(R.id.expiredFilter);
        mTimingFilter = (ToggleButton) v.findViewById(R.id.timingFilter);
        mTypeFilter = (TextView) v.findViewById(R.id.typeFilter);
        mDayFilter = (TextView) v.findViewById(R.id.dateFilter);
        mDayFilter.setText(AdapterUtils.sDayNames.get(
                AdapterUtils.sDayAbbreviations.indexOf(
                        AdapterUtils.getCurrentOrFirstDayAbbreviation())).toUpperCase());
        setupTouchListeners();
        addView(v);
    }

    public void setReceiver(PlayaListViewHeaderReceiver receiver) {
        mReceiver = receiver;
    }

    protected void setupTouchListeners() {
        mTypeFilter.setTag("type");
        mDayFilter.setTag("day");
        mExpiredFilter.setTag("expired");
        mTimingFilter.setTag("timing");
        mTypeFilter.setOnClickListener(mOnClickListener);
        mDayFilter.setOnClickListener(mOnClickListener);
        mExpiredFilter.setOnClickListener(mOnClickListener);
        mTimingFilter.setOnClickListener(mOnClickListener);
    }

    protected void dispatchSelection() {
        if (mReceiver != null) {
            mReceiver.onSelectionChanged(mDaySelection, mTypeSelection,
                    mIncludeExpiredSelection,
                    mTimingSelection);
        }
    }

}
