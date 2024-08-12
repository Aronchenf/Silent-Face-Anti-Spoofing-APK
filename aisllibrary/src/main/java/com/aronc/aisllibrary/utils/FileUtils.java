package com.aronc.aisllibrary.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.aronc.aisllibrary.model.WaveHeader;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Desc	        ${文件相关工具类}
 */
public final class FileUtils {

    private final static String PATTERN = "yyyyMMddHHmmss";    // 时间戳命名

    /**
     * 得到SD卡根目录，SD卡不可用则获取内部存储的根目录
     */
    public static File getRootPath() {
        File path = null;
        if (sdCardIsAvailable()) {
            path = Environment.getExternalStorageDirectory(); //SD卡根目录    /storage/emulated/0
        } else {
            path = Environment.getExternalStorageDirectory();//内部存储的根目录    /data  //Environment.getDataDirectory()
        }
        return path;
    }

    public static String getTempPath(Context context) {
        return context.getExternalFilesDir("video").getAbsolutePath();
    }

    /**
     * 获取apex相对应的存储文件夹
     */
    public static String getApexIdFolder(Context context, String apexId) {
        return getTempPath(context) + File.separator + "apex_id_" + apexId;
    }

    public static String getVideoPathWithApexId(Context context, String apexId) {
        return getApexIdFolder(context, apexId) + File.separator + "video";
    }

    public static String getFrameFolderPathWithApexId(Context context, String apexId) {
        return getApexIdFolder(context, apexId) + File.separator + "keyFrame";
    }

    /**
     * 获取音频的存储文件夹
     */
    public static String getAudioPath(Context context) {
        return getTempPath(context) + File.separator + "audioSource";
    }

