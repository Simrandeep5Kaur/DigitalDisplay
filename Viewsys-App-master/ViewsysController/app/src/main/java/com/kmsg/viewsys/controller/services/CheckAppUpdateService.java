package com.kmsg.viewsys.controller.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.Nullable;

import com.kmsg.viewsys.controller.util.Constants;
import com.kmsg.viewsys.controller.util.JSONParser;
import com.kmsg.viewsys.controller.util.SharedPrefManager;
import com.kmsg.viewsys.controller.util.UtilityServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ADMIN on 01-Feb-18.
 * Service to check and install new apk
 */

public class CheckAppUpdateService extends IntentService {

    String INSTALL_VIEW_SYS = "pm install -r %s\n" +
            "sleep 5\n" +
            "am start -n com.kmsg.digitaldisplay/.activity.SplashActivity --es \"AppVersion\" %s";

    String INSTALL_CONTROLLER = "pm install -r %s";

    private String version;
    private String dtTm;
    private String ctrlVersion;
    private String ctrlDtTm;


    public CheckAppUpdateService() {
        super(CheckAppUpdateService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        SharedPrefManager.getSharedPreferences(this);

        if (UtilityServices.checkInternetConnection(this)) {
            getLatestAppDetails();
        }
    }

    private void getLatestAppDetails() {
        JSONParser jsonParser = new JSONParser();
        JSONObject rootObj = jsonParser.makeHttpRequest(Constants.CHECK_APP_VERSION, "POST", new ContentValues());
        try {
            if (rootObj != null) {
                String status = rootObj.getString(Constants.SVC_STATUS);
                if (Constants.STATUS_SUCCESS.equals(status)) {
                    version = rootObj.getString("version");
                    dtTm = rootObj.getString("dtTm");
                    String path = rootObj.getString("apkPath");
                    ctrlVersion = rootObj.getString("versionCtrl");
                    ctrlDtTm = rootObj.getString("dtTmCtrl");
                    String ctrlPath = rootObj.getString("apkPathCtrl");

                    if (compareDates(dtTm, SharedPrefManager.getString("LastUpdateTm", null))) {
                        downloadInstallApk(path, true);
                    }
                    if (compareDates(ctrlDtTm, SharedPrefManager.getString("ControllerUpdateTm", null))) {
                        downloadInstallApk(ctrlPath, false);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void downloadInstallApk(String path, boolean isViewSys) {
        UtilityServices.appendLog("download apk" + path);
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + "/DDC");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = isViewSys ? "app.apk" : "ctrl.apk";
            File destinationFile = new File(dir, fileName);

            if (destinationFile.exists()) {
                destinationFile.delete();
            }

            URL url = new URL(path);

            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setDoOutput(true);
            c.connect();

            FileOutputStream fos = new FileOutputStream(destinationFile);
            InputStream is = c.getInputStream();

            byte[] buffer = new byte[1024];
            int len1 = 0;
            while ((len1 = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len1);
            }
            fos.close();
            is.close();

            if (destinationFile.exists()) {
                UtilityServices.appendLog("install apk  "+ destinationFile.getName());
                if (isViewSys) {
                    processCommand(String.format(INSTALL_VIEW_SYS, destinationFile.getAbsolutePath(), version));
                    destinationFile.delete();
                    SharedPrefManager.putString("LastUpdateTm", dtTm);
                    SharedPrefManager.putString("LastVersion", version);
                } else {
                    SharedPrefManager.putString("ControllerUpdateTm", ctrlDtTm);
                    SharedPrefManager.putString("ControllerVersion", ctrlVersion);
                    processCommand(String.format(INSTALL_CONTROLLER, destinationFile.getAbsolutePath()));
                }
            } else {
                UtilityServices.appendLog("file not found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // returns false if current date is less than or equals to last date else return true
    private boolean compareDates(String currentDate, String lastDate) {
        if (lastDate != null && currentDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy hh:mm", Locale.getDefault());
            try {
                Date lastDtTm = sdf.parse(lastDate);
                Date currentDtTm = sdf.parse(currentDate);
                if (lastDtTm.getTime() >= currentDtTm.getTime()) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean processCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            int result = process.waitFor();
            UtilityServices.appendLog("results " + result);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            UtilityServices.appendLog("error occurred in installing: " + e.getMessage());
        }
        return false;
   }

}