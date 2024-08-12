package com.aronc.aisllibrary.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

import com.jiangdg.mediacodec4mp4.tflite.SimilarityClassifier;
import com.jiangdg.mediacodec4mp4.utils.Size;

import java.util.List;

public class MaskView extends View {


    private Paint mPaint;
    private RectF mScanRect;
    private Paint mFacePaint;
    private RectData[] mFaceRects;
    private final int okColor = Color.rgb(76, 155, 87);
    private final int errorColor = Color.RED;

    private boolean isUpdate=false;

    public MaskView(Context context) {
        super(context);
        init();
    }

    public MaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskView(Context context,  AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setAlpha(120);
        mFacePaint = new Paint();
        mFacePaint.setColor(okColor);
        mFacePaint.setAlpha(200);
        mFacePaint.setStrokeWidth(10);
        mFacePaint.setStyle(Paint.Style.STROKE);
        mFaceRects = new RectData[0];
    }

    public void setScanRect(RectF rect) {
        this.mScanRect = rect;
    }

    public RectF getScanRect() {
        return this.mScanRect;
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (mScanRect == null || isUpdate) {
            int padding = 0;
            this.mScanRect = new RectF(padding, padding, getWidth() - padding, getHeight() - padding);
            setUpdate(false);
        }
        canvas.save();
        canvas.clipRect(this.mScanRect, Region.Op.DIFFERENCE);
        canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
        canvas.restore();

        for (int i = 0; i < mFaceRects.length; i++) {

            if (this.mScanRect.contains(mFaceRects[i].rectF)) {

                mFacePaint.setColor(mFaceRects[i].color);
            } else {
                mFacePaint.setColor(errorColor);
            }
            canvas.drawRect(mFaceRects[i].rectF, mFacePaint);
        }

    }

    public void setFaceRect(Size bitmapSize, List<SimilarityClassifier.Recognition> rects) {

        this.mFaceRects = new RectData[rects.size()];


        float sHorizontal = 1.0f * getWidth() / bitmapSize.getWidth();
        float sVertical = 1.0f * getHeight() / bitmapSize.getHeight();

        Matrix matrix = new Matrix();
        matrix.setScale(sHorizontal, sVertical);


        for (int i = 0; i < mFaceRects.length; i++) {
            SimilarityClassifier.Recognition r = rects.get(i);
            RectF rect = new RectF();
            matrix.mapRect(rect, r.getLocation());

            RectData rectData = new RectData();
            rectData.rectF = rect;
            rectData.color = r.getColor();

            mFaceRects[i] = rectData;
        }


        invalidate();
    }

    public void setUpdate(boolean update) {
        isUpdate = update;
    }

    public static class RectData {
        RectF rectF;
        int color;
    }

}
