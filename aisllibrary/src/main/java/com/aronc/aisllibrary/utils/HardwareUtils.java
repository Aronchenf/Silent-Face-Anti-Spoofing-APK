package com.aronc.aisllibrary.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * 作者：linchangan on 4/7/21 6:14 PM
 * 邮箱：linchangan@apexsoft.com.cn
 */
public class HardwareUtils {
    private static final String TAG = "HardwareUtils";

    /**
     * 判断摄像头是否可用
     *
     * @return true 可用,false 不可用
     */
    public static boolean checkCameraEnable() {
        boolean result;
        Camera camera = null;
        try {
            camera = Camera.open();
            if (camera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                boolean connected = false;
                for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                    Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                    try {
                        camera = Camera.open(camIdx);
                        connected = true;
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                    }
                    if (connected) {
                        break;
                    }
                }
            }
            List<Camera.Size> supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
            result = supportedPreviewSizes != null;
            /* Finally we are ready to start the preview */
            Log.d(TAG, "startPreview");
            camera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
            result = false;
        } finally {
            if (camera != null) {
                camera.release();
            }
        }
        return result;
    }


    /**
     * 判断麦克风是否可用
     *
     * @return true 没有被占用,false 被占用
     */
    public static boolean validateMicAvailability() {
        Boolean available = true;
        AudioRecord recorder =
                new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_DEFAULT, 44100);
        try {
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                available = false;

            }

            recorder.startRecording();
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
                available = false;

            }
            recorder.stop();
        } catch (Exception e) {
            available = false;
        } finally {
            recorder.release();
            recorder = null;
        }

        return available;
    }

    /**
     * 获取耳机的连接状态
     *
     * @return 根据返回的int值进行自己的逻辑操作
     * 1--有线耳机处于连接状态 2--蓝牙耳机处于连接状态 3--无耳机连接
     */
    public static int getHeadSetStatus(Context context) {
        //判断有线耳机是否连接

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE); //获取声音管理器

        if (audioManager.isWiredHeadsetOn()) {  //有线耳机是否连接
            return 2;
        } else {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();  //蓝牙耳机
            if (adapter.isEnabled()) {
                int a2dp = adapter.getProfileConnectionState(BluetoothProfile.A2DP);
                int headset = adapter.getProfileConnectionState(BluetoothProfile.HEADSET);
                int health = adapter.getProfileConnectionState(BluetoothProfile.HEALTH);
                if (BluetoothProfile.STATE_CONNECTED == a2dp) {
                    return 1;
                } else if (BluetoothProfile.STATE_CONNECTED == headset) {
                    return 1;
                } else if (BluetoothProfile.STATE_CONNECTED == health) {
                    return 1;
                } else {
                    return 3;
                }
            } else {
                return 3;
            }
        }


    }

    //外放
    public static void loudSpeaker(Activity context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);
        context.setVolumeControlStream(0);
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    //内放
    public static void microSpeaker(Activity context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(false);
        context.setVolumeControlStream(0);
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }
}
