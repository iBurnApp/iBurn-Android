package com.gaiagps.iburn.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import androidx.core.widget.TextViewCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextSwitcher
import android.widget.TextView
import com.gaiagps.iburn.R
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by dbro on 8/18/17.
 */
public class BottomTickerView(val viewParent: ViewGroup,
                              val layoutParams: ViewGroup.LayoutParams,
                              val showUnlockButton: Boolean,
                              val tickerHeader: String,
                              val tickerMessages: Array<String>,
                              val tickerDisplayTimeS: Int = 11,
                              val tickerMessageDisplayTimeS: Int = 4) {

    companion object {
        const val debugLog = false
    }

    var callback: Callback? = null

    fun show() {
        val context = viewParent.context.applicationContext
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val embargoBanner = inflater.inflate(R.layout.activity_main_embargo_banner, viewParent, false) as ViewGroup

        embargoBanner.layoutParams = layoutParams

        val tickerHeaderText = embargoBanner.findViewById<TextView>(R.id.ticker_text_header)
        tickerHeaderText.text = tickerHeader

        val tickerContentText = embargoBanner.findViewById<TextSwitcher>(R.id.ticker_text_switcher)
        tickerContentText.setFactory {
            Timber.d("Creating new autoResizeTextView")
            val tickerText = TextView(viewParent.context)
            tickerText.gravity = Gravity.CENTER
            tickerText.setTextColor(Color.BLACK)
            tickerText.setSingleLine(true)
            TextViewCompat.setAutoSizeTextTypeWithDefaults(tickerText, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)

            val textParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
            tickerText.layoutParams = textParams

            tickerText
        }
        val `in` = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
        val out = AnimationUtils.loadAnimation(context, R.anim.slide_out_left)

        // set the animation type of textSwitcher
        tickerContentText.inAnimation = `in`
        tickerContentText.outAnimation = out

        embargoBanner.alpha = 0f

        val enterUnlockCodeBtn = embargoBanner.findViewById<ImageButton>(R.id.enter_unlock_code_btn)
        if (!showUnlockButton) enterUnlockCodeBtn.visibility = View.GONE

        viewParent.addView(embargoBanner)

        val alphaAnimator = ValueAnimator.ofFloat(0f, 1f)
        alphaAnimator.duration = 1000
        alphaAnimator.addUpdateListener { valueAnimator -> embargoBanner.alpha = valueAnimator.animatedValue as Float }

        val tickerDisposable = Flowable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { numSecs ->
                    val messageIdx = (numSecs / tickerMessageDisplayTimeS).toInt() % tickerMessages.size
                    if (debugLog) {
                        Timber.d("Embargo counter %d messageIdx %d", numSecs, messageIdx)
                    }
                    if (numSecs == 0L) {
                        callback?.onShown()
                        alphaAnimator.start()
                        tickerContentText.setText(tickerMessages[messageIdx])
                    } else if (numSecs % tickerMessageDisplayTimeS == 0L) {
                        tickerContentText.setText(tickerMessages[messageIdx])
                    }
                }

        // Allow clicking anywhere on banner to enter unlock code
        embargoBanner.setOnClickListener { view ->  enterUnlockCodeBtn.performClick() }
        enterUnlockCodeBtn.setOnClickListener { view ->
            remove()
            callback?.onEnterUnlockCodeRequested()
            tickerDisposable.dispose()
        }

        //Auto - dismiss banner
        Flowable.timer(tickerDisplayTimeS.toLong(), TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { counter ->
                    remove()
                    tickerDisposable.dispose()
                }


    }

    fun remove() {
        val embargoBanner = viewParent.findViewById<View>(R.id.embargo_banner)
        if (embargoBanner != null) {
            val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
            fadeOutAnim.duration = 1000
            fadeOutAnim.addUpdateListener { valueAnimator ->
                embargoBanner.alpha = valueAnimator.animatedValue as Float
                if (valueAnimator.animatedFraction == 0f) {
                    viewParent.removeView(embargoBanner)
                    callback?.onDismissed()
                }
            }
            fadeOutAnim.start()
        }
    }

    interface Callback {
        fun onShown()
        fun onDismissed()
        fun onEnterUnlockCodeRequested()
    }

}