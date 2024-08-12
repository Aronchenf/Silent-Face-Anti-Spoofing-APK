package com.jiangdg.mediacodec4mp4.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

/**
 * Camera操作封装类
 * Created by jiangdongguo on 2017/5/6.
 */
public class CameraManager {
    private byte[] cameraData;
    private static final String TAG = "CameraManager";
    public static int PREVIEW_WIDTH = 1280;
    public static int PREVIEW_HEIGHT = 720;
    public static boolean isUsingYv12 = false;
    private Camera mCamera;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    private boolean isFrontCamera = true;
    private OnPreviewFrameResult mPreviewListener;
    private WeakReference<SurfaceHolder> mHolderRef;
//	private CallBack mCallBack;   //自定义的回调

    //旋转预览方向
    int rotateDegree;

    @SuppressLint("StaticFieldLeak")
    private static CameraManager mCameraManager;
    private Matrix frameToCropTransform;
    private Bitmap croppedBitmap;
    private Bitmap rgbFrameBitmap;

//    private boolean isFlyme = HardwareUtils.isFlyme();

    private CameraManager() {
    }


    public interface OnPreviewFrameResult {
        void onPreviewResult(byte[] data, Camera camera);
    }

    public interface OnCameraFocusResult {
        void onFocusResult(boolean result);
    }

    public interface OnTakePictureResultListener {
        void onTakeResult(String path);
    }

    public static CameraManager getCamManagerInstance(Context mContext) {
        CameraManager.mContext = mContext;
        if (mCameraManager == null) {
            mCameraManager = new CameraManager();
        }
        return mCameraManager;
    }

