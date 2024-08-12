package com.aronc.aisllibrary.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtil {

    private static final String TAG = "ImageUtil";

    public static String saveFrameImage(Context context, Bitmap bitmap, String apexId, String name) {
        File dirRoot = new File(FileUtils.getFrameFolderPathWithApexId(context, apexId));
        if (FileUtils.createOrExistsDir(dirRoot)) {
            Log.d(TAG, "create frame folder Success! ");
        }

        File imageFile = new File(dirRoot, name + ".png");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
