package com.gaiagps.iburn.activity;

import android.content.ContentValues;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.UserPoiTable;
import com.gaiagps.iburn.fragment.WelcomeFragment;

public class WelcomeActivity extends AppCompatActivity implements WelcomeFragment.HomeCampSelectionListener {
    static final int NUM_PAGES = 4;

    private PrefsHelper prefs;

    private CampSelection homeCampSelection;

    private ViewPager pager;
    private PagerAdapter pagerAdapter;
    private LinearLayout circles;
    private Button skip;
    private Button done;
    private ImageButton next;
    private boolean isOpaque = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        setContentView(R.layout.activity_welcome);
        skip = Button.class.cast(findViewById(R.id.skip));
        skip.setOnClickListener(v -> endTutorial());

        next = ImageButton.class.cast(findViewById(R.id.next));
        next.setOnClickListener(v -> pager.setCurrentItem(pager.getCurrentItem() + 1, true));

        done = Button.class.cast(findViewById(R.id.done));
        done.setOnClickListener(v -> endTutorial());

        pager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setPageTransformer(true, new CrossfadePageTransformer());

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                if (position == NUM_PAGES - 2 && positionOffset > 0) {
                    if (isOpaque) {
                        pager.setBackgroundColor(Color.TRANSPARENT);
                        isOpaque = false;
                    }
                } else {
                    if (!isOpaque) {
                        pager.setBackgroundColor(getResources().getColor(R.color.off_blk_2));
                        isOpaque = true;
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                setIndicator(position);
                if (position == NUM_PAGES - 2) {
                    skip.setVisibility(View.GONE);
                    next.setVisibility(View.GONE);
                    done.setVisibility(View.VISIBLE);
                } else if (position < NUM_PAGES - 2) {
                    skip.setVisibility(View.VISIBLE);
                    next.setVisibility(View.VISIBLE);
                    done.setVisibility(View.GONE);
                } else if (position == NUM_PAGES - 1) {
                    endTutorial();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        buildCircles();

        prefs = new PrefsHelper(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pager != null) {
            pager.clearOnPageChangeListeners();
        }
    }

    private void buildCircles() {
        circles = LinearLayout.class.cast(findViewById(R.id.circles));

        float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (5 * scale + 0.5f);

        for (int i = 0; i < NUM_PAGES - 1; i++) {
            ImageView circle = new ImageView(this);
            circle.setImageResource(R.drawable.ic_swipe_indicator_white_18dp);
            circle.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            circle.setAdjustViewBounds(true);
            circle.setPadding(padding, 0, padding, 0);
            circles.addView(circle);
        }

        setIndicator(0);
    }

    private void setIndicator(int index) {
        if (index < NUM_PAGES) {
            for (int i = 0; i < NUM_PAGES - 1; i++) {
                ImageView circle = (ImageView) circles.getChildAt(i);
                if (i == index) {
                    circle.setColorFilter(getResources().getColor(R.color.iburn_color));
                } else {
                    circle.setColorFilter(getResources().getColor(android.R.color.transparent));
                }
            }
        }
    }

    public void endTutorial() {

        if (homeCampSelection != null) {
            ContentValues poiValues = new ContentValues();
            poiValues.put(UserPoiTable.name, homeCampSelection.name);
            poiValues.put(UserPoiTable.latitude, homeCampSelection.lat);
            poiValues.put(UserPoiTable.longitude, homeCampSelection.lon);
            poiValues.put(UserPoiTable.drawableResId, UserPoiTable.HOME);
            DataProvider.getInstance(this)
                    .map(dataProvider -> dataProvider.insert(PlayaDatabase.POIS, poiValues))
                    .subscribe();
        }

        prefs.setDidShowWelcome(true);
        finish();
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            pager.setCurrentItem(pager.getCurrentItem() - 1);
        }
    }

    @Override
    public void onHomeCampSelected(CampSelection selection) {
        homeCampSelection = selection;
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            WelcomeFragment tp = null;
            switch (position) {
                case 0:
                    tp = WelcomeFragment.newInstance(R.layout.welcome_fragment1);
                    break;
                case 1:
                    tp = WelcomeFragment.newInstance(R.layout.welcome_fragment2);
                    break;
                case 2:
                    tp = WelcomeFragment.newInstance(R.layout.welcome_fragment3);
                    break;
                case 3:
                    tp = WelcomeFragment.newInstance(R.layout.welcome_fragment4);
                    break;
            }

            return tp;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    public class CrossfadePageTransformer implements ViewPager.PageTransformer {

        @Override
        public void transformPage(View page, float position) {
            int pageWidth = page.getWidth();

            View brcOutline = page.findViewById(R.id.a000);
            View welcomeHeader = page.findViewById(R.id.welcomeHeader);
            View video = page.findViewById(R.id.video);
            View welcome2 = page.findViewById(R.id.welcome_fragment2);
            View bodyContent = page.findViewById(R.id.bodyContent);

            if (position <= -1.0f || position >= 1.0f) {
                // do nothing
            } else if (position == 0.0f) {
                // do nothing
            } else {
                if (video != null) {
                    video.setAlpha(1.0f - Math.abs(position));
                    page.setTranslationX(pageWidth * -position);
                }

                if (welcomeHeader != null) {
                    welcomeHeader.setAlpha(1.0f - Math.abs(position));
                    welcomeHeader.setTranslationX(pageWidth * 1.2f * position);
                }

                if (welcome2 != null) {
                    welcome2.setAlpha(1.0f - Math.abs(position));
                }
                if (bodyContent != null) {
                    bodyContent.setTranslationX(pageWidth * 1.5f * position);
                    bodyContent.setAlpha(1.0f - Math.abs(position));
                }

                if (brcOutline != null) {
                    brcOutline.setTranslationX(pageWidth * position);
                    brcOutline.setAlpha(1.0f - Math.abs(position));
                }
            }
        }
    }
}
