package com.gaiagps.iburn;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import com.viewpagerindicator.TitlePageIndicator;

import java.util.ArrayList;

public class MainActivity extends FragmentActivity {

    // Hold display width to allow MapViewPager to calculate
    // swiping margin on screen's right border.
    public static int display_width = -1;

    TabHost mTabHost;
    MapViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
    TitlePageIndicator mTitlePageIndicator;

    LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getDisplayWidth();
        setContentView(R.layout.activity_main);
        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setupFragmentStatePagerAdapter();
    }

    private void setupFragmentStatePagerAdapter(){
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mViewPager = (MapViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);


        mTitlePageIndicator = (TitlePageIndicator)findViewById(R.id.titles);
        mTitlePageIndicator.setViewPager(mViewPager);

        String label = null;
        for(Constants.TAB_TYPE tabType : Constants.TAB_TYPE.values()){
            label = getString(Constants.TAB_TO_TITLE.get(tabType));
            if(tabType.compareTo(Constants.TAB_TYPE.MAP) == 0){
                mTabsAdapter.addTab(mTabHost.newTabSpec(label).setIndicator(inflateCustomTab(label)),
                        GoogleMapFragment.class, null);
            }else{
                Bundle bundle = new Bundle(1);
                bundle.putSerializable("type", tabType);
                mTabsAdapter.addTab(mTabHost.newTabSpec(label).setIndicator(inflateCustomTab(label)),
                        ListViewFragment.class, bundle);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    private View inflateCustomTab(String tab_title){
        ViewGroup tab = (ViewGroup) inflater.inflate(R.layout.burn_tab, (ViewGroup) this.findViewById(android.R.id.tabs), false);
        ((TextView)tab.findViewById(R.id.title)).setText(tab_title);
        return tab;
    }

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
    
}
