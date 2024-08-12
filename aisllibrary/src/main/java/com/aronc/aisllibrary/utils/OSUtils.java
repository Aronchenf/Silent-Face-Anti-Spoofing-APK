package com.aronc.aisllibrary.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OSUtils {
    public static final String ROM_HARMONY = "HARMONY";
    public static final String ROM_MIUI = "MIUI";
    public static final String ROM_EMUI = "EMUI";
    public static final String ROM_FLYME = "FLYME";
    public static final String ROM_OPPO = "OPPO";
    public static final String ROM_SMARTISAN = "SMARTISAN";
    public static final String ROM_VIVO = "VIVO";

    private static final String KEY_VERSION_MIUI = "ro.miui.ui.version.name";
    private static final String KEY_VERSION_EMUI = "ro.build.version.emui";
    private static final String KEY_VERSION_OPPO = "ro.build.version.opporom";
    private static final String KEY_VERSION_SMARTISAN = "ro.smartisan.version";
    private static final String KEY_VERSION_VIVO = "ro.vivo.os.version";


    @SuppressLint("StaticFieldLeak")
    private static volatile OSUtils mInstance;
    private final Context mContext;
    private PackageInfo packageInfo;
    private static volatile String osVersionInfo;

    public static OSUtils getInstance(Context context) {
        if (mInstance == null) {
            synchronized (OSUtils.class) {
                if (mInstance == null) {
                    mInstance = new OSUtils(context);
                }
            }
        }
        return mInstance;
    }

    private OSUtils(Context context) {
        mContext = context;
    }

    @SuppressLint("WrongConstant")
    private void getAppInfo() {
        try {
            PackageManager packageManager = mContext.getPackageManager();
            packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取应用系统版本
     */
    public String getAppVersionName() {
        if (packageInfo == null) {
            getAppInfo();
        }
        return packageInfo.versionName;
    }

    /**
     * 获取应用系统版本号
     */
    public int getAppVersionCode() {
        if (packageInfo == null) {
            getAppInfo();
        }
        return packageInfo.versionCode;
    }

    /**
     * 获取应用UID
     */
    public String getAndroidId(Context context) {
        return Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * 获取设备型号
     */
    public String getDisplayName() {
        if (!TextUtils.isEmpty(Build.MODEL)) {
            return Build.MODEL;
        } else return "";
    }

    public String getBrand(){
        if (!TextUtils.isEmpty(Build.BRAND)){
            return Build.BRAND;
        }else return "";
    }


    /**
     * 获取操作系统名称
     */
    public String getOsName() {
        if (isHarmonyOS()) {
            return ROM_HARMONY;
        }
        if (isNotEmpty(getBrand(KEY_VERSION_MIUI))) {
            return ROM_MIUI;
        } else if (isNotEmpty(getBrand(KEY_VERSION_EMUI))) {
            return ROM_EMUI;
        } else if (isNotEmpty(getBrand(KEY_VERSION_OPPO))) {
            return ROM_OPPO;
        } else if (isNotEmpty(getBrand(KEY_VERSION_VIVO))) {
            return ROM_VIVO;
        } else if (isNotEmpty(getBrand(KEY_VERSION_SMARTISAN))) {
            return ROM_SMARTISAN;
        } else {
            String brandName = Build.DISPLAY;
            if (brandName.toUpperCase().contains(ROM_FLYME)) {
                return ROM_FLYME;
            } else {
                return Build.MANUFACTURER.toUpperCase();
            }
        }
    }

    /**
     * 获取操作系统是鸿蒙还是Android
     */
    public String getHarmonyOrAndroid() {
        if (isHarmonyOS()) {
            return "Harmony";
        } else {
            return "Android";
        }
    }

    private boolean isNotEmpty(String content) {
        return !TextUtils.isEmpty(content);
    }

    /**
     * 校验是否是鸿蒙系统
     */
    private boolean isHarmonyOS() {
        try {
            Class<?> clazz = Class.forName("com.huawei.system.BuildEx");
            Method method = clazz.getMethod("getOsBrand");
            return "harmony".equals(method.invoke(clazz));
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取手机操作系统版本号
     */
    public String getSystemVersion() {
        if (isHarmonyOS()) {
            return getProp("hw_sc.build.platform.version", "");
        } else {
            return Build.VERSION.RELEASE;
        }
    }

    private String getProp(String property, String defaultValue) {
        try {
            @SuppressLint("PrivateApi") Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("get", String.class);
            String value = (String) method.invoke(clazz, property);
            if (TextUtils.isEmpty(value)) {
                return defaultValue;
            }
            return value;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    private String getBrand(String name) {
        String line;
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec("getprop" + name);
            input = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }

    /**
     * 获取手机系统以及APP相关信息
     */
    public static String getSystemAndPhoneInfo(Context context) {
        if (TextUtils.isEmpty(osVersionInfo)) {
            StringBuilder xuaHeaders = new StringBuilder();
            OSUtils osUtils = OSUtils.getInstance(context);
            xuaHeaders.append("v=1&vn=").append(osUtils.getAppVersionName())
                    .append("&vc=").append(osUtils.getAppVersionCode())
                    .append("&ivn=1.1.4&ivc=4&uid=").append(osUtils.getAndroidId(context))
                    .append("&os=").append(osUtils.getHarmonyOrAndroid()).append("&sv=")
                    .append(osUtils.getSystemVersion()).append("&dn=").append(osUtils.getBrand()).append("_").append(osUtils.getDisplayName());
            osVersionInfo = xuaHeaders.toString();
        }
        return osVersionInfo;
    }


}
