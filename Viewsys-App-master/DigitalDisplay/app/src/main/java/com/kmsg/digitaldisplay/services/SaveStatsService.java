package com.kmsg.digitaldisplay.services;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.Nullable;

import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.JSONParser;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by ADMIN on 28-Feb-18.
 */

public class SaveStatsService extends IntentService {

    private String paths[] = {"/sys/devices/virtual/switch/hdmi/state", "/sys/class/switch/hdmi/state", "/sys/class/display/display0.hdmi/connect"};

    public SaveStatsService() {
        super(SaveStatsService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SharedPrefManager.getSharedPreferences(this);
        gatherAllInfo();
    }

    private void gatherAllInfo() {
        // BASIC
        String os = "Android";
        String versionCode = Build.VERSION.RELEASE;
        int apiVersion = Build.VERSION.SDK_INT;

        // RAM
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        long totalRamValue = mi.totalMem / 1048576L;
        long freeRamValue = mi.availMem / 1048576L;
        long usedRamValue = totalRamValue - freeRamValue;

        // STORAGE
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        long totalBlocks = stat.getBlockCountLong();

        long totalStorage = (totalBlocks * blockSize) / 1048576L;
        long availableStorage = (availableBlocks * blockSize) / 1048576L;
        long usedStorage = (totalStorage - availableStorage);

        // internet speed
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int internetSpeed = wifiManager.getConnectionInfo().getLinkSpeed();
        internetSpeed = (internetSpeed == -1 ? 0 : internetSpeed); // -1 is returned if not connected to internet via wifi


        // HDMI status
        // only rooted devices
        boolean hdmiStatus = false;
        File file;
        int i = 0;
        while (!hdmiStatus && i < paths.length) {
            file = new File(paths[i]);
            if (file.exists()) {
                hdmiStatus = checkContentHDMI(file);
            }
            UtilityServices.appendLog("status: " + hdmiStatus + ", i: " + i);
            i++;
        }


        try {
            JSONObject statObj = new JSONObject();
            statObj.put("os", os);
            statObj.put("versionCode", versionCode);
            statObj.put("apiVersion", apiVersion);
            statObj.put("totalRam", totalRamValue);
            statObj.put("usedram", usedRamValue);
            statObj.put("totalStorage", totalStorage);
            statObj.put("usedStorage", usedStorage);
            statObj.put("networkSpeed", internetSpeed);
            statObj.put("hdmiStatus", hdmiStatus);
            // extras
            statObj.put("fcm_id", "");
            statObj.put("requestSentOn", "");
            statObj.put("lastRequestReceivedOn", "");
            statObj.put("requestSent", "0");


            JSONParser parser = new JSONParser();
            ContentValues param = new ContentValues();
            param.put("clientId", SharedPrefManager.getString("ClientId"));
            param.put("stbId", SharedPrefManager.getString("StbId"));
            param.put("statModel", statObj.toString());

            parser.makeHttpRequest(Constants.SAVE_STATS, "POST", param);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private boolean checkContentHDMI(File file) {
        // get the data from file.If the data in the file is 0 hdmi is not connected if its 1 its connected
        UtilityServices.appendLog("file found: " + file.getAbsolutePath());
        /*try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
                if (line != null) {
                    sb.append("\n");
                }
            }
            String content = sb.toString();
            UtilityServices.appendLog("file content: " + content);
            br.close();
            return content.equals("1");

        } catch (IOException ex) {
            UtilityServices.appendLog("exception occurred: " + ex.getMessage());
            ex.printStackTrace();
        }*/
        try {
            Scanner switchFileScanner = new Scanner(file);
            int switchValue = switchFileScanner.nextInt();
            switchFileScanner.close();
            return switchValue > 0;
        } catch (Exception e) {
            UtilityServices.appendLog("exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
