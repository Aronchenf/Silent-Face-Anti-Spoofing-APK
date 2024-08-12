package com.aronc.aisllibrary.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.widget.FrameLayout;


public class SurfaceWithMaskView extends FrameLayout {

    private SurfaceView mSurface;
    private MaskView mMaskView;

    public SurfaceWithMaskView(Context context) {
        super(context);
        init();
    }

    public SurfaceWithMaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SurfaceWithMaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mSurface = new SurfaceView(getContext());
        mMaskView = new MaskView(getContext());
        addView(mSurface, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(mMaskView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    }

    public SurfaceView getSurfaceView() {
        return mSurface;
    }

    public MaskView getMaskView() {
        return mMaskView;
    }

}
