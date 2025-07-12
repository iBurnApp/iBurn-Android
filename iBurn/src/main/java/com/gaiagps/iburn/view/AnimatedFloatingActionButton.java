package com.gaiagps.iburn.view;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import androidx.annotation.ColorInt;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.AttributeSet;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.gaiagps.iburn.R;

import timber.log.Timber;

/**
 * Modified from work by Miroslaw Stanek and Joel Dean from https://github.com/jd-alexander/LikeButton
 */
public class AnimatedFloatingActionButton extends FloatingActionButton {

    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateDecelerateInterpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator();

    private int START_COLOR = 0xFFFF5F5F;
    private int END_COLOR = 0xFFFE8080;

    private ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    private Paint circlePaint = new Paint();
    private Paint maskPaint = new Paint();

    private Bitmap tempBitmap;
    private Canvas tempCanvas;

    private float outerCircleRadiusProgress = 0f;
    private float innerCircleRadiusProgress = 0f;

    private int width = 0;
    private int height = 0;

    private int maxCircleSize;

    private Bitmap nonSelectedBitmap;
    private Bitmap selectedBitmap;
    private boolean selectionState;

    public AnimatedFloatingActionButton(Context context) {
        super(context);
        init();
    }

    public AnimatedFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        circlePaint.setStyle(Paint.Style.FILL);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        nonSelectedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_heart);
        selectedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_heart_pressed);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        invalidate();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (width != 0 && height != 0)
            setMeasuredDimension(width, height);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        maxCircleSize = w / 2;
        tempBitmap = Bitmap.createBitmap(getWidth(), getWidth(), Bitmap.Config.ARGB_8888);
        tempCanvas = new Canvas(tempBitmap);
    }

    //private Rect bitmapRect;
    private Matrix bitmapMatrix;
    private float bitmapMatrixInitialScale;
    private float lastBitmapMatrixScale;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        tempCanvas.drawColor(0xffffff, PorterDuff.Mode.CLEAR);
        tempCanvas.drawCircle(getWidth() / 2, getHeight() / 2, outerCircleRadiusProgress * maxCircleSize, circlePaint);
        tempCanvas.drawCircle(getWidth() / 2, getHeight() / 2, innerCircleRadiusProgress * maxCircleSize, maskPaint);

        if (bitmapMatrix == null) {
            // Initial setup
            bitmapMatrix = new Matrix();
            bitmapMatrixInitialScale = ((float) canvas.getClipBounds().width() * .5f) / selectedBitmap.getWidth();
//            bitmapMatrix.setTranslate(selectedBitmap.getWidth() / 16, selectedBitmap.getHeight() / 16);
            bitmapMatrix.setScale(bitmapMatrixInitialScale, bitmapMatrixInitialScale);
            bitmapMatrix.postTranslate((getWidth() / 2) - (selectedBitmap.getWidth() * bitmapMatrixInitialScale / 2),
                    (getHeight() / 2) - (selectedBitmap.getHeight() * bitmapMatrixInitialScale / 2));
            //bitmapRect = new Rect(getWidth() / 4, getHeight() / 4, (int) (getWidth() * .75f), (int) (getHeight() * .75f));
            Timber.d("bitmap width %s view width %s. scale %f. translate x %d", selectedBitmap.getWidth(), getWidth(), bitmapMatrixInitialScale, selectedBitmap.getWidth());
        }

        tempCanvas.drawBitmap(selectionState ? selectedBitmap : nonSelectedBitmap, bitmapMatrix, circlePaint);
        canvas.drawBitmap(tempBitmap, 0, 0, null);
    }

    public void setInnerCircleRadiusProgress(float innerCircleRadiusProgress) {
        this.innerCircleRadiusProgress = innerCircleRadiusProgress;
        postInvalidate();
    }

    public float getInnerCircleRadiusProgress() {
        return innerCircleRadiusProgress;
    }

    public void setOuterCircleRadiusProgress(float outerCircleRadiusProgress) {
        this.outerCircleRadiusProgress = outerCircleRadiusProgress;
        updateCircleColor();
        postInvalidate();
    }

    public void setIconScale(float scale) {
        lastBitmapMatrixScale = bitmapMatrixInitialScale * scale;
        if (bitmapMatrix == null) return;
        bitmapMatrix.setScale(lastBitmapMatrixScale, lastBitmapMatrixScale);
        bitmapMatrix.postTranslate((getWidth() / 2) - (selectedBitmap.getWidth() * lastBitmapMatrixScale / 2),
                (getHeight() / 2) - (selectedBitmap.getHeight() * lastBitmapMatrixScale / 2));
    }

    public float getIconScale() {;
        return lastBitmapMatrixScale;
    }

    private void updateCircleColor() {
        float colorProgress = (float) Utils.clamp(outerCircleRadiusProgress, 0.5, 1);
        colorProgress = (float) Utils.mapValueFromRangeToRange(colorProgress, 0.5f, 1f, 0f, 1f);
        this.circlePaint.setColor((Integer) argbEvaluator.evaluate(colorProgress, START_COLOR, END_COLOR));
    }

    public float getOuterCircleRadiusProgress() {
        return outerCircleRadiusProgress;
    }

    public static final Property<AnimatedFloatingActionButton, Float> INNER_CIRCLE_RADIUS_PROGRESS =
            new Property<AnimatedFloatingActionButton, Float>(Float.class, "innerCircleRadiusProgress") {
                @Override
                public Float get(AnimatedFloatingActionButton object) {
                    return object.getInnerCircleRadiusProgress();
                }

                @Override
                public void set(AnimatedFloatingActionButton object, Float value) {
                    object.setInnerCircleRadiusProgress(value);
                }
            };

    public static final Property<AnimatedFloatingActionButton, Float> OUTER_CIRCLE_RADIUS_PROGRESS =
            new Property<AnimatedFloatingActionButton, Float>(Float.class, "outerCircleRadiusProgress") {
                @Override
                public Float get(AnimatedFloatingActionButton object) {
                    return object.getOuterCircleRadiusProgress();
                }

                @Override
                public void set(AnimatedFloatingActionButton object, Float value) {
                    object.setOuterCircleRadiusProgress(value);
                }
            };

    public static final Property<AnimatedFloatingActionButton, Float> ICON_SCALE =
            new Property<AnimatedFloatingActionButton, Float>(Float.class, "setIconScale") {
                @Override
                public Float get(AnimatedFloatingActionButton object) {
                    return object.getIconScale();
                }

                @Override
                public void set(AnimatedFloatingActionButton object, Float value) {
                    object.setIconScale(value);
                }
            };

    public void setStartColor(@ColorInt int color) {
        START_COLOR = color;
        invalidate();
    }

    public void setEndColor(@ColorInt int color) {
        END_COLOR = color;
        invalidate();
    }

    public void setSelectedState(boolean selected, boolean animate) {

        selectionState = selected;

        if (animate) {
            setInnerCircleRadiusProgress(0);
            setOuterCircleRadiusProgress(0);

            AnimatorSet animatorSet = new AnimatorSet();

            ObjectAnimator outerCircleAnimator = ObjectAnimator.ofFloat(this, AnimatedFloatingActionButton.OUTER_CIRCLE_RADIUS_PROGRESS, 0.1f, 1f);
            outerCircleAnimator.setDuration(250);
            outerCircleAnimator.setInterpolator(DECCELERATE_INTERPOLATOR);

            ObjectAnimator innerCircleAnimator = ObjectAnimator.ofFloat(this, AnimatedFloatingActionButton.INNER_CIRCLE_RADIUS_PROGRESS, 0.1f, 1f);
            innerCircleAnimator.setDuration(200);
            innerCircleAnimator.setStartDelay(200);
            innerCircleAnimator.setInterpolator(DECCELERATE_INTERPOLATOR);

            // Double pulse animation matching ViewExtensions.animateScalePulse
            // Total duration: 300ms, divided into 5 phases of 60ms each

            // First pulse: up and down
            ObjectAnimator iconAnimator1 = ObjectAnimator.ofFloat(this, AnimatedFloatingActionButton.ICON_SCALE, 1f, 1.2f);
            iconAnimator1.setDuration(60);  // duration/5
            iconAnimator1.setStartDelay(0);
            iconAnimator1.setInterpolator(DECCELERATE_INTERPOLATOR);

            ObjectAnimator iconAnimator2 = ObjectAnimator.ofFloat(this, AnimatedFloatingActionButton.ICON_SCALE, 1.2f, 1f);
            iconAnimator2.setDuration(60);  // duration/5
            iconAnimator2.setStartDelay(60);
            iconAnimator2.setInterpolator(DECCELERATE_INTERPOLATOR);

            // Pause between pulses (60ms delay before second pulse)

            // Second pulse: up and down (larger scale like ViewExtensions)
            ObjectAnimator iconAnimator3 = ObjectAnimator.ofFloat(this, AnimatedFloatingActionButton.ICON_SCALE, 1f, 1.32f);
            iconAnimator3.setDuration(60);  // duration/5
            iconAnimator3.setStartDelay(180);  // After first pulse + pause
            iconAnimator3.setInterpolator(DECCELERATE_INTERPOLATOR);

            ObjectAnimator iconAnimator4 = ObjectAnimator.ofFloat(this, AnimatedFloatingActionButton.ICON_SCALE, 1.32f, 1f);
            iconAnimator4.setDuration(60);  // duration/5
            iconAnimator4.setStartDelay(240);
            iconAnimator4.setInterpolator(DECCELERATE_INTERPOLATOR);

            animatorSet.playTogether(
                    outerCircleAnimator,
                    innerCircleAnimator,
                    iconAnimator1,
                    iconAnimator2,
                    iconAnimator3,
                    iconAnimator4);

            animatorSet.start();
        }
    }
}