package com.gaiagps.iburn.view

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnPreDraw
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.animation.ViewPositionAnimator
import com.alexvasilkov.gestures.views.GestureImageView
import kotlin.math.abs

/** Fullscreen image presentation powered by GestureViews. */
class FullscreenImageDialog(
    context: Context,
    private val sourceImage: ImageView,
    private val imageDescription: CharSequence?
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var root: FrameLayout
    private lateinit var scrim: View
    private lateinit var gestureImage: ZoomedSwipeDismissImageView
    private lateinit var closeButton: AppCompatImageButton
    private var sourcePresentationWasChanged = false
    private var isFinishing = false
    private val sourceOriginalVisibility = sourceImage.visibility

    private val positionListener =
        ViewPositionAnimator.PositionUpdateListener { position, isLeaving ->
            scrim.alpha = position
            closeButton.alpha = controlsAlpha(position)
            if (position == 0f && isLeaving) finishDismissal()
        }

    @Suppress("DEPRECATION") // Required below API 35 to keep both windows' bar colors identical.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { windowAnimations = 0 }
            // The platform fullscreen theme can apply a different system-bar appearance while
            // this window is being attached. Keeping the host appearance avoids an icon flash.
            WindowCompat.setDecorFitsSystemWindows(this, false)
            val hostWindow = (sourceImage.context as? Activity)?.window
            if (hostWindow != null) {
                statusBarColor = hostWindow.statusBarColor
                navigationBarColor = hostWindow.navigationBarColor
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    isStatusBarContrastEnforced = hostWindow.isStatusBarContrastEnforced
                    isNavigationBarContrastEnforced =
                        hostWindow.isNavigationBarContrastEnforced
                }
                val hostController = WindowInsetsControllerCompat(
                    hostWindow,
                    hostWindow.decorView
                )
                WindowInsetsControllerCompat(this, decorView).apply {
                    isAppearanceLightStatusBars = hostController.isAppearanceLightStatusBars
                    isAppearanceLightNavigationBars =
                        hostController.isAppearanceLightNavigationBars
                }
            }
        }

        val image = sourceImage.drawable ?: return
        root = FrameLayout(context)
        scrim = View(context).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
        }
        gestureImage = ZoomedSwipeDismissImageView(context).apply {
            visibility = View.INVISIBLE
            contentDescription = imageDescription
            setImageDrawable(copyDrawable(image))
            onZoomedSwipeDismiss = {
                controller.stopAllAnimations()
                startExit()
            }
            controller.settings
                .setMaxZoom(MAX_ZOOM)
                .setDoubleTapZoom(DOUBLE_TAP_ZOOM)
                .setOverzoomFactor(OVERZOOM_FACTOR)
                .setOverscrollDistance(context, OVERSCROLL_DISTANCE_DP, OVERSCROLL_DISTANCE_DP)
                .setExitEnabled(true)
                .setExitType(Settings.ExitType.ALL)
                .setAnimationsDuration(SETTLE_DURATION_MS)
            positionAnimator.addPositionUpdateListener(positionListener)
        }
        closeButton = AppCompatImageButton(context).apply {
            val buttonSize = dp(48)
            layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.TOP or Gravity.END).apply {
                topMargin = dp(12)
                marginEnd = dp(12)
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            contentDescription = context.getString(android.R.string.cancel)
            alpha = 0f
            setOnClickListener { startExit() }
        }

        root.addView(scrim, matchParentLayoutParams())
        root.addView(gestureImage, matchParentLayoutParams())
        root.addView(closeButton)
        setContentView(root)

        setOnKeyListener { _, keyCode, event ->
            if (keyCode != KeyEvent.KEYCODE_BACK) return@setOnKeyListener false
            if (event.action == KeyEvent.ACTION_UP) startExit()
            true
        }
    }

    override fun onStart() {
        super.onStart()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        root.doOnPreDraw {
            if (!isShowing || isFinishing) return@doOnPreDraw
            // GestureViews makes the source view INVISIBLE while it owns the transition.
            // Record that before entering so every dismissal path can restore the source.
            sourcePresentationWasChanged = true
            gestureImage.positionAnimator.enter(sourceImage, true)
            // Establish the source-position state before the dialog's first frame. Otherwise the
            // full-size image and the activity controls can each be exposed for a frame.
            gestureImage.visibility = View.VISIBLE
        }
    }

    override fun dismiss() {
        cleanup()
        super.dismiss()
    }

    override fun onDetachedFromWindow() {
        cleanup()
        super.onDetachedFromWindow()
    }

    private fun startExit() {
        if (isFinishing || !isShowing || gestureImage.positionAnimator.isLeaving) return
        gestureImage.positionAnimator.exit(true)
    }

    private fun finishDismissal() {
        if (isFinishing) return
        isFinishing = true

        // GestureViews restores fullscreen state after completing an exit. Keep the dialog image
        // at the source position, restore the real image beneath it, then fade the dialog copy
        // away. Activity controls underneath are therefore revealed gradually instead of being
        // drawn over the returning image for the last frame.
        gestureImage.controller.settings.disableBounds()
        gestureImage.positionAnimator.setState(0f, false, false)
        restoreSourceImage()
        gestureImage.animate()
            .alpha(0f)
            .setDuration(EXIT_FADE_DURATION_MS)
            .withEndAction { completeDismissal() }
            .start()
    }

    private fun completeDismissal() {
        if (!isShowing) return
        super.dismiss()
    }

    private fun cleanup() {
        if (::gestureImage.isInitialized) {
            gestureImage.animate().cancel()
            gestureImage.positionAnimator.removePositionUpdateListener(positionListener)
        }
        restoreSourceImage()
    }

    private fun restoreSourceImage() {
        if (!sourcePresentationWasChanged) return
        sourceImage.visibility = sourceOriginalVisibility
    }

    private fun controlsAlpha(position: Float): Float =
        ((position - CONTROLS_FADE_START) / (1f - CONTROLS_FADE_START)).coerceIn(0f, 1f)

    private fun copyDrawable(image: Drawable): Drawable =
        image.constantState?.newDrawable(context.resources)?.mutate() ?: image

    private fun matchParentLayoutParams() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val MAX_ZOOM = 5f
        const val DOUBLE_TAP_ZOOM = 2.5f
        const val OVERZOOM_FACTOR = 1.3f
        const val OVERSCROLL_DISTANCE_DP = 96f
        const val SETTLE_DURATION_MS = 180L
        const val EXIT_FADE_DURATION_MS = 140L
        const val CONTROLS_FADE_START = 0.72f
    }
}

