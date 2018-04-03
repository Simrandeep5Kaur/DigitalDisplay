package com.kmsg.digitaldisplay.services;

import android.Manifest;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.kmsg.digitaldisplay.data.ContentLog;
import com.kmsg.digitaldisplay.database.DBHelper;
import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.JSONParser;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by ADMIN on 18-Dec-17.
 * save location and app active state in backend
 */

public class AppStateUpdateService extends IntentService {

    protected Location mLastLocation;
    FusedLocationProviderClient mClient;

    public AppStateUpdateService() {
        super(AppStateUpdateService.class.getName());

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SharedPrefManager.getSharedPreferences(this);

        if (UtilityServices.checkInternetConnection(this)) {
            if (hasGPS(getApplicationContext()) && checkPermissions()) {
                LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
                boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);
                UtilityServices.appendLog("location enabled: " + enabled);
                if (enabled) {
                    getLastLocation();
                } else {
                    // gps is off
                    saveAppActiveState();
                }
            } else {
                UtilityServices.appendLog("location permission not granted");
                // location permission is denied
                saveAppActiveState();
            }
        }
    }

    public boolean hasGPS(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mClient = LocationServices.getFusedLocationProviderClient(this);
        mClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLastLocation = task.getResult();
                        }
                        saveAppActiveState();
                    }
                });
    }

    private void saveAppActiveState() {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ContentValues param = new ContentValues();
        param.put("clientId", SharedPrefManager.getString("ClientId", ""));
        param.put("stbId", SharedPrefManager.getString("StbId", ""));
        if (mLastLocation != null) {
            param.put("latitude", mLastLocation.getLatitude());
            param.put("longitude", mLastLocation.getLongitude());
        }
        // content log
        DBHelper dbHelper = new DBHelper(this);
        List<ContentLog> logs= dbHelper.getContentLog();
        JSONArray lstLog= new JSONArray();
        if (logs.size()>0){
            for (ContentLog log : logs) {
                try {
                    JSONObject logObj= new JSONObject();
                    logObj.put("contentId", log.getContentId())
                            .put("dtTm", log.getDtTm());
                    lstLog.put(logObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        param.put("playTime", lstLog.toString());
        JSONParser jsonParser = new JSONParser();
        JSONObject rootObj = jsonParser.makeHttpRequest(Constants.SAVE_APP_ACTIVE_STATE, "POST", param);
        if (rootObj!= null) {
            try {
                if (rootObj.getString(Constants.SVC_STATUS).equals(Constants.STATUS_SUCCESS)) {
                    dbHelper.deleteContentLog();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
