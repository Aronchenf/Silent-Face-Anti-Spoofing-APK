package com.aronc.aisllibrary.utils;

import android.content.Context;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogUtils {
    private static class Singleton {
        private static LogUtils instance = new LogUtils();
    }

    public static LogUtils getInstance() {
        return Singleton.instance;
    }


    private ExecutorService singleThread;

    private LogUtils() {
        if (singleThread == null) {
            singleThread = Executors.newCachedThreadPool();
        }

    }

    public void saveLog(Context context, String tag, String content) {
        String filePath = FileUtils.getTempPath(context) + File.separator + "log.txt";
        singleThread.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(filePath);
                    String result = String.format("%s===>%s====>%s\n", TimeUtils.getCurTimeStr(), tag, content);
                    RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                    raf.seek(file.length());
                    raf.write(result.getBytes());
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