    /**
     * SD卡是否可用
     */
    public static boolean sdCardIsAvailable() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sd = new File(Environment.getExternalStorageDirectory().getPath());
            return sd.canWrite();
        } else
            return false;
    }

    /**
     * 判断目录是否存在，不存在则判断是否创建成功
     *
     * @param dirPath 文件路径
     * @return {@code true}: 存在或创建成功<br>{@code false}: 不存在或创建失败
     */
    public static boolean createOrExistsDir(String dirPath) {
        return createOrExistsDir(getFileByPath(dirPath));
    }

    /**
     * 判断目录是否存在，不存在则判断是否创建成功
     *
     * @param file 文件
     * @return {@code true}: 存在或创建成功<br>{@code false}: 不存在或创建失败
     */
    public static boolean createOrExistsDir(File file) {
        // 如果存在，是目录则返回true，是文件则返回false，不存在则返回是否创建成功
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    /**
     * 判断文件是否存在，不存在则判断是否创建成功
     *
     * @param filePath 文件路径
     * @return {@code true}: 存在或创建成功<br>{@code false}: 不存在或创建失败
     */
    public static boolean createOrExistsFile(String filePath) {
        return createOrExistsFile(getFileByPath(filePath));
    }

    /**
     * 判断文件是否存在，不存在则判断是否创建成功
     *
     * @param file 文件
     * @return {@code true}: 存在或创建成功<br>{@code false}: 不存在或创建失败
     */
    public static boolean createOrExistsFile(File file) {
        if (file == null)
            return false;
        // 如果存在，是文件则返回true，是目录则返回false
        if (file.exists())
            return file.isFile();
        if (!createOrExistsDir(file.getParentFile()))
            return false;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建文件
     *
     * @param context  context
     * @param filePath 文件路径
     * @return file
     */
    public static File createTmpFile(Context context, String filePath) {

        String timeStamp = new SimpleDateFormat(PATTERN, Locale.CHINA).format(new Date());

        String externalStorageState = Environment.getExternalStorageState();

        File dir = new File(Environment.getExternalStorageDirectory() + filePath);

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return new File(dir, timeStamp + ".jpg");
        } else {
            File cacheDir = context.getCacheDir();
            return new File(cacheDir, timeStamp + ".jpg");
        }

    }


    /**
     * 创建初始文件夹。保存拍摄图片和裁剪后的图片
     *
     * @param filePath 文件夹路径
     */
    public static void createFile(String filePath) {
        String externalStorageState = Environment.getExternalStorageState();

        File dir = new File(Environment.getExternalStorageDirectory() + filePath);
        File cropFile = new File(Environment.getExternalStorageDirectory() + filePath + "/crop");

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            if (!cropFile.exists()) {
                cropFile.mkdirs();
            }

            File file = new File(cropFile, ".nomedia");    // 创建忽视文件。   有该文件，系统将检索不到此文件夹下的图片。
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    public static String getFilePath(Context context) {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getPath();
        } else {
            return context.getCacheDir().getAbsolutePath();
        }
    }


    /**
     * @param filePath 文件夹路径
     * @return 截图完成的 file
     */
    public static File getCorpFile(String filePath) {
        String timeStamp = new SimpleDateFormat(PATTERN, Locale.CHINA).format(new Date());
        return new File(Environment.getExternalStorageDirectory() + filePath + "/crop/" + timeStamp + ".jpg");
    }

    /**
     * 根据文件路径获取文件
     *
     * @param filePath 文件路径
     * @return 文件
     */
    public static File getFileByPath(String filePath) {
        return isSpace(filePath) ? null : new File(filePath);
    }

    /**
     * 判断字符串是否为 null 或全为空白字符
     *
     * @param s
     * @return
     */
    private static boolean isSpace(final String s) {
        if (s == null)
            return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 关闭IO
     *
     * @param closeables closeable
     */
    public static void closeIO(Closeable... closeables) {
        if (closeables == null)
            return;
        try {
            for (Closeable closeable : closeables) {
                if (closeable != null) {
                    closeable.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * byte[] 转 file
     *
     * @param bytes
     * @param path
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static void byteToFile(byte[] bytes, String path) {
        try {
            // 根据绝对路径初始化文件
            File localFile = new File(path);
            if (!localFile.exists()) {
                localFile.createNewFile();
            }
            // 输出流
            FileOutputStream os = new FileOutputStream(localFile);
            os.write(bytes, 0, bytes.length);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * byte[] 转 file
     *
     * @param bytes
     * @param path
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static void byteToImage(byte[] bytes, String path) {
        try {
            // 根据绝对路径初始化文件
            File localFile = new File(path);
            if (!localFile.exists()) {
                localFile.createNewFile();
            }

            YuvImage yuvimage = new YuvImage(bytes, ImageFormat.NV21, 480, 640, null);//20、20分别是图的宽度与高度
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, 480, 640), 80, baos);//80--JPG图片的质量[0-100],100最高
            byte[] jdata = baos.toByteArray();

            Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
            // 输出流
            FileOutputStream os = new FileOutputStream(localFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static File checkFolder(String path, String fileName) {
        File tempFolder;
        tempFolder = new File(path, fileName);
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }
        return tempFolder;
    }

    /**
     * 生成随机文件名：当前年月日时分秒+五位随机数
     *
     * @return
     */
    public static String getRandomFileName() {

        SimpleDateFormat simpleDateFormat;

        simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

        Date date = new Date();

        String str = simpleDateFormat.format(date);

        Random random = new Random();

        int rannum = (int) (random.nextDouble() * (99999 - 10000 + 1)) + 10000;// 获取5位随机数

        return rannum + str;// 当前时间
    }

    public static String getFileName(String pathandname) {
        /**
         * 仅保留文件名不保留后缀
         */
        int start = pathandname.lastIndexOf("/");
        int end = pathandname.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return pathandname.substring(start + 1, end);
        } else {
            return null;
        }
    }

    /**
     * 保留文件名及后缀
     */
    public static String getFileNameWithSuffix(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        if (start != -1) {
            return pathandname.substring(start + 1);
        } else {
            return null;
        }
    }

    /**
     * 图片按比例大小压缩方法
     *
     * @param srcPath （根据路径获取图片并压缩）
     * @return
     */
    public static File compressImagePixels(Context context, String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了，只读取图片的大小，不分配内存
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);// 此时返回bm为空
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 现在主流手机比较多是1920*1080分辨率，所以高和宽我们设置为
        float hh = 1920f;// 这里设置高度为1280f
        float ww = 1080f;// 这里设置宽度为720f
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;// 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        //检测并旋转图片
        Bitmap bitmap1 = changeImageLocate(srcPath, bitmap);
        return compressImage(context, bitmap1);// 压缩好比例大小后再进行质量压缩
    }

    //检测并旋转图片
    public static Bitmap changeImageLocate(String filepath, Bitmap bitmap) {
        //根据图片的filepath获取到一个ExifInterface的对象
        int degree = 0;
        try {
            ExifInterface exif = new ExifInterface(filepath);
            if (exif != null) {
                // 读取图片中相机方向信息
                int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Log.e("degree========ori====", ori + "");

                // 计算旋转角度
                switch (ori) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                    default:
                        degree = 0;
                        break;
                }
                Log.e("degree============", degree + "");
                if (degree != 0) {
                    Log.e("degree============", "degree != 0");
                    // 旋转图片
                    Matrix m = new Matrix();
//                    m.setScale(0.5f,0.5f);
                    m.postRotate(degree);

                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);

                    return bitmap;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    //质量压缩
    public static File compressImage(Context context, Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;

        while (baos.toByteArray().length / 1024 > 2048) { // 循环判断如果压缩后图片是否大于1MB,大于继续压缩
            baos.reset(); // 重置baos即清空baos
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;// 每次都减少10
        }
        File dirFile = new File(getTempPath(context) + "/image");
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        for (File file : Objects.requireNonNull(dirFile.listFiles())) {
            if (file.isFile()) {
                file.delete();
            }
        }

        File file = new File(dirFile.getPath(), System.currentTimeMillis() + ".jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(baos.toByteArray());
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        recycleBitmap(bitmap);
        return file;
    }

    //回收Bitmap
    public static void recycleBitmap(Bitmap... bitmaps) {
        if (bitmaps == null) {
            return;
        }
        for (Bitmap bm : bitmaps) {
            if (null != bm && !bm.isRecycled()) {
                bm.recycle();
            }
        }
    }

    /**
     * 文件路径转换为Uri
     */
    public static Uri path2Uri(Context context, String filePath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = context.getApplicationContext().getPackageName();
            String authority = packageName + ".provider";
            return FileProvider.getUriForFile(context, authority, new File(filePath));
        } else {
            return Uri.fromFile(new File(filePath));
        }

    }

    /**
     * 获取视频首帧
     */
    public static Bitmap getFirstBitmap(Context context, String filePath) {
        if (isVideoFileType(filePath)) {
            MediaMetadataRetriever media = new MediaMetadataRetriever();
            media.setDataSource(context, FileUtils.path2Uri(context, filePath));
            return media.getFrameAtTime();
        }
        return null;
    }

    /**
     * 判断文件是否是mp4
     */
    public static boolean isVideoFileType(String filePath) {
        File tempFile = new File(filePath);
        if (tempFile.canRead()) {
            String[] array = filePath.split("\\.");
            return array[array.length - 1].equals("mp4");
        }
        return false;
    }

    /**
     * 删除单个文件
     *
     * @param filePath 被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 删除文件夹以及目录下的文件
     *
     * @param filePath 被删除目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String filePath) {
        boolean flag = false;
        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        //遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                //删除子文件
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } else {
                //删除子目录
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前空目录
        return dirFile.delete();
    }

    /**
     * 根据路径删除指定的目录或文件，无论存在与否
     *
     * @param filePath 要删除的目录或文件
     * @return 删除成功返回 true，否则返回 false。
     */
    public static boolean DeleteFolder(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                // 为文件时调用删除文件方法
                return deleteFile(filePath);
            } else {
                // 为目录时调用删除目录方法
                return deleteDirectory(filePath);
            }
        }
    }

    public static boolean pcm2Wav(Context context, boolean isAudition) {
        String sourceFilePath = FileUtils.getTempPath(context) + File.separator + "aac/video.pcm";
        String targetFilePath = FileUtils.getTempPath(context) + File.separator + "aac/video.wav";

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(sourceFilePath);
            outputStream = new FileOutputStream(targetFilePath);

            byte[] buf = new byte[1024 * 4];
            int size = inputStream.read(buf);
            int pcmSize = 0;
            while (size != -1) {
                pcmSize += size;
                size = inputStream.read(buf);
            }
            inputStream.close();

            WaveHeader header = new WaveHeader();
            header.fileLength = pcmSize + (44 - 8);
            header.FmtHdrLeth = 16;
            header.BitsPerSample = 16;
            header.Channels = 2;
            header.FormatTag = 0x0001;
            header.SamplesPerSec = 8000;
            header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
            header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
            header.DataHdrLeth = pcmSize;

            byte[] h = header.getHeader();
            assert h.length == 44;
            outputStream.write(h, 0, h.length);
            inputStream = new FileInputStream(sourceFilePath);
            size = inputStream.read(buf);
            while (size != -1) {
                outputStream.write(buf, 0, size);
                size = inputStream.read(buf);
            }
            return true;
        } catch (IOException e) {
            return false;

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                  e.printStackTrace();
                }
            }
        }

    }
}
