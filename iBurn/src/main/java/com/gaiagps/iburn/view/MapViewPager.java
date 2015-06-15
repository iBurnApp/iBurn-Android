package com.gaiagps.iburn.view;

/**
 * Created by davidbrodsky on 8/4/13.
 */

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.gaiagps.iburn.activity.MainActivity;

/**
 * This class intercepts touch events to prevent
 * swiping pages of the View Pager when on the map tab.
 * This class assumes the map is on the first tab.
 * See onPageScrolled(..) to alter this behavior
 *
 * @author davidbrodsky
 *
 */
public class MapViewPager extends ViewPager {
    private static final String TAG = "MapViewPager";

    private float lastX, maxX = 0;

    // threshold in pixels from screen border to allow scroll in mapview
    private static final float cutoff_threshold = 20;

    private boolean onMap = false;


    public MapViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                maxX = getWidth() - cutoff_threshold;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if(onMap){
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
//                    Log.d("PAGER-DOWN", "X: " + String.valueOf(lastX) + " MaxX: " + String.valueOf(maxX));
                    break;
                case MotionEvent.ACTION_MOVE:
//                    Log.d("PAGER-MOVE", "X: " + String.valueOf(lastX) + " MaxX: " + String.valueOf(maxX));
                    //                        Log.d("PAGER-MOVE", "interpreting touch as page right");
                    return lastX > maxX;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if(positionOffset == 0){
            onMap = (position == 0);
        }
        super.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

}