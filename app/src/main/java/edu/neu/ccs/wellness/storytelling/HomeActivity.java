package edu.neu.ccs.wellness.storytelling;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import edu.neu.ccs.wellness.storytelling.homeview.AdventurePresenter;
import edu.neu.ccs.wellness.storytelling.homeview.HomeAdventurePresenter;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSetting;
import edu.neu.ccs.wellness.storytelling.settings.SynchronizedSettingRepository;
import edu.neu.ccs.wellness.utils.WellnessIO;

import static edu.neu.ccs.wellness.storytelling.notifications.FcmNotificationService
        .KEY_HOME_TAB_TO_SHOW;
import static edu.neu.ccs.wellness.storytelling.notifications.FcmNotificationService.KEY_TAG;
import static edu.neu.ccs.wellness.storytelling.notifications.FcmNotificationService.NOTIF_GEOSTORY_UPDATE;
import static edu.neu.ccs.wellness.storytelling.notifications.FcmNotificationService.NOTIF_BG_SYNC_PACKAGE;

public class HomeActivity extends AppCompatActivity
        implements AdventurePresenter.AdventurePresenterListener {

    public static final String KEY_DEFAULT_TAB = "KEY_DEFAULT_TAB";
    public static final String KEY_TAB_INDEX = "HOME_TAB_INDEX";
    public static final int CODE_STORYVIEW_RESULT = 123;
    public static final String RESULT_CODE = "HOME_ACTIVITY_RESULT_CODE";
    public static final int RESULT_CHALLENGE_PICKED = 1;
    public static final int RESULT_RESET_STORY_STATES = 2;
    public static final int RESULT_RESET_THIS_STORY = 3;
    public static final String CODE_STORY_ID_TO_RESET = "CODE_STORY_ID_TO_RESET";

    // TABS RELATED CONSTANTS
    public static final int NUMBER_OF_FRAGMENTS = 3;
    public static final int TAB_STORYBOOKS = 0;
    public static final int TAB_ADVENTURE = 1;
    public static final int TAB_GEOSTORY = 2;
    public static final int TAB_TREASURES = 20;

    // TABS RELATED VARIABLES
    private final int[] TAB_ICONS = new int[]{
            R.drawable.ic_tab_storybooks, // R.drawable.ic_book_white_24,
            R.drawable.ic_tab_adventures, // R.drawable.ic_gift_white_24
            R.drawable.ic_tab_geostory // R.drawable.ic_map_white_24px
    };

    private final int[] TAB_ICONS_NEW = new int[]{
            R.drawable.ic_tab_storybooks,
            R.drawable.ic_tab_adventures,
            R.drawable.ic_tab_geostory_new
    };

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private HomePageFragmentsAdapter mScrolledTabsAdapter;
    private ViewPager mStoryHomeViewPager;
    private Bundle incomingExtras;
    private TabLayout tabLayout;
    private Storywell storywell;

    // SUPERCLASS METHODS
    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        storywell = new Storywell(this);

        mScrolledTabsAdapter = new HomePageFragmentsAdapter(getSupportFragmentManager());

        mStoryHomeViewPager = findViewById(R.id.container);
        mStoryHomeViewPager.setAdapter(mScrolledTabsAdapter);

        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mStoryHomeViewPager);
        tabLayout.getTabAt(TAB_STORYBOOKS).setIcon(TAB_ICONS[TAB_STORYBOOKS]);
        tabLayout.getTabAt(TAB_ADVENTURE).setIcon(TAB_ICONS[TAB_ADVENTURE]);
        tabLayout.getTabAt(TAB_GEOSTORY).setIcon(TAB_ICONS[TAB_GEOSTORY]);
        tabLayout.addOnTabSelectedListener(homeTabSelectedListener);

        this.incomingExtras = getIntent().getExtras();
    }

    @Override
    protected void onResume() {
        super.onResume();
        doSetCurrentTabFromSharedPrefs();
        doSetNotificationsOnTabs();
        registerReceiver(geoStoryActivityReceiver, new IntentFilter(NOTIF_BG_SYNC_PACKAGE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.setTabToSharedPrefs(mStoryHomeViewPager.getCurrentItem());

        if (geoStoryActivityReceiver != null) {
            unregisterReceiver(geoStoryActivityReceiver);
        }
    }

    @Override
    public void goToStoriesTab(String highlightedStoryId) {
        this.goToThisTab(TAB_STORYBOOKS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case CODE_STORYVIEW_RESULT:
                if (resultCode == Activity.RESULT_OK) {
                    handleStoryViewResult(intent);
                }
                break;
            case HomeAdventurePresenter.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    AdventureFragment fragment = (AdventureFragment) getSupportFragmentManager()
                            .findFragmentByTag(
                                    "android:switcher:" + R.id.container + ":" + TAB_ADVENTURE);
                    fragment.onActivityResult(requestCode, resultCode, intent);
                }
                break;
        }

    }

    private void handleStoryViewResult(Intent intent) {
        int resultCode = intent.getIntExtra(HomeActivity.RESULT_CODE, 0);

        switch (resultCode){
            case RESULT_CHALLENGE_PICKED:
                AdventureFragment fragment = (AdventureFragment) getSupportFragmentManager()
                        .findFragmentByTag(
                                "android:switcher:" + R.id.container + ":" + TAB_ADVENTURE);

                if (fragment != null) {
                    fragment.updateChallengeAndFitnessData();
                }

                break;
            case RESULT_RESET_STORY_STATES:
                new ResetStoryStates().execute();
                break;
            case RESULT_RESET_THIS_STORY:
                resetStoryCurrentPageId(intent.getStringExtra(HomeActivity.CODE_STORY_ID_TO_RESET));
                break;
        }
    }

    /* PRIVATE METHODS */
    private void goToThisTab(int tabPosition) {
        if (tabPosition >= 0 && tabPosition < NUMBER_OF_FRAGMENTS) {
            mStoryHomeViewPager.setCurrentItem(tabPosition);
        }
    }

    private void doSetCurrentTabFromSharedPrefs() {
        int tabPosition;

        if (incomingExtras != null) {
            tabPosition = Integer.valueOf(
                    incomingExtras.getString(KEY_HOME_TAB_TO_SHOW, String.valueOf(TAB_STORYBOOKS)));
        } else {
            tabPosition = WellnessIO.getSharedPref(this)
                    .getInt(KEY_DEFAULT_TAB, TAB_STORYBOOKS);
        }

        mStoryHomeViewPager.setCurrentItem(tabPosition);

        resetCurrentTab();
    }

    private void doSetNotificationsOnTabs() {
        if (storywell.getSynchronizedSetting().getNotificationInfo().isNewGeoStoryExists()) {
            tabLayout.getTabAt(TAB_GEOSTORY).setIcon(TAB_ICONS_NEW[TAB_GEOSTORY]);
        } else {
            tabLayout.getTabAt(TAB_GEOSTORY).setIcon(TAB_ICONS[TAB_GEOSTORY]);
        }
    }


    private void setTabToSharedPrefs(int position) {
        WellnessIO.getSharedPref(this).edit()
                .putInt(KEY_DEFAULT_TAB, position)
                .apply();
    }

    private void resetCurrentTab() {
        WellnessIO.getSharedPref(this).edit()
                .remove(KEY_DEFAULT_TAB)
                .apply();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class HomePageFragmentsAdapter extends FragmentPagerAdapter {

        public HomePageFragmentsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_STORYBOOKS:
                    return StoryListFragment.newInstance();
                case TAB_ADVENTURE:
                    return AdventureFragment.newInstance();
                case TAB_GEOSTORY:
                    return GeoStoryFragment.newInstance(incomingExtras);
                case TAB_TREASURES:
                    return TreasureListFragment.newInstance();
                default:
                    return StoryListFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return NUMBER_OF_FRAGMENTS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_STORYBOOKS:
                    return getString(R.string.title_stories);
                case TAB_ADVENTURE:
                    return getString(R.string.title_activities);
                case TAB_GEOSTORY:
                    return getString(R.string.title_storymap);
                case TAB_TREASURES:
                    return getString(R.string.title_treasures);
                default:
                    return getString(R.string.title_stories);
            }
        }
    }

    OnTabSelectedListener homeTabSelectedListener = new OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            switch(tab.getPosition()) {
                case TAB_STORYBOOKS:
                  break;
                case TAB_ADVENTURE:
                    break;
                case TAB_GEOSTORY:
                    SynchronizedSetting setting = storywell.getSynchronizedSetting();
                    setting.getNotificationInfo().setNewGeoStoryExist(false);
                    SynchronizedSettingRepository.saveLocalAndRemoteInstance(
                            setting, getApplicationContext());
                    tabLayout.getTabAt(TAB_GEOSTORY).setIcon(TAB_ICONS[TAB_GEOSTORY]);
                    break;
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    /**
     * AsyncTask class to reset story states;
     */
    public class ResetStoryStates extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            Storywell storywell = new Storywell(getApplicationContext());
            storywell.resetStoryStatesAsync();
            return true;
        }
    }

    /**
     * Broadcast receiver to handle new Geostory activities.
     */
    private BroadcastReceiver geoStoryActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            if (extras != null && extras.containsKey(KEY_TAG)) {
                if (NOTIF_GEOSTORY_UPDATE.equals(extras.get(KEY_TAG))) {
                    tabLayout.getTabAt(TAB_GEOSTORY).setIcon(TAB_ICONS_NEW[TAB_GEOSTORY]);
                }
            }
        }
    };

    /**
     * Resets the given story of {@param storyId} so that the current page id is 0.
     * @param storyId
     */
    private void resetStoryCurrentPageId(String storyId) {
        Storywell storywell = new Storywell(getApplicationContext());
        SynchronizedSetting setting = storywell.getSynchronizedSetting();
        setting.getStoryListInfo().getCurrentStoryPageId().put(storyId, 0);
        SynchronizedSettingRepository.saveLocalAndRemoteInstance(setting, getApplicationContext());
    }
}
