package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.util.AttributeSet;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;
import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;

public class SimpleSectionTitleIndicator extends SectionTitleIndicator<String> {

    public SimpleSectionTitleIndicator(Context context) {
        super(context);
    }

    public SimpleSectionTitleIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SimpleSectionTitleIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setSection(String startTime) {
        setTitleText(startTime);
        //setIndicatorTextColor(colorGroup.getAsColor());
    }

}