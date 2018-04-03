package com.kmsg.viewsys.controller.util;


import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class UtilityServices {

    public static boolean checkInternetConnection(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // test for connection
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public static void getReason(int reason) {
        String reasonStr = "";
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                reasonStr = "ERROR_CANNOT_RESUME";
                break;

            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                reasonStr = "ERROR_DEVICE_NOT_FOUND";
                break;

            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                reasonStr = "ERROR_FILE_ALREADY_EXISTS";
                break;

            case DownloadManager.ERROR_FILE_ERROR:
                reasonStr = "ERROR_FILE_ERROR";
                break;

            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                reasonStr = "ERROR_HTTP_DATA_ERROR";
                break;

            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                reasonStr = "ERROR_INSUFFICIENT_SPACE";
                break;

            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                reasonStr = "ERROR_TOO_MANY_REDIRECTS";
                break;

            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                reasonStr = "ERROR_UNHANDLED_HTTP_CODE";
                break;
        }

        appendLog("reason is : " + reason + ": " + reasonStr);
    }

    public static int getAppVersionNo(Context context) {
        PackageManager pkgManager = context.getPackageManager();
        try {
            PackageInfo pkgInfo = pkgManager.getPackageInfo(context.getPackageName(), 0);
            return pkgInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void appendLog(String text) {
//        Log.e("DDC", text);
    }



/*
    public static void appendLog(String text) {
        Log.e("DDC", text);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm aa", Locale.getDefault());
        String dtTm = sdf.format(System.currentTimeMillis());
        File logFolder = new File(Environment.getExternalStorageDirectory().getPath(), "DDCLog");
        if (!logFolder.exists()) {
            try {
                logFolder.mkdirs();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        File logFile = new File(logFolder.getAbsolutePath() + "/Log.txt");
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(dtTm + ": ");
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
*/



}
