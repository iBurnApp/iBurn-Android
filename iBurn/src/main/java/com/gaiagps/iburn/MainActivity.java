package com.gaiagps.iburn;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import static com.gaiagps.iburn.DataUtils.checkAndSetupDB;

public class MainActivity extends ActionBarActivity {

    // Hold display width to allow MapViewPager to calculate
    // swiping margin on screen's right border.
    public static int display_width = -1;

    TabHost mTabHost;
    MapViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
    //TitlePageIndicator mTitlePageIndicator;

    LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("");
        getDisplayWidth();
        setContentView(R.layout.activity_main);
        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setupFragmentStatePagerAdapter();
        checkIntentForExtras(getIntent());
        if(getSharedPreferences(Constants.GENERAL_PREFS, MODE_PRIVATE).getBoolean(Constants.FIRST_TIME, true)){
            showWelcomeDialog();
            getSharedPreferences(Constants.GENERAL_PREFS, MODE_PRIVATE).edit().putBoolean(Constants.FIRST_TIME, false).commit();
        }

    }

    private void showWelcomeDialog(){
        View dialog = getLayoutInflater().inflate(R.layout.dialog_welcome, null);
        new AlertDialog.Builder(this)
                .setView(dialog)
                .show();
    }

    @Override
    protected void onNewIntent(Intent intent){
        checkIntentForExtras(intent);
    }

    private void checkIntentForExtras(Intent intent){
        if(intent.hasExtra("tab")){
            Constants.TAB_TYPE tab = (Constants.TAB_TYPE) intent.getSerializableExtra("tab");
            switch(tab){
                case MAP:
                    mViewPager.setCurrentItem(0, true);
                    LatLng marker = new LatLng(intent.getFloatExtra("lat",0), intent.getFloatExtra("lon",0));

                    ((GoogleMapFragment)mTabsAdapter.getItem(0)).mapAndCenterOnMarker(new MarkerOptions().position(marker).title(intent.getStringExtra("title")));
                    Log.i("GoogleMapFragment", "queue marker");
                    break;

            }
        }
    }

    private void setupFragmentStatePagerAdapter(){
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mViewPager = (MapViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);


        //mTitlePageIndicator = (TitlePageIndicator)findViewById(R.id.titles);
        //mTitlePageIndicator.setViewPager(mViewPager);

        String label = null;
        for(Constants.TAB_TYPE tabType : Constants.TAB_TYPE.values()){
            label = getString(Constants.TAB_TO_TITLE.get(tabType));
            Bundle bundle = new Bundle(1);
            bundle.putSerializable("type", tabType);
            switch(tabType){
                case MAP:
                    mTabsAdapter.addTab(mTabHost.newTabSpec(label).setIndicator(inflateCustomTab(label)),
                            GoogleMapFragment.class, null);
                    break;
                case ART:
                    mTabsAdapter.addTab(mTabHost.newTabSpec(label).setIndicator(inflateCustomTab(label)),
                            ArtListViewFragment.class, bundle);
                    break;
                case EVENTS:
                    mTabsAdapter.addTab(mTabHost.newTabSpec(label).setIndicator(inflateCustomTab(label)),
                            EventListViewFragment.class, bundle);
                    break;
                case CAMPS:
                    mTabsAdapter.addTab(mTabHost.newTabSpec(label).setIndicator(inflateCustomTab(label)),
                            CampListViewFragment.class, bundle);
                    break;

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!BurnState.isEmbargoClear(getApplicationContext()))
            getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(BurnState.isEmbargoClear(getApplicationContext()))
            menu.removeItem(R.id.action_unlock);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        int id = item.getItemId();
        if(id == R.id.action_unlock){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Enter Unlock Password");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            alert.setView(input);
            alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString();
                    if(value.compareTo(BurnState.UNLOCK_PW)==0){
                        BurnState.setEmbargoClear(getApplicationContext(), true);
                        new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.victory))
                        .setMessage(getString(R.string.location_data_unlocked))
                        .show();
                    }
                    else{
                        dialog.cancel();
                        new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.invalid_password))
                        .show();
                    }
                }
            });

            alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });

            alert.show();
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    /*
    private View inflateCustomTab(String tab_title){
        ViewGroup tab = (ViewGroup) inflater.inflate(R.layout.burn_tab, (ViewGroup) this.findViewById(android.R.id.tabs), false);
        ((TextView)tab.findViewById(R.id.title)).setText(tab_title);
        return tab;
    }
    */

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentStatePagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }

        @Override
        public CharSequence getPageTitle (int position){
            return mTabs.get(position).tag;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    /**
     * Measure display width so the view pager can implement its
     * custom behavior re: paging on the map view
     */
    private void getDisplayWidth(){
        Display display = getWindowManager().getDefaultDisplay();
        display_width = display.getWidth();
    }

    private View inflateCustomTab(String tab_title){
        ViewGroup tab = (ViewGroup) inflater.inflate(R.layout.tab_indicator_iburn, (ViewGroup) this.findViewById(android.R.id.tabs), false);
        ((TextView)tab.findViewById(R.id.title)).setText(tab_title);
        return tab;
    }
    
}
