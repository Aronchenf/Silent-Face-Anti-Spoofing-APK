package com.aronc.aisllibrary.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import com.aronc.aisllibrary.R;


public class FlashBox extends FrameLayout {
    private static final int DEFAULT_BOX_COLOR = Color.parseColor("#F0282D");
    private static final float DEFAULT_BOX_STROKE_WIDTH = 12.0F;
    private static final int DEFAULT_BOX_SHADOW_COLOR = Color.parseColor("#F0282D");
    private static final float DEFAULT_BOX_SHADOW_WIDTH = 60.0F;
    private static final int DEFAULT_BOX_ANIM_DELAY = 1000;
    private Paint mBoxPaint;
    private int mBoxColor = DEFAULT_BOX_COLOR;
    private float mBoxStrokeWidth = 12.0F;
    private Paint mBoxShadowPaint;
    private int mBoxShadowColor = DEFAULT_BOX_SHADOW_COLOR;
    private float mBoxShadowWidth = 60.0F;
    private int mBoxAnimDelay = 1000;
    private LinearGradient mLeftBorderGradient;
    private LinearGradient mTopBorderGradient;
    private LinearGradient mRightBorderGradient;
    private LinearGradient mBottomBorderGradient;
    private ValueAnimator mFlashAnimator;
    private boolean isFlashing;

    public FlashBox(Context paramContext) {
        this(paramContext, null);
    }

    public FlashBox(Context paramContext, AttributeSet paramAttributeSet) {
        this(paramContext, paramAttributeSet, 0);
    }

    public FlashBox(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        if ((paramAttributeSet != null) && ((paramContext.obtainStyledAttributes(paramAttributeSet, R.styleable.FlashBox)) != null)) {
            TypedArray a = paramContext.obtainStyledAttributes(paramAttributeSet, R.styleable.FlashBox);
            this.mBoxColor = a.getColor(R.styleable.FlashBox_boxColor,DEFAULT_BOX_COLOR);
            this.mBoxStrokeWidth = a.getDimension(R.styleable.FlashBox_boxStrokeWidth,12.0F);
            this.mBoxShadowColor = a.getColor(R.styleable.FlashBox_boxShadowColor, DEFAULT_BOX_SHADOW_COLOR);
            this.mBoxShadowWidth =  a.getDimension(R.styleable.FlashBox_boxShadowWidth, 60.0F);
            this.mBoxAnimDelay = a.getInt(R.styleable.FlashBox_boxAnimDelay, 1000);
            a.recycle();
        }
        init();
    }

    private void init() {
        this.mBoxPaint = new Paint(Paint.HINTING_ON);
        this.mBoxPaint.setStyle(Style.STROKE);
        this.mBoxPaint.setColor(this.mBoxColor);
        this.mBoxPaint.setStrokeWidth(this.mBoxStrokeWidth);
        this.mBoxShadowPaint = new Paint(Paint.HINTING_ON);
        this.mBoxShadowPaint.setStyle(Style.STROKE);
        this.mBoxShadowPaint.setStrokeWidth(this.mBoxShadowWidth);
        setWillNotDraw(false);
        setVisibility(View.GONE);
    }

    private void refresh(float paramFloat) {
        setAlpha(paramFloat);
    }

    public boolean isFlashing() {
        return this.isFlashing;
    }

    public void startFlash() {
        if (this.isFlashing) {
            return;
        }
        this.isFlashing = true;
        setVisibility(View.VISIBLE);
        float[] tmp25_24 = new float[3];
        tmp25_24[0] = 0.0F;
        tmp25_24[1] = 1.0F;
        tmp25_24[2] = 0.0F;
        this.mFlashAnimator = ValueAnimator.ofFloat(tmp25_24);
        this.mFlashAnimator.setDuration(1800L);
        this.mFlashAnimator.setInterpolator(new LinearInterpolator());
        this.mFlashAnimator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator paramAnonymousAnimator) {
            }

            public void onAnimationEnd(Animator paramAnonymousAnimator) {
                if (paramAnonymousAnimator != null && mFlashAnimator!=null) {
                    mFlashAnimator.setStartDelay( mBoxAnimDelay);
                    mFlashAnimator.start();
                }
            }

            public void onAnimationCancel(Animator paramAnonymousAnimator) {
            }

