package com.jiangdg.mediacodec4mp4;

import java.util.List;

public interface FaceDetectorListener {
    enum ErrorCode{
     OutCameraWarning,//出境警告
        TooManyFacesWarning,//同框人脸太多警告
        CompareFailWarning,//比对失败警告
        LiveDetectWarning,//活体检测失败警告
        CoverEyesWarning,//眼睛遮挡警告
        TooFarAwayWarning,//距离太远警告
        TooCloseWarning,//距离太近警告

        OutCameraTooLongError,//出境太久错误
        OutCameraManyTimesError,//出境次数太多错误
        CompareFailError,//人脸比对错误
        LiveDetectFailError,//活体检测错误
    }

    void onError(List<ErrorCode> errorCodes);
}
