package com.gaiagps.iburn.activity

import android.Manifest
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.gaiagps.iburn.PrefsHelper
import com.gaiagps.iburn.R
import com.gaiagps.iburn.database.CampWithUserData
import com.gaiagps.iburn.database.DataProvider.Companion.getInstance
import com.gaiagps.iburn.database.UserPoi
import com.gaiagps.iburn.fragment.WelcomeFragment
import com.gaiagps.iburn.fragment.WelcomeFragment.Companion.newInstance
import com.gaiagps.iburn.fragment.WelcomeFragment.HomeCampSelectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import kotlin.math.abs

@RuntimePermissions
class WelcomeActivity : AppCompatActivity(), HomeCampSelectionListener {
    private var prefs: PrefsHelper? = null

    private var homeCampSelection: CampWithUserData? = null

    private var pager: ViewPager? = null
    private var pagerAdapter: PagerAdapter? = null
    private var skip: Button? = null
    private var done: Button? = null
    private var next: ImageButton? = null
    private var nextDrawable: Drawable? = null
    private var isOpaque = true

    private var performedEntranceAnimation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(this)

        //        Window window = getWindow();
//        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_welcome)
        skip = Button::class.java.cast(findViewById<View?>(R.id.skip))
        skip!!.setOnClickListener(View.OnClickListener { v: View? -> endTutorial() })
        // Start bottom nav controls as white over black video
        val navColor = getColor(android.R.color.white)
        skip!!.setTextColor(navColor)

        next = ImageButton::class.java.cast(findViewById<View?>(R.id.next))
        next!!.setOnClickListener(View.OnClickListener { v: View? ->
            pager!!.setCurrentItem(
                pager!!.getCurrentItem() + 1,
                true
            )
        })
        // Intercept the button drawable for dynamic tinting of just this button
        nextDrawable = next!!.getDrawable().mutate()
        nextDrawable!!.setTint(navColor)
        next!!.setImageDrawable(nextDrawable)

        done = Button::class.java.cast(findViewById<View?>(R.id.done))
        done!!.setOnClickListener(View.OnClickListener { v: View? -> endTutorial() })

        pager = findViewById<View?>(R.id.pager) as ViewPager?
        pagerAdapter = ScreenSlidePagerAdapter(getSupportFragmentManager())
        pager!!.setAdapter(pagerAdapter)
        pager!!.setPageTransformer(true, CrossfadePageTransformer())

        pager!!.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position == NUM_PAGES - 2 && positionOffset > 0) {
                    if (isOpaque) {
                        pager!!.setBackgroundColor(Color.TRANSPARENT)
                        isOpaque = false
                    }
                } else {
                    if (!isOpaque) {
                        pager!!.setBackgroundColor(getResources().getColor(R.color.window_background))
                        isOpaque = true
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    // The first page is a video that starts black
                    val navColor = getColor(android.R.color.white)
                    skip!!.setTextColor(navColor)
                    nextDrawable!!.setTint(navColor)
                } else {
                    // All other pages should use the regular text color
                    val navColor = getColor(R.color.regular_text)
                    skip!!.setTextColor(navColor)
                    nextDrawable!!.setTint(navColor)
                }
                if (position == NUM_PAGES - 2) {
                    skip!!.setVisibility(View.GONE)
                    next!!.setVisibility(View.GONE)
                    done!!.setVisibility(View.VISIBLE)
                } else if (position < NUM_PAGES - 2) {
                    skip!!.setVisibility(View.VISIBLE)
                    next!!.setVisibility(View.VISIBLE)
                    done!!.setVisibility(View.GONE)
                } else if (position == NUM_PAGES - 1) {
                    endTutorial()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        prefs = PrefsHelper(this)
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun testPermission() {
    }

    override fun onDestroy() {
        super.onDestroy()
        if (pager != null) {
            pager!!.clearOnPageChangeListeners()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!performedEntranceAnimation) {
            val target = findViewById<View>(R.id.button_layout)
            target.setAlpha(0f)
            val subFadeIn = ValueAnimator.ofFloat(0f, 1f)
            subFadeIn.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator? ->
                target.setAlpha(
                    (animation!!.getAnimatedValue() as kotlin.Float?)!!
                )
            })
            subFadeIn.setStartDelay(3000)
            subFadeIn.setDuration((1 * 1000).toLong())
            subFadeIn.start()
            performedEntranceAnimation = true
        }
    }

    fun endTutorial() {
        if (homeCampSelection != null) {
            val poi = UserPoi()
            val homeCamp = homeCampSelection!!.item
            poi.name = homeCamp.name
            if (homeCamp.hasLocation()) {
                poi.latitude = homeCamp.latitude
                poi.longitude = homeCamp.longitude
            } else {
                poi.latitude = homeCamp.latitudeUnofficial
                poi.longitude = homeCamp.longitudeUnofficial
            }
            poi.icon = UserPoi.ICON_HOME
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    getInstance(applicationContext).insertUserPoi(poi)
                } catch (ignored: Throwable) {
                }
            }
        }

        prefs!!.setDidShowWelcome(true)
        val mainIntent = Intent(getApplicationContext(), MainActivity::class.java)
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(mainIntent)
        finish()
        //overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }

    override fun onBackPressed() {
        if (pager!!.currentItem == 0) {
            super.onBackPressed()
        } else {
            pager!!.setCurrentItem(pager!!.getCurrentItem() - 1)
        }
    }

    //    public void onAudioTourDownloadButtonClicked(View view) {
    //        Button downloadButton = (Button) view;
    //        downloadButton.setText("Consider it done!");
    //        downloadButton.setEnabled(false);
    //
    //        AudioTourDownloader atd = new AudioTourDownloader();
    //        atd.downloadAudioTours(this);
    //    }
    override fun onHomeCampSelected(homeCamp: CampWithUserData?) {
        homeCampSelection = homeCamp
    }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            val tp: WelcomeFragment = when (position) {
                0 -> newInstance(R.layout.welcome_fragment1)
                1 -> newInstance(R.layout.welcome_fragment2)
                2 -> newInstance(R.layout.welcome_fragment3)
                else -> newInstance(R.layout.welcome_fragment5)
            }

            return tp
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }
    }

    inner class CrossfadePageTransformer : ViewPager.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            val pageWidth = page.getWidth()

            val parallax0 = page.findViewById<View?>(R.id.parallax0)
            val parallax1 = page.findViewById<View?>(R.id.parallax1)

            val welcomeHeader = page.findViewById<View?>(R.id.welcomeHeader)
            val video = page.findViewById<View?>(R.id.video)
            val welcome2 = page.findViewById<View?>(R.id.welcome_fragment2)

            if (position <= -1.0f || position >= 1.0f) {
                // do nothing
            } else if (position == 0.0f) {
                // do nothing
            } else {
                if (video != null) {
                    video.setAlpha(1.0f - abs(position))
                    page.setTranslationX(pageWidth * -position)
                }

                if (welcomeHeader != null) {
                    welcomeHeader.setAlpha(1.0f - abs(position))
                    welcomeHeader.setTranslationX(pageWidth * 1.2f * position)
                }

                if (welcome2 != null) {
                    welcome2.setAlpha(1.0f - abs(position))
                }

                if (parallax0 != null) {
                    parallax0.setTranslationX(pageWidth * position)
                    parallax0.setAlpha(1.0f - abs(position))
                }

                if (parallax1 != null) {
                    parallax1.setTranslationX(.75f * pageWidth * position)
                    parallax1.setAlpha(1.0f - abs(position))
                }
            }
        }
    }

    companion object {
        const val NUM_PAGES: Int = 4
    }
}