            public void onAnimationRepeat(Animator paramAnonymousAnimator) {
            }
        });
    mFlashAnimator.addUpdateListener(new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            float alpha = (float) animation.getAnimatedValue();
            refresh(alpha);
        }
    });
        mFlashAnimator.start();
    }

    public void cancelFlash() {
        this.isFlashing = false;
        ValueAnimator localValueAnimator;
        if ((localValueAnimator = this.mFlashAnimator) != null) {
            localValueAnimator.cancel();
            this.mFlashAnimator = null;
        }
        if (getVisibility() == View.VISIBLE) {
            setVisibility(View.GONE);
        }
    }

    protected void onLayout(boolean paramBoolean, int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
        super.onLayout(paramBoolean, paramInt1, paramInt2, paramInt3, paramInt4);
        if (this.mLeftBorderGradient == null) {
//            paramBoolean = this.mBoxShadowWidth;
//            paramInt1 = this.mBoxShadowColor;
//            paramInt2 = Shader.TileMode.CLAMP;
            this.mLeftBorderGradient = new LinearGradient(0.0F, 0.0F, mBoxShadowWidth, 0.0F, mBoxShadowColor, 0, Shader.TileMode.CLAMP);
        }
        if (this.mTopBorderGradient == null) {
//            paramBoolean = this.mBoxShadowWidth;
//            paramInt1 = this.mBoxShadowColor;
//            paramInt2 = Shader.TileMode.CLAMP;
            this.mTopBorderGradient = new LinearGradient(0.0F, 0.0F, 0.0F, mBoxShadowWidth, mBoxShadowColor, 0, Shader.TileMode.CLAMP);
        }
        if (this.mRightBorderGradient == null) {
//            paramBoolean = getWidth() - this.mBoxShadowWidth;
//            paramInt1 = this.mBoxShadowColor;
//            paramInt2 = Shader.TileMode.CLAMP;
            this.mRightBorderGradient = new LinearGradient(getWidth(), 0.0F, getWidth() - this.mBoxShadowWidth, 0.0F, mBoxShadowColor, 0, Shader.TileMode.CLAMP);
        }
        if (this.mBottomBorderGradient == null) {
//            paramBoolean = getHeight();
//            this = getHeight() - this.mBoxShadowWidth;
//            paramInt1 = this.mBoxShadowColor;
//            paramInt2 = Shader.TileMode.CLAMP;
            this.mBottomBorderGradient = new LinearGradient(0.0F, getHeight(), 0.0F, getHeight() - this.mBoxShadowWidth, mBoxShadowColor, 0, Shader.TileMode.CLAMP);
        }
    }

    protected void onDraw(Canvas paramCanvas) {
        super.onDraw(paramCanvas);
        this.mBoxShadowPaint.setShader(this.mLeftBorderGradient);
        float tmp39_38 = (this.mBoxShadowWidth / 2.0F);
        Paint localPaint1 = this.mBoxShadowPaint;
        paramCanvas.drawLine(tmp39_38, 0.0F, tmp39_38, getHeight(), localPaint1);
        this.mBoxShadowPaint.setShader(this.mTopBorderGradient);
        float f1 = getWidth();
        float f2 = this.mBoxShadowWidth / 2.0F;
        Paint localPaint3 = this.mBoxShadowPaint;
        paramCanvas.drawLine(0.0F, mBoxShadowWidth / 2.0F, f1, f2, localPaint3);
        this.mBoxShadowPaint.setShader(this.mRightBorderGradient);
        f1 = getHeight();
        Paint localPaint2 = this.mBoxShadowPaint;
        paramCanvas.drawLine(getWidth() - this.mBoxShadowWidth / 2.0F, 0.0F, getWidth() - this.mBoxShadowWidth / 2.0F, f1, localPaint2);
        this.mBoxShadowPaint.setShader(this.mBottomBorderGradient);
        f1 = getWidth();
        float f3 = getHeight() - this.mBoxShadowWidth / 2.0F;
        localPaint3 = this.mBoxShadowPaint;
        paramCanvas.drawLine(0.0F, getHeight() - this.mBoxShadowWidth / 2.0F, f1, f3, localPaint3);
        float tmp212_211 = (this.mBoxStrokeWidth / 2.0F);
        paramCanvas.drawRect(tmp212_211, tmp212_211, getWidth() - this.mBoxStrokeWidth / 2.0F, getHeight() - this.mBoxStrokeWidth / 2.0F, this.mBoxPaint);
    }
}
