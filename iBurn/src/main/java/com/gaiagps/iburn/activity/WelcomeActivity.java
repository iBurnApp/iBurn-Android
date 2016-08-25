package com.gaiagps.iburn.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import com.gaiagps.iburn.AudioTourDownloader;
import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.UserPoiTable;
import com.gaiagps.iburn.fragment.WelcomeFragment;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class WelcomeActivity extends AppCompatActivity implements WelcomeFragment.HomeCampSelectionListener {
    static final int NUM_PAGES = 5;

    private PrefsHelper prefs;

    private CampSelection homeCampSelection;

    private ViewPager pager;
    private PagerAdapter pagerAdapter;
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

        prefs = new PrefsHelper(this);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void testPermission() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pager != null) {
            pager.clearOnPageChangeListeners();
        }
    }

    public void endTutorial() {

        if (homeCampSelection != null) {
            ContentValues poiValues = new ContentValues();
            poiValues.put(UserPoiTable.name, homeCampSelection.name);
            poiValues.put(UserPoiTable.latitude, homeCampSelection.lat);
            poiValues.put(UserPoiTable.longitude, homeCampSelection.lon);
            poiValues.put(UserPoiTable.drawableResId, UserPoiTable.HOME);
            DataProvider.getInstance(getApplicationContext())
                    .map(dataProvider -> dataProvider.insert(PlayaDatabase.POIS, poiValues))
                    .subscribe();
        }

        prefs.setDidShowWelcome(true);
        Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        finish();
        //overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
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

    public void onAudioTourDownloadButtonClicked(View view) {
        Button downloadButton = (Button) view;
        downloadButton.setText("Consider it done!");
        downloadButton.setEnabled(false);

        AudioTourDownloader atd = new AudioTourDownloader();
        atd.downloadAudioTours(this);
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
                case 4:
                    tp = WelcomeFragment.newInstance(R.layout.welcome_fragment5);
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
            View yurt = page.findViewById(R.id.yurt);

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
                if (yurt != null) {
                    yurt.setTranslationX(pageWidth * 1.5f * position);
                    yurt.setAlpha(1.0f - Math.abs(position));
                }

                if (brcOutline != null) {
                    brcOutline.setTranslationX(pageWidth * position);
                    brcOutline.setAlpha(1.0f - Math.abs(position));
                }
            }
        }
    }
}
