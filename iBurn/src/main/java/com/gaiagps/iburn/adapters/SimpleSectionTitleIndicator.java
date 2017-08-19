package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.util.AttributeSet;

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