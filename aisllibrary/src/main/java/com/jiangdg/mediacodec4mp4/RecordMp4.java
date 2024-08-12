package com.jiangdg.mediacodec4mp4;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.Nullable;

import com.jiangdg.mediacodec4mp4.bean.EncoderParams;
import com.jiangdg.mediacodec4mp4.model.AACEncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer;
import com.jiangdg.mediacodec4mp4.model.MediaMuxerUtil;
import com.jiangdg.mediacodec4mp4.mtcnn.Box;
import com.jiangdg.mediacodec4mp4.mtcnn.MTCNN;
import com.jiangdg.mediacodec4mp4.tflite.SimilarityClassifier;
import com.jiangdg.mediacodec4mp4.tflite.TFLiteObjectDetectionAPIModel;
import com.jiangdg.mediacodec4mp4.utils.CameraManager;
import com.jiangdg.mediacodec4mp4.utils.ImageUtils;
import com.jiangdg.mediacodec4mp4.utils.SensorAccelerometer;
import com.mv.live.EngineWrapper;

import org.easydarwin.sw.JNIUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 录制实现类
 * <p>
 * Created by jianddongguo on 2017/7/20.
 */

public class RecordMp4 {
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final boolean DEBUG = false;
    private static final String TAG = "RecordMp4";
    public AACEncodeConsumer mAacConsumer;
    private H264EncodeConsumer mH264Consumer;
    private MediaMuxerUtil mMuxer;
    private EncoderParams mParams;
    private SensorAccelerometer mSensorAccelerometer;
    public CameraManager mCamManager;

    private int mDegree = 0;

    private boolean mCameraFrameReady;
    private final Object lock = new Object();
    private boolean mStopThread;
    private int[] rgbBytes;
    private FaceDetectorListener mFaceDetectorListener;
    private Bitmap faceBmp;
    private SimilarityClassifier detector;
    private Context mContext;

    private long liveDetectLastTime;
    private long outCameraTooLongLastTime;
    private long outCameraLastTime;
    private long compareLastTime;
    private long eyesCoverLastTime;

//    private FaceDetector faceDetector;

    private float threshold;
    private float liveThreshold;
    private int faceErrorNum;
    /**
     * 录制状态为：true:录制中，false:未录制
     */
    protected boolean isRecording;
    private EngineWrapper engineWrapper;
    private boolean isPrepared;

    private int FACE_COMPARISON_TIME_APP;//APP单次最大人脸比对连续失败时间(单位：秒)
    private int FACE_COMPARISON_TIME_AUDIT;//标准审核单次最大人脸比对连续失败时间(单位：秒)
    private int FACE_COMPARISON_TIME_MAX;//App录制过程中未达失败前单次最大人脸比对连续失败时间(单位：秒)

    private int LIVE_DECTION_TIME_APP;//APP最大活体检测连续失败时间(单位：秒)
    private int LIVE_DECTION_TIME_AUDIT;//标准审核单次最大活体检测连续失败时间(单位：秒)
    private int LIVE_DECTION_TIME_MAX;//APP录制过程中未达失败前单次最大活体检测连续失败时间(单位：秒)

    public void setFaceDetectorListener(FaceDetectorListener faceDetectorListener) {
        this.mFaceDetectorListener = faceDetectorListener;
    }

    private MTCNN mtcnn;

    public RecordMp4() {
        FACE_COMPARISON_TIME_MAX = -1;
        LIVE_DECTION_TIME_MAX = -1;
    }