    //将预览数据回传到onPreviewResult方法中
    private PreviewCallback previewCallback = new PreviewCallback() {
        private boolean rotate = false;

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            mPreviewListener.onPreviewResult(data, camera);

//			mCallBack.onPreviewFrame(data);
        }
    };

    public Bitmap getRgbFrameBitmap() {
        return rgbFrameBitmap;
    }

    public Bitmap getCroppedBitmap() {
        return croppedBitmap;
    }

    public byte[] getCameraData() {
        return cameraData;
    }

    public void setCameraData(byte[] cameraData) {
        this.cameraData = cameraData.clone();
    }

    public Matrix getFrameToCropTransform() {
        return frameToCropTransform;
    }

    public void setOnPreviewResult(OnPreviewFrameResult mPreviewListener) {
        this.mPreviewListener = mPreviewListener;
    }

    public void setSurfaceHolder(SurfaceHolder mSurfaceHolder) {
        if (mHolderRef != null) {
            mHolderRef.clear();
            mHolderRef = null;
        }
        mHolderRef = new WeakReference<SurfaceHolder>(mSurfaceHolder);
    }

    public void startPreview() {
        if (mCamera == null) {
            return;
        }
        //设定预览控件
        try {
            Log.i(TAG, "CameraManager-->开始相机预览;" + System.currentTimeMillis());
            mCamera.setPreviewDisplay(mHolderRef.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //开始预览Camera
        try {
            mCamera.startPreview();
        } catch (RuntimeException e) {
            Log.i(TAG, "相机预览失败，重新启动Camera." + System.currentTimeMillis());
            stopPreivew();
            destoryCamera();
            createCamera();
            startPreview();
        }
        //自动对焦
        mCamera.autoFocus(null);
        //设置预览回调缓存
        int previewFormat = mCamera.getParameters().getPreviewFormat();
        Size previewSize = mCamera.getParameters().getPreviewSize();
        int size = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(previewFormat) / 8;
        mCamera.addCallbackBuffer(new byte[size]);
        mCamera.setPreviewCallbackWithBuffer(previewCallback);
//		startFaceDetect();

    }
//	public interface CallBack {
//		void onPreviewFrame(byte[] data);
//		void onTakePic(byte[] data);
//		void onFaceDetect(Camera.Face[] faces,int rotateDegree);
//	}
//	private void startFaceDetect() {
//		mCamera.startFaceDetection();
//		mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
//			@Override
//			public void onFaceDetection(Camera.Face[] faces, Camera camera) {
//				mCallBack.onFaceDetect(faces,rotateDegree);
//				Log.d("检测到 ",faces.length+"张人脸");
//			}
//		});
//
//	}


    public void stopPreivew() {
        if (mCamera == null) {
            return;
        }
        try {
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            Log.i(TAG, "CameraManager-->停止相机预览");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createCamera() {
        //创建Camera
//		Log.i(TAG, "createCamera-->开始;"+System.currentTimeMillis());
        openCamera();
//		Log.i(TAG, "openCamera-->结束;"+System.currentTimeMillis());
        setCamParameters();
//		Log.i(TAG, "setCamParameters-->结束;"+System.currentTimeMillis());
    }

    private void openCamera() {
        if (mCamera != null) {
//			Log.i(TAG, "stopPreivew-->开始;"+System.currentTimeMillis());
//			stopPreivew();
//			Log.i(TAG, "stopPreivew-->结束;"+System.currentTimeMillis());
            destoryCamera();
//			Log.i(TAG, "destoryCamera-->结束;"+System.currentTimeMillis());
            Log.i(TAG, "destoryCamera-->结束;");
        }
        //true 打开前置摄像头  false
        if (isFrontCamera) {
            CameraInfo cameraInfo = new CameraInfo();
            int camNums = Camera.getNumberOfCameras();
            for (int i = 0; i < camNums; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        mCamera = Camera.open(i);
                        Log.i(TAG, "CameraManager-->创建Camera对象，开启前置摄像头;" + System.currentTimeMillis());
                        break;
                    } catch (Exception e) {
                        Log.d(TAG, "打开前置摄像头失败：" + e.getMessage());
                    }
                }
            }
        } else {
            try {
                mCamera = Camera.open();
                Log.i(TAG, "CameraManager-->创建Camera对象，开启后置摄像头;" + System.currentTimeMillis());
            } catch (Exception e) {
                Log.d(TAG, "打开后置摄像头失败：" + e.getMessage());
            }
        }
    }

    public void destoryCamera() {
        if (mCamera == null) {
            return;
        }
//		try {
//			mCamera.setPreviewDisplay(null);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.release();
        mCamera = null;
        Log.i(TAG, "CameraManager-->释放相机资源");
    }

    private void setCamParameters() {
        if (mCamera == null)
            return;

        Camera.Parameters params = mCamera.getParameters();
        if (isUsingYv12) {
            params.setPreviewFormat(ImageFormat.YV12);
        } else {
            params.setPreviewFormat(ImageFormat.NV21);
        }
        //开启自动对焦
        List<String> focusModes = params.getSupportedFocusModes();
        if (isSupportFocusAuto(focusModes)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        //设置预览分辨率，问题出在这里
        List<Size> previewSizes = params.getSupportedPreviewSizes();
        Size previewSize = getOptimalPreviewSize(previewSizes, 1280, 720);
        PREVIEW_WIDTH = previewSize.width;
        PREVIEW_HEIGHT = previewSize.height;
        Log.e(TAG, "setCamParameters: !isSupportPreviewSize:" + PREVIEW_WIDTH + ": " + PREVIEW_HEIGHT);

//        Camera.Size nearSize = getNearlySize(previewSizes, (float) 16 / 9);
////        Camera.Size previewSize = getOptimalPreviewSize(previewSizes, 640, 480);
//        if (nearSize.width > 1280) {
//            PREVIEW_WIDTH = 1280;
//            PREVIEW_HEIGHT = 720;
//        } else {
//            PREVIEW_WIDTH = nearSize.width;
//            PREVIEW_HEIGHT = nearSize.height;
//        }

        Log.d(TAG, "setCamParameters: !isSupportPreviewSize:" + PREVIEW_WIDTH + ": " + PREVIEW_HEIGHT);

        params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);

        //设置预览的最大、最小像素
        int[] max = determineMaximumSupportedFramerate(params);
        params.setPreviewFpsRange(max[0], max[1]);
        //使参数配置生效
        mCamera.setParameters(params);

        //旋转预览方向
        rotateDegree = getPreviewRotateDegree();
        mCamera.setDisplayOrientation(rotateDegree);


        int targetW, targetH;
        if (rotateDegree == 90 || rotateDegree == 270) {
            targetH = PREVIEW_WIDTH;
            targetW = PREVIEW_HEIGHT;
        } else {
            targetW = PREVIEW_WIDTH;
            targetH = PREVIEW_HEIGHT;
        }

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        PREVIEW_WIDTH, PREVIEW_HEIGHT,
                        targetW, targetH,
                        rotateDegree, false);

        rgbFrameBitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);

//		mCamera.unlock();
    }

