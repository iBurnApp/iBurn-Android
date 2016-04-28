package com.gaiagps.iburn.activity;

import android.support.test.espresso.action.ViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.test.suitebuilder.annotation.LargeTest;

import com.gaiagps.iburn.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * This UI Test clicks on every item in the list views, ensuring no crashes occur
 * while populating the item detail views.
 *
 * Created by dbro on 8/24/15.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule(
            MainActivity.class);

    @Test
    public void changeText_sameActivity() {
//
//        // Click browse tab
//        onView(withText(R.string.browse_tab)).perform(click());
//
//        // Click through every list item
//        int itemCount = ((RecyclerView) activityRule.getActivity().findViewById(android.R.id.list)).getAdapter().getItemCount();
//
//        for (int pos = 0; pos < itemCount; pos++) {
//            // Click on a list item, then back out
//            onView(withId(android.R.id.list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
//            onView(isRoot()).perform(ViewActions.pressBack());
//
//        }
    }
}
