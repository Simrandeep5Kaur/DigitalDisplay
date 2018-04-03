package com.kmsg.digitaldisplay.util;


import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

import java.text.ParseException;
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

    public static long getTimeInMillis(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy hh:mm", Locale.getDefault());
        return sdf.parse(date).getTime();
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

    // imp method
    // if its been three days to content update --> stop running local content and set device as expired
    public static boolean checkOffRunTime(Context context) {
        SharedPrefManager.getSharedPreferences(context);
        long lastUpdatedTime = SharedPrefManager.getLong("LastUpdatedContent", System.currentTimeMillis());
        if ((System.currentTimeMillis() - lastUpdatedTime) < SharedPrefManager.getLong("InactiveDays", Constants.FIVE_DAYS)) {
            return true;
        } else {
            SharedPrefManager.putBoolean("Expired", true);
            return false;
        }
    }

//    public static int getAppVersionNo(Context context) {
//        PackageManager pkgManager = context.getPackageManager();
//        try {
//            PackageInfo pkgInfo = pkgManager.getPackageInfo(context.getPackageName(), 0 );
//            return pkgInfo.versionCode;
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//        return 0;
//    }

    public static void appendLog(String text) {
//        Log.w("DD", text);
    }

/*
    public static void appendLog(String text) {
        Log.e("DD", text);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm aa", Locale.getDefault());
        String dtTm = sdf.format(System.currentTimeMillis());
        File logFolder = new File(Environment.getExternalStorageDirectory().getPath(), "DDLog");
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
