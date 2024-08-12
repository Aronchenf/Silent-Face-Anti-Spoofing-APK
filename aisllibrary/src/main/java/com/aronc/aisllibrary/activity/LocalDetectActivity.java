package com.aronc.aisllibrary.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aronc.aisllibrary.R;
import com.aronc.aisllibrary.utils.FileUtils;
import com.aronc.aisllibrary.utils.ImageUtil;
import com.aronc.aisllibrary.widget.FlashBox;
import com.aronc.aisllibrary.widget.MaskView;
import com.aronc.aisllibrary.widget.MessageDialog;
import com.aronc.aisllibrary.widget.SurfaceWithMaskView;
import com.jiangdg.mediacodec4mp4.FaceDetectorListener;
import com.jiangdg.mediacodec4mp4.RecordMp4;
import com.jiangdg.mediacodec4mp4.bean.EncoderParams;
import com.jiangdg.mediacodec4mp4.model.AACEncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer;
import com.jiangdg.mediacodec4mp4.mtcnn.Box;
import com.jiangdg.mediacodec4mp4.tflite.SimilarityClassifier;
import com.jiangdg.mediacodec4mp4.utils.CameraManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * 本地人脸检测，包含人脸在框、多人脸、活体、眼部遮挡
 *
 * @author Aroncf
 */

