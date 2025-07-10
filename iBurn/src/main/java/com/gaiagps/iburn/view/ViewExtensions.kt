package com.gaiagps.iburn.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * Extension functions for View animations
 */

/**
 * Animates the View with two scale pulses from 1x to 1.2x and back to 1x
 * @param duration Duration for the complete animation cycle (default 300ms for both pulses)
 * @param onAnimationEnd Optional callback when animation completes
 */
fun View.animateScalePulse(
    duration: Long = 300L,
    onAnimationEnd: (() -> Unit)? = null
) {
    // First pulse - scale up and down
    val scaleUp1X = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.2f)
    val scaleUp1Y = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.2f)
    val scaleDown1X = ObjectAnimator.ofFloat(this, "scaleX", 1.2f, 1f)
    val scaleDown1Y = ObjectAnimator.ofFloat(this, "scaleY", 1.2f, 1f)

    // Second pulse - scale up and down
    val scaleUp2X = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1.32f)
    val scaleUp2Y = ObjectAnimator.ofFloat(this, "scaleY", 1f, 1.32f)
    val scaleDown2X = ObjectAnimator.ofFloat(this, "scaleX", 1.32f, 1f)
    val scaleDown2Y = ObjectAnimator.ofFloat(this, "scaleY", 1.32f, 1f)

    // Create animation sets for each phase
    val firstPulseUp = AnimatorSet().apply {
        playTogether(scaleUp1X, scaleUp1Y)
        setDuration(duration / 5)  // Adjusted to account for pause
        interpolator = DecelerateInterpolator()
    }

    val firstPulseDown = AnimatorSet().apply {
        playTogether(scaleDown1X, scaleDown1Y)
        setDuration(duration / 5)  // Adjusted to account for pause
        interpolator = DecelerateInterpolator()
    }

    // Create a pause between pulses using a dummy animator
    val pauseBetweenPulses = ObjectAnimator.ofFloat(this, "alpha", 1f, 1f).apply {
        setDuration(duration / 5)  // Short pause duration
    }

    val secondPulseUp = AnimatorSet().apply {
        playTogether(scaleUp2X, scaleUp2Y)
        setDuration(duration / 5)  // Adjusted to account for pause
        interpolator = DecelerateInterpolator()
    }

    val secondPulseDown = AnimatorSet().apply {
        playTogether(scaleDown2X, scaleDown2Y)
        setDuration(duration / 5)  // Adjusted to account for pause
        interpolator = DecelerateInterpolator()
    }

    // Create the complete animation sequence - two pulses with pause
    val completeAnimation = AnimatorSet().apply {
        playSequentially(
            firstPulseUp,
            firstPulseDown,
            pauseBetweenPulses,
            secondPulseUp,
            secondPulseDown
        )
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd?.invoke()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    completeAnimation.start()
}

/**
 * Animates the View with a scale effect and executes the provided action
 * @param action The action to execute during the animation
 * @param duration Duration for the complete animation cycle (default 300ms for both pulses)
 */
fun View.animateScalePulseWithAction(
    duration: Long = 300L,
    action: () -> Unit,
) {
    animateScalePulse(duration) {
        // Animation completed
    }
    // Execute action immediately when animation starts
    action()
}