    // 预览数据处理
    private final CameraManager.OnPreviewFrameResult mPreviewListener = new CameraManager.OnPreviewFrameResult() {
        int getPictime = 0;

        @Override
        public void onPreviewResult(byte[] data, Camera camera) {

            mCamManager.setCameraData(data);

            int width = 480;
            int height = 640;
            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();
                if (parameters != null) {
                    width = parameters.getPreviewSize().width;
                    height = parameters.getPreviewSize().height;
                }
            }

            if (rgbBytes == null) {
                rgbBytes = new int[width * height];
            }
            getPictime++;
            if (getPictime >= 15) {
                getPictime = 0;
                synchronized (lock) {
                    ImageUtils.convertYUV420SPToARGB8888(data, width, height, rgbBytes);
                    mCameraFrameReady = true;
                    lock.notify();
                }
            }

            // 处理1：旋转YUV
            rotateYuv2(data, camera, width, height);
//            long time = new Date().getTime() + DateDiffFromSystemUtils.diffTime;

            // 处理3：yuv转换颜色格式，再编码
            if (mH264Consumer != null) {
                mH264Consumer.addData(data);
            }
            mCamManager.getCameraIntance().addCallbackBuffer(data);

        }
    };

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            liveDetectLastTime = System.currentTimeMillis();
            outCameraLastTime = System.currentTimeMillis();
            compareLastTime = System.currentTimeMillis();
            outCameraTooLongLastTime = System.currentTimeMillis();
            eyesCoverLastTime = System.currentTimeMillis();
            faceErrorNum = 0;
            do {
                boolean hasFrame = false;
                synchronized (lock) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady) {
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!mStopThread && hasFrame && isRecording) {
                    detectFace();
                }
            } while (!mStopThread);
        }
    }

    @SuppressLint("MissingPermission")
    private void detectFace() {

        final List<FaceDetectorListener.ErrorCode> errorCodes = new ArrayList<>();
        byte[] data = mCamManager.getCameraData();
        Bitmap srcBitmap = mCamManager.getRgbFrameBitmap();
        srcBitmap.setPixels(rgbBytes, 0, srcBitmap.getWidth(), 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight());

        final Bitmap croppedBitmap = mCamManager.getCroppedBitmap();
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(srcBitmap, mCamManager.getFrameToCropTransform(), null);

        Vector<Box> boxes = mtcnn.detectFaces(croppedBitmap, croppedBitmap.getWidth() / 5); // 只有这句代码检测

        if (mFaceDetectorListener != null && isRecording) {
            //活体检测
            //一秒缓冲时间
            final long bufferTime = 1000;
            if (isPrepared && data != null) {
                float c = engineWrapper.detect(
                        data, CameraManager.PREVIEW_WIDTH, CameraManager.PREVIEW_HEIGHT, 7);
                Log.d("detectFace:", "w " + CameraManager.PREVIEW_WIDTH + " h " + CameraManager.PREVIEW_HEIGHT + " con: " + c);
                if (c != -1) {
                    if (c > getLiveThreshold()) {
                        liveDetectLastTime = System.currentTimeMillis();
                    } else {
                        if (System.currentTimeMillis() > liveDetectLastTime + bufferTime) {
                            errorCodes.add(FaceDetectorListener.ErrorCode.LiveDetectWarning);
                            sendLogs(generateMessage("活体检测失败", c, getLiveThreshold()), FaceDetectorListener.ErrorCode.LiveDetectWarning);
                            long diff = System.currentTimeMillis() - liveDetectLastTime;
                            if (diff > LIVE_DECTION_TIME_APP * 1000L + bufferTime) {
                                errorCodes.add(FaceDetectorListener.ErrorCode.LiveDetectFailError);
                            } else {
                                LIVE_DECTION_TIME_MAX = (int) (Math.max(diff, LIVE_DECTION_TIME_MAX * 1000L) / 1000);
                            }
                        }
                    }
                }
            }

            //进行人脸数目判断
            //判断出境
            if (boxes.size() < 1) {
                if (System.currentTimeMillis() > outCameraLastTime + bufferTime) {
                    errorCodes.add(FaceDetectorListener.ErrorCode.OutCameraWarning);
                    sendLogs("请全程保持头像在框内", FaceDetectorListener.ErrorCode.OutCameraWarning);
                }
                if (System.currentTimeMillis() - outCameraTooLongLastTime > 2 * 1000L + bufferTime) {//长时间出境
                    errorCodes.add(FaceDetectorListener.ErrorCode.OutCameraTooLongError);
                } else if (System.currentTimeMillis() - outCameraLastTime > 1000L + bufferTime) {//单次出境
                    outCameraLastTime = System.currentTimeMillis();
                    faceErrorNum++;
                    if (faceErrorNum > 3) {
                        errorCodes.add(FaceDetectorListener.ErrorCode.OutCameraManyTimesError);
                    }
                }
            } else {
                outCameraTooLongLastTime = System.currentTimeMillis();
                outCameraLastTime = System.currentTimeMillis();
                if (boxes.size() > 1) {//人脸过多
                    errorCodes.add(FaceDetectorListener.ErrorCode.TooManyFacesWarning);
                    sendLogs("超出同框人数,注意周边环境,(" + boxes.size() + "/1)", FaceDetectorListener.ErrorCode.TooManyFacesWarning);
                }
                //只有一张脸
                final Canvas cvFace = new Canvas(faceBmp);
                Matrix matrix = new Matrix();

                Box face = boxes.get(0);

                Rect r = face.transform2Rect();

                float sx = ((float) 224) / r.width();
                float sy = ((float) 224) / r.height();

                matrix.reset();
                matrix.postTranslate(-r.left, -r.top);
                matrix.postScale(sx, sy);

                cvFace.drawBitmap(croppedBitmap, matrix, null);
                final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, false);

                //人脸比对
                if (resultsAux.size() > 0) {
                    SimilarityClassifier.Recognition result = resultsAux.get(0);
                    float conf = result.getDistance();
                    if (conf != Float.MAX_VALUE && conf >= getThreshold()) {
                        compareLastTime = System.currentTimeMillis();
                    } else {
                        long currTime = System.currentTimeMillis();
                        //添加一个警告
                        if (conf == Float.MAX_VALUE) {
                            conf = 20;
                        }
                        if (currTime - compareLastTime > bufferTime) {
                            errorCodes.add(FaceDetectorListener.ErrorCode.CompareFailWarning);
                            sendLogs(generateMessage("人脸比对失败，请确保本人操作", conf, getThreshold()), FaceDetectorListener.ErrorCode.CompareFailWarning);
                            //添加一个错误
                            long diff = currTime - compareLastTime;
                            if (diff > FACE_COMPARISON_TIME_APP * 1000L + bufferTime) {
                                errorCodes.add(FaceDetectorListener.ErrorCode.CompareFailError);
                            } else {
                                FACE_COMPARISON_TIME_MAX = (int) (Math.max(diff, FACE_COMPARISON_TIME_MAX * 1000L) / 1000);
                            }
                        }
                    }
                }

                //请靠近一些警告
                double rate = face.width() * 1.00 / croppedBitmap.getWidth();
                if (rate < 0.3) {
                    errorCodes.add(FaceDetectorListener.ErrorCode.TooFarAwayWarning);
                    sendLogs(generateMessage("请再靠近一些", rate, 0.3), FaceDetectorListener.ErrorCode.TooFarAwayWarning);
                } else if (rate > 0.80) {
                    errorCodes.add(FaceDetectorListener.ErrorCode.TooCloseWarning);
                    sendLogs(generateMessage("请离镜头稍远一点", rate, 0.80), FaceDetectorListener.ErrorCode.TooCloseWarning);
                }
            }
            //眼部遮挡警告
//            InputImage inputImage = InputImage.fromBitmap(faceBmp, mDegree);
//            faceDetector.process(inputImage).addOnSuccessListener(faces -> {
//                if (faces.size() > 0) {
//                    Face result = faces.get(0);
//                    Log.d(TAG, "onSuccess: left" + result.getLeftEyeOpenProbability() + "  right  " + result.getRightEyeOpenProbability());
//                    if (result.getLeftEyeOpenProbability() != null && result.getRightEyeOpenProbability() != null) {
//                        if (result.getLeftEyeOpenProbability() > 0.50 && result.getRightEyeOpenProbability() > 0.50) {
//                            eyesCoverLastTime = System.currentTimeMillis();
//                        } else {
//                            sendLogs(
//                                    generateMessage(
//                                            "请勿闭眼或者遮挡眼睛",
//                                            Math.min(result.getLeftEyeOpenProbability(), result.getRightEyeOpenProbability()),
//                                            0.50f),
//                                    FaceDetectorListener.ErrorCode.CoverEyesWarning);
//                            long diff = System.currentTimeMillis() - eyesCoverLastTime;
//                            if (diff > 2000)
//                                errorCodes.add(FaceDetectorListener.ErrorCode.CoverEyesWarning);
//                        }
//                    }
//                }
//            });

            if (errorCodes.size() == 0) {
                sendLogs("", null);
            }
            if (mFaceDetectorListener != null)
                mFaceDetectorListener.onError(errorCodes);
        }
    }

    @SuppressLint("DefaultLocale")
    private String generateMessage(String message, double current, double config) {
        return String.format("%s,(%.6f/%.6f)", message, current, config);
    }

    /**
     * 发送人脸识别时候错误的状态日志
     */
    private void sendLogs(String message, @Nullable FaceDetectorListener.ErrorCode code) {
        Log.e(TAG, "sendLogs: " + message);
    }

    public SimilarityClassifier getTFLiteDetector() {
        return detector;
    }

    private void initFaceDetector(Context context) {
        final int TF_OD_API_INPUT_SIZE = 112;
        final boolean TF_OD_API_IS_QUANTIZED = false;
        final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        try {
            faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            mtcnn = new MTCNN(context);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public MTCNN getMtcnn() {
        return mtcnn;
    }

    public void init(Context context) {
        /*
         * 活体检测初始化
         */
        mContext = context;
        engineWrapper = new EngineWrapper(context.getAssets());
        isPrepared = engineWrapper.init();
        // 实例化摄像头管理类
        mCamManager = CameraManager.getCamManagerInstance(context);
        // 实例化加速传感器
        mSensorAccelerometer = SensorAccelerometer.getSensorInstance();
        mSensorAccelerometer.initSensor(context);

        getDgree(context);

        initFaceDetector(context);
//        initEyesDetector();
    }


    //初始化眼部检测
//    private void initEyesDetector() {
//        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
//                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
//                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
//                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
//                .build();
//        faceDetector = FaceDetection.getClient(options);
//    }


    private void rotateYuv2(byte[] data, Camera camera, int width, int height) {
        if (CameraManager.PREVIEW_WIDTH != width || CameraManager.PREVIEW_HEIGHT != height) {
            CameraManager.PREVIEW_WIDTH = width;
            CameraManager.PREVIEW_HEIGHT = height;
            return;
        }
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        if (isFrontCamera()) {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_FRONT, camInfo);
        } else {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);
        }
        int cameraRotationOffset = camInfo.orientation;
        if (cameraRotationOffset % 180 != 0) {
            yuvRotate(data, 1, width, height, cameraRotationOffset);
        }

        //TODO 将角度定位0 保证横屏录制不出问题
//        yuvRotate(data, 1, width, height, 0);
    }

    /**
     * 旋转YUV格式数据
     * <p>
     * src    YUV数据
     * format 0，420P；1，420SP
     * width  宽度
     * height 高度
     * degree 旋转度数
     */
    private static void yuvRotate(byte[] src, int format, int width, int height, int degree) {
        int offset = 0;
        if (format == 0) {
            JNIUtil.rotateMatrix(src, offset, width, height, degree);
            offset += (width * height);
            JNIUtil.rotateMatrix(src, offset, width / 2, height / 2, degree);
            offset += width * height / 4;
            JNIUtil.rotateMatrix(src, offset, width / 2, height / 2, degree);
        } else if (format == 1) {
            JNIUtil.rotateMatrix(src, offset, width, height, degree);
            offset += width * height;
            JNIUtil.rotateShortMatrix(src, offset, width / 2, height / 2, degree);
        }
    }

    public void setEncodeParams(EncoderParams mParams) {
        this.mParams = mParams;
    }

    public void startRecord() {
        if (mParams == null)
            throw new IllegalStateException("EncoderParams can not be null,need call setEncodeParams method!");
        // 判断手机方向
        boolean rotate = false;
        if (mDegree == 0) {
            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(isFrontCamera() ? Camera.CameraInfo.CAMERA_FACING_FRONT
                    : Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);
            int cameraRotationOffset = camInfo.orientation;
            if (cameraRotationOffset == 90) {
                rotate = true;
            } else if (cameraRotationOffset == 270) {
                rotate = true;
            }
        }

        mParams.setVertical(rotate);
        Log.i(TAG, "-------------------->rotate = " + rotate);

        // 创建音视频编码线程
        mH264Consumer = new H264EncodeConsumer();
        mAacConsumer = new AACEncodeConsumer();
        //new File(mParams.getVideoPath(), new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date())).toString()
        mMuxer = new MediaMuxerUtil(mParams.getVideoPath(), 1000000);
        if (mH264Consumer != null) {
            mH264Consumer.setTmpuMuxer(mMuxer, mParams);
        }
        if (mAacConsumer != null) {
            mAacConsumer.setTmpuMuxer(mMuxer, mParams);
        }
        // 配置好混合器后启动线程
        if (mH264Consumer != null) mH264Consumer.start();
        if (mAacConsumer != null) mAacConsumer.start();

        isRecording = true;
    }

    public void stopRecord() {

//        if (faceDetector != null) {
//            faceDetector.close();
//        }

        // 停止混合器
        if (mMuxer != null) {
            mMuxer.release();
            mMuxer = null;
            if (RecordMp4.DEBUG)
                Log.i(TAG, TAG + "---->停止本地录制");
        }
        if (mH264Consumer != null) {
            mH264Consumer.setTmpuMuxer(null, null);
        }
        if (mAacConsumer != null) {
            mAacConsumer.setTmpuMuxer(null, null);
        }
        // 停止视频编码线程
        if (mH264Consumer != null) {
            mH264Consumer.exit();
            try {
                Thread t2 = mH264Consumer;
                mH264Consumer = null;
                if (t2 != null) {
                    t2.interrupt();
                    t2.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 停止音频编码线程
        if (mAacConsumer != null) {
            mAacConsumer.exit();
            try {
                Thread t1 = mAacConsumer;
                mAacConsumer = null;
                if (t1 != null) {
                    t1.interrupt();
                    t1.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isRecording = false;
    }

    public void startCamera(SurfaceHolder surfaceHolder) {
        if (mCamManager == null)
            return;
        mCamManager.setSurfaceHolder(surfaceHolder);
        mCamManager.setOnPreviewResult(mPreviewListener);
        mCamManager.createCamera();
        mCamManager.startPreview();
        startSensorAccelerometer();

        mStopThread = false;
        new Thread(new CameraWorker()).start();
    }

    public void restartCamera() {
        mCamManager.createCamera();
        mCamManager.startPreview();
    }


    public void stopCamera() {
//        System.out.println("stopCamera");
        mStopThread = true;
        synchronized (lock) {
            lock.notify();
        }

        if (mCamManager == null) {
            return;
        }
        //    mCamManager.stopPreivew();
        mCamManager.destoryCamera();
        mCamManager.setOnPreviewResult(null);
        stopSensorAccelerometer();
    }


    public void setPreviewSize(int width, int height) {
        if (mCamManager != null) {
            mCamManager.modifyPreviewSize(width, height);
        }
    }

    public boolean isFrontCamera() {
        return mCamManager != null &&
                mCamManager.getCameraDirection();
    }


    private void startSensorAccelerometer() {
        // 启动加速传感器，注册结果事件监听器
        if (mSensorAccelerometer != null) {
            mSensorAccelerometer.startSensorAccelerometer(new SensorAccelerometer.OnSensorChangedResult() {
                @Override
                public void onStopped() {
                    // 对焦成功，隐藏对焦图标
                }

                @Override
                public void onMoving(int x, int y, int z) {
                }
            });
        }
    }

    private void stopSensorAccelerometer() {
        // 释放加速传感器资源
        if (mSensorAccelerometer == null) {
            return;
        }
        mSensorAccelerometer.stopSensorAccelerometer();
    }

    private void getDgree(Context context) {
        int rotation = ((Activity) context).getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                mDegree = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                mDegree = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                mDegree = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                mDegree = 270;
                break;// Landscape right
        }
    }

    public CameraManager getmCamManager() {
        return this.mCamManager;
    }


    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public float getLiveThreshold() {
        return liveThreshold;
    }

    public void setLiveThreshold(float liveThreshold) {
        this.liveThreshold = liveThreshold;
    }

    public void setRecordStatus(boolean isRecording) {
        this.isRecording = isRecording;
    }

    public boolean isRecording() {
        return isRecording;
    }


    public int getFaceComparisonTimeMax() {
        return FACE_COMPARISON_TIME_MAX;
    }


    public int getLiveDetectionTimeMax() {
        return LIVE_DECTION_TIME_MAX;
    }

    public boolean liveDetectSuccessActually() {
        return LIVE_DECTION_TIME_MAX < LIVE_DECTION_TIME_AUDIT;
    }

}