//    public void cameraFocus(final OnCameraFocusResult listener) {
//        if (mCamera != null) {
//            mCamera.autoFocus(new AutoFocusCallback() {
//                @Override
//                public void onAutoFocus(boolean success, Camera camera) {
//                    if (listener != null) {
//                        listener.onFocusResult(success);
//                    }
//                }
//            });
//        }
//    }

    private int getPreviewRotateDegree() {
        int phoneDegree = 0;
        int result = 0;
        //获得手机方向
        int phoneRotate = ((Activity) mContext).getWindowManager().getDefaultDisplay().getOrientation();
        //得到手机的角度
        switch (phoneRotate) {
            case Surface.ROTATION_0:
                phoneDegree = 0;
                break;     //旋转90度
            case Surface.ROTATION_90:
                phoneDegree = 90;
                break;    //旋转0度
            case Surface.ROTATION_180:
                phoneDegree = 180;
                break;//旋转270
            case Surface.ROTATION_270:
                phoneDegree = 270;
                break;//旋转180
        }
        //分别计算前后置摄像头需要旋转的角度
        CameraInfo cameraInfo = new CameraInfo();
        if (isFrontCamera) {
            Camera.getCameraInfo(CameraInfo.CAMERA_FACING_FRONT, cameraInfo);
            result = (cameraInfo.orientation + phoneDegree) % 360;
            result = (360 - result) % 360;
        } else {
            Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, cameraInfo);
            result = (cameraInfo.orientation - phoneDegree + 360) % 360;
        }
        return result;
    }

    private boolean isSupportFocusAuto(List<String> focusModes) {
        boolean isSupport = false;
        for (String mode : focusModes) {
            if (mode.equals(Camera.Parameters.FLASH_MODE_AUTO)) {
                isSupport = true;
                break;
            }
        }
        return isSupport;
    }

    private boolean isSupportPreviewSize(List<Size> previewSizes) {
        boolean isSupport = false;
        for (Size size : previewSizes) {
            if ((size.width == PREVIEW_WIDTH && size.height == PREVIEW_HEIGHT)
                    || (size.width == PREVIEW_HEIGHT && size.height == PREVIEW_WIDTH)) {
                isSupport = true;
                break;
            }
        }
        return isSupport;
    }

    public void switchCamera() {
        isFrontCamera = !isFrontCamera;
        createCamera();
        Log.i(TAG, "startPreview-->开始;" + System.currentTimeMillis());
        startPreview();
        Log.i(TAG, "startPreview-->结束;" + System.currentTimeMillis());

    }

    public void modifyPreviewSize(int width, int height) {
        PREVIEW_WIDTH = width;
        PREVIEW_HEIGHT = height;
    }

    public int getPreviewFormat() {
        if (mCamera == null) {
            return -1;
        }
        return mCamera.getParameters().getPreviewFormat();
    }

    public Camera getCameraIntance() {
        return mCamera;
    }

    public SurfaceHolder getSurfaceHolder() {
        if (mHolderRef == null) {
            return null;
        }
        return mHolderRef.get();
    }

    public boolean getCameraDirection() {
        return isFrontCamera;
    }

    public static int[] determineMaximumSupportedFramerate(Camera.Parameters parameters) {
        int[] maxFps = new int[]{0, 0};
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext(); ) {
            int[] interval = it.next();
            if (interval[1] > maxFps[1] || (interval[0] > maxFps[0] && interval[1] == maxFps[1])) {
                maxFps = interval;
            }
        }
        return supportedFpsRanges.get(0);
    }

    public boolean getIsFrontCamera() {
        return this.isFrontCamera;
    }

    public void setisFrontCamera(boolean isFrontCamera) {
        this.isFrontCamera = isFrontCamera;
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;
        Size optimalSize = null;
        double minDIff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDIff) {
                optimalSize = size;
                minDIff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDIff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDIff) {
                    optimalSize = size;
                    minDIff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * 筛选 接近 目标高度的16:9的比例
     * 因为16:9的预览效果最好
     *
     * @param sizeList 预览分辨率列表
     * @param targetRatio 目标宽高比
     * @return 最接近目标宽高比的size
     */
    private Size getNearlySize(List<Size> sizeList, float targetRatio) {
        if (sizeList == null) {
            return null;
        }

        int index = 0;//目标索引

        for (int i = 0; i < sizeList.size(); i++) {
            Size size = sizeList.get(i);

            if (size.width > size.height && size.width >= 640 && size.height >= 480) {
                float ratio = (float) size.width / size.height;
                Log.d(TAG, "getNearlySize: Width:" + size.width + "   Height: " + size.height + "  ratio: " + ratio);
                if (Math.abs(ratio - targetRatio) < 0.05) {
                    index = i;
                }
            }
        }

        Log.d(TAG, "getNearlySize: Out Width:" + sizeList.get(index).width + "  TargetHeight: " + sizeList.get(index).height);
        return sizeList.get(index);
    }

}
