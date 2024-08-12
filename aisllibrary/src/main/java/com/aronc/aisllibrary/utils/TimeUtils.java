package com.aronc.aisllibrary.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 作者：linchangan on 3/10/21 2:15 PM
 * 邮箱：linchangan@apexsoft.com.cn
 */
public class TimeUtils {

    /**
     * 格式：年－月－日 小时：分钟：秒
     */
    public static final String FORMAT_ONE = "yyyy-MM-dd HH:mm:ss";
    /**
     * 默认formater yyyy-MM-dd HH:mm:ss
     */
    public static final SimpleDateFormat TIMEFORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final static ThreadLocal<SimpleDateFormat> dateFormater = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(FORMAT_ONE);
        }
    };

    /***
     * 计算两个时间差，返回的是的秒s
     *
     * @author qxian 2016-5-25下午23:50:06
     *
     * @return long
     * @param dete1
     * @param date2
     * @return
     */
    public static long calDateDifferent(String dete1, String date2) {

        long diff = 0;

        Date d1 = null;
        Date d2 = null;

        try {
            d1 = dateFormater.get().parse(dete1);
            d2 = dateFormater.get().parse(date2);

            // 毫秒ms
            diff = d2.getTime() - d1.getTime();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return diff / 1000;
    }

    public static String getCurTimeStr() {
        Calendar cal = Calendar.getInstance();
        String curDate = dateFormater.get().format(cal.getTime());
        return curDate;
    }

}
