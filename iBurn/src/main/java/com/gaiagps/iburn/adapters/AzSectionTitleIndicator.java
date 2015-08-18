package com.gaiagps.iburn.adapters;

import android.content.Context;
import android.util.AttributeSet;

import java.util.Arrays;
import java.util.List;

import xyz.danoz.recyclerviewfastscroller.sectionindicator.title.SectionTitleIndicator;

public class AzSectionTitleIndicator extends SectionTitleIndicator<String> {

    public static String[] sections = new String[] { "!", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    public static  List<String> sectionsList = Arrays.asList(sections);

    public static int getSectionIndexForName(String title) {
        int result = Math.max(0, AzSectionTitleIndicator.sectionsList.indexOf(title.toUpperCase().substring(0, 1)));
        //Timber.d("Got section index %d for %s", result, title.toLowerCase().substring(0, 1));
        return result;
    }

    public AzSectionTitleIndicator(Context context) {
        super(context);
    }

    public AzSectionTitleIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AzSectionTitleIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setSection(String leadingChar) {
        setTitleText(leadingChar);
        //setIndicatorTextColor(colorGroup.getAsColor());
    }

}