public class LocalDetectActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private final String TAG = "LocalDetectActivity";
    private RecordMp4 mRecMp4;
    private SurfaceView mPreview;
    private MaskView mMaskView;
    private TextView titleView, errorTextView;
    private FlashBox mFlashBox;
    private String savedVideoFilePath;
    private MessageDialog myDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_detect);
        init();
        startRecord();
    }

    /**
     * 初始化
     * 1.初始化控件
     * 2.设置本地人脸比对图片
     */
    private void init() {
        initView();
        savedVideoFilePath = FileUtils.getTempPath(this) + "/record_file.mp4";
        //初始化引擎
        mRecMp4 = new RecordMp4();
        mRecMp4.init(this);
        mRecMp4.setFaceDetectorListener(new FaceDetectorListener() {
            @Override
            public void onError(List<ErrorCode> errorCodes) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFlashBox != null && !mFlashBox.isFlashing()) {
                            mFlashBox.startFlash();
                        }
                        if (errorCodes.size() == 0) {
                            cancelFlashing();
                            return;
                        }
                        if (errorCodes.contains(ErrorCode.OutCameraWarning)) {
                            showFlashText(4);
                            return;
                        }
                        if (errorCodes.contains(FaceDetectorListener.ErrorCode.CompareFailWarning)) {//人脸比对失败警告
                            showFlashText(2);
                            return;
                        }

                        if (errorCodes.contains(FaceDetectorListener.ErrorCode.LiveDetectWarning)) {//活体检测失败警告
                            showFlashText(3);
                            return;
                        }

                        if (errorCodes.contains(FaceDetectorListener.ErrorCode.TooManyFacesWarning)) {//同框人脸过多
                            showFlashText(1);
                            return;
                        }

                        if (errorCodes.contains(FaceDetectorListener.ErrorCode.CoverEyesWarning)) {//眼睛遮挡警告
                            showFlashText(6);
                            return;
                        }

                        if (errorCodes.contains(FaceDetectorListener.ErrorCode.TooFarAwayWarning)) {//距离太远警告
                            showFlashText(5);
                            return;
                        }
                        if (errorCodes.contains(FaceDetectorListener.ErrorCode.TooCloseWarning)) {//距离太近警告
                            showFlashText(7);
                        }
                    }
                });

            }
        });
        mPreview.getHolder().addCallback(this);
        getFaceComparePic();
        mMaskView.setUpdate(true);
    }

    private void showFlashText(int code) {
        String errorMessage = "";
        switch (code) {
            case 1:
                errorMessage = "超出同框人数，注意周边环境";
                break;
            case 2:
                errorMessage = "人脸比对失败，请确保本人操作";
                break;
            case 3:
                errorMessage = "活体检测失败，请确保本人操作";
                break;//活体检测
            case 4:
                errorMessage = "请全程保持头像在框内";
                break;//出境警告
            case 5:
                errorMessage = "请再靠近一些";
                break;//请再靠近一些
            case 6:
                errorMessage = "请勿闭眼或者遮挡眼睛";
                break;//遮挡眼部警告
            case 7:
                errorMessage = "请离镜头稍远一点";
                break;//请离镜头稍远一点
            case 8:
                errorMessage = "当前无法连接网络，请检查网络连接。";
                break;

        }
        errorTextView.setText(errorMessage);
        mRecMp4.setThreshold(0.6f);
        mRecMp4.setLiveThreshold(0.915f);
        myDialog = new MessageDialog(this).builder();
        myDialog.setGone().setTitle("提示").setMsg("").setCancelable(false);
    }

    //取消红框闪烁
    private void cancelFlashing() {
        if (mFlashBox != null) {
            mFlashBox.cancelFlash();
        }
    }

    private void initView() {
        SurfaceWithMaskView surfaceWithMaskView = findViewById(R.id.surfaces_view);
        mPreview = surfaceWithMaskView.getSurfaceView();
        mMaskView = surfaceWithMaskView.getMaskView();
        titleView = findViewById(R.id.title);
        ImageView imv_back = findViewById(R.id.imbtn_back);
        imv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backEvent();
            }
        });
        mFlashBox = findViewById(R.id.flashBox);
        errorTextView = findViewById(R.id.tv_error_prompt);
    }

    /**
     * 设置人脸比对的基底
     */
    private void getFaceComparePic() {
        Intent data = getIntent();
        if (data != null) {
            String picPath = data.getStringExtra("picPath");
            File file = new File(picPath);
            if (!file.exists()) {
                return;
            }
            FileInputStream fileInputStream = null;
            try {
                if (mRecMp4 != null) {
                    fileInputStream = new FileInputStream(file);
                    final Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
                    ImageUtil.saveFrameImage(this, bitmap, "1111111", "hhh");
                    Vector<Box> faces = mRecMp4.getMtcnn().detectFaces(bitmap, bitmap.getWidth() / 5);
                    if (faces.size() == 0) {
                        return;
                    }
                    Rect rect = faces.get(0).transform2Rect();
                    Bitmap face = Bitmap.createBitmap(bitmap,
                            rect.left,
                            rect.top,
                            rect.width(),
                            rect.height());
                    final List<SimilarityClassifier.Recognition> resultAux = mRecMp4.getTFLiteDetector().recognizeImage(face, true);
                    mRecMp4.getTFLiteDetector().register("self", resultAux.get(0));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            finish();
        }
    }

    private void backEvent() {
        if (myDialog != null && !myDialog.isShowing()) {
            myDialog.setMsg("是否退出录制?")
                    .setCancelable(false)
                    .setNegativeButton("退出", v -> {
                        if (mRecMp4 != null) mRecMp4.stopRecord();
                        myDialog.dismiss();
                        finish();
                    }).setPositiveButton("继续录制", v -> myDialog.dismiss())
                    .show();
        }
    }

    private void startRecord() {
        mRecMp4.setEncodeParams(getEncodeParams());
        mRecMp4.startRecord();
    }

    private void stopRecord() {
        if (mRecMp4 != null) {
            mRecMp4.stopRecord();
            mRecMp4.stopCamera();
        }
    }

    @Override
    public void onBackPressed() {
        backEvent();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (mRecMp4 != null) {
            mRecMp4.getmCamManager().setisFrontCamera(true);
            mRecMp4.startCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    private EncoderParams getEncodeParams() {
        File file = new File(savedVideoFilePath);
        if (file.exists()) {
            file.delete();
        }
        EncoderParams params = new EncoderParams();
        params.setVideoPath(savedVideoFilePath);
        params.setFrameWidth(CameraManager.PREVIEW_WIDTH);
        params.setFrameHeight(CameraManager.PREVIEW_HEIGHT);
        params.setBitRateQuality(H264EncodeConsumer.Quality.LOW);
        params.setFrameRateDegree(H264EncodeConsumer.FrameRate._20fps);
        params.setAudioBitrate(AACEncodeConsumer.DEFAULT_BIT_RATE);
        params.setAudioSampleRate(AACEncodeConsumer.DEFAULT_SAMPLE_RATE);
        params.setAudioChannelConfig(AACEncodeConsumer.CHANNEL_IN_MONO);
        params.setAudioChannelCount(AACEncodeConsumer.CHANNEL_COUNT_MONO);
        params.setAudioFormat(AACEncodeConsumer.ENCODING_PCM_16BIT);
        params.setAudioSouce(AACEncodeConsumer.SOURCE_MIC);
        return params;
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelFlashing();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecMp4 != null) {
            mRecMp4.setFaceDetectorListener(null);
            stopRecord();
            mRecMp4 = null;
        }


    }
}