/** Adds a deliberate swipe exit only while GestureViews' built-in exit is zoom-gated. */
private class ZoomedSwipeDismissImageView(context: Context) : GestureImageView(context) {
    var onZoomedSwipeDismiss: (() -> Unit)? = null
    private var hadMultiplePointers = false

    private val dismissDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onFling(
                firstEvent: MotionEvent?,
                lastEvent: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val start = firstEvent ?: return false
                if (hadMultiplePointers || !isZoomedIn()) return false

                val density = resources.displayMetrics.density
                val distanceY = abs(lastEvent.y - start.y)
                val speedY = abs(velocityY)
                val speedX = abs(velocityX)
                if (distanceY < MIN_VERTICAL_DISTANCE_DP * density ||
                    speedY < MIN_VERTICAL_VELOCITY_DP_PER_SECOND * density ||
                    speedY < speedX * MIN_VERTICAL_DOMINANCE
                ) {
                    return false
                }

                onZoomedSwipeDismiss?.invoke()
                return true
            }
        }
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) hadMultiplePointers = false
        if (event.pointerCount > 1) hadMultiplePointers = true

        val handled = super.onTouchEvent(event)
        dismissDetector.onTouchEvent(event)
        return handled
    }

    private fun isZoomedIn(): Boolean {
        val state = controller.state
        val minZoom = controller.stateController.getMinZoom(state)
        return state.zoom > minZoom * MIN_ZOOM_RATIO
    }

    private companion object {
        const val MIN_VERTICAL_DISTANCE_DP = 120f
        const val MIN_VERTICAL_VELOCITY_DP_PER_SECOND = 2_000f
        const val MIN_VERTICAL_DOMINANCE = 1.5f
        const val MIN_ZOOM_RATIO = 1.01f
    }
}
