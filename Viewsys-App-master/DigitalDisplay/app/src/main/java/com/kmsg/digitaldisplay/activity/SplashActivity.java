package com.kmsg.digitaldisplay.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.kmsg.digitaldisplay.R;
import com.kmsg.digitaldisplay.database.DBHelper;
import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.JSONParser;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends Activity {

    private static final int PERMISSIONS_MULTIPLE_REQUEST = 100;
    private static final int SPLASH_TIME_OUT = 4000;

    private long startTime;
    private TextView mVersion;
    private TextView mLocationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        SharedPrefManager.getSharedPreferences(this);
        /// initialize db to update db related changes
        new DBHelper(this);

        try {
            if (getPackageManager().getPackageInfo(getPackageName(), 0).versionCode < 3) {
                SharedPrefManager.putBoolean("Expired", false);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        String version= getIntent().getStringExtra("AppVersion");

        if (!TextUtils.isEmpty(version)) {
            UtilityServices.appendLog(" version found  "+ version);
            SharedPrefManager.putString("AppVersion", version);
        } else {
            UtilityServices.appendLog("version not found");
        }

        UtilityServices.appendLog("AppVersion: "+SharedPrefManager.getString("AppVersion"));
        Log.d("Simran AppVersion: ", SharedPrefManager.getString("AppVersion"));

        mVersion = (TextView)findViewById(R.id.tvVersionCode);
        mLocationId = (TextView)findViewById(R.id.tvLocationId);
        Log.d("Harry", "Trial");
        if (!TextUtils.isEmpty(SharedPrefManager.getString("AppVersion", ""))) {
            mVersion.setText(getResources().getString(R.string.version_code_txt, SharedPrefManager.getString("AppVersion")));
        }
        if (!TextUtils.isEmpty(SharedPrefManager.getString("StbId", ""))) {
            String str=getResources().getString(R.string.location_id_txt, SharedPrefManager.getString("StbId"));
            Log.d("Harry", str);
            mLocationId.setText(str);
        }

//        /* testing only */
//        Toast.makeText(this, "version code is: "+ UtilityServices.getAppVersionNo(this), Toast.LENGTH_LONG).show();


        // check if device is not expired
        if (!SharedPrefManager.getBoolean("Expired", false)) {
            startTime = System.currentTimeMillis();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkAndRequestPermissions()) {
                    setupView();
                }
            } else {
                setupView();
            }

        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(R.string.txt_expiry_msg)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok_txt, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            SplashActivity.this.finish();
                        }
                    });

            builder.create().show();
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
//        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
//        if ("com.android.packageinstaller.permission.ui.GrantPermissionsActivity".equals(cn.getClassName())){
//            //permission pDialog is displayed
//            System.out.println("permission pDialog is displayed");
//        } else {
//            if (checkAndRequestPermissions()) {
//                setupView();
//            }
//        }
    }

    private void setupView() {
        long currentTime = System.currentTimeMillis();
        long timeTaken = currentTime - startTime;

        if (timeTaken < SPLASH_TIME_OUT) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    startApp();
                }
            }, (SPLASH_TIME_OUT - timeTaken));
        } else {
            startApp();
        }
    }

    private void startApp() {
        if (SharedPrefManager.getBoolean("Configured", false)) {
            if (UtilityServices.checkInternetConnection(this)) {
                String ClientId = SharedPrefManager.getString("ClientId");
                String StbId = SharedPrefManager.getString("StbId");
                /*new CheckForContentUpdate().execute(ClientId, StbId);*/
                checkAndGotoPlayActivity();
            } else {
                showToast(getResources().getString(R.string.no_internet));
                checkAndGotoPlayActivity();
            }
        } else {
            startActivity(new Intent(this, RegisterSTBActivity.class));
            this.finish();
        }
    }


    // permissions
    private boolean checkAndRequestPermissions() {
        int permissionStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERMISSIONS_MULTIPLE_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_MULTIPLE_REQUEST) {

            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                UtilityServices.appendLog("User interaction was cancelled.");
            } else {
                boolean status = true;
                for (int grantResult : grantResults) {
                    status = status && (grantResult == PackageManager.PERMISSION_GRANTED);
                }
                if (status) {
                    // Permissions granted.
                    setupView();
                } else {
                    // Permission denied.
                    showPermissionDialog();
                }
            }
        }
    }

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.txt_title_permission_req)
                .setMessage(R.string.permission_denied_explanation)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_txt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkAndRequestPermissions();
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }


    private void gotoGetContentActivity() {
        startActivity(new Intent(this, DownloadNewContentActivity.class));
        this.finish();
    }

    private void checkAndGotoPlayActivity() {
        if (UtilityServices.checkOffRunTime(this)) {
            startActivity(new Intent(this, VideoActivity.class));
            this.finish();
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }


    private class CheckForContentUpdate extends AsyncTask<String, String, String> {

        String lastUpdateTm = "";

        @Override
        protected String doInBackground(String... params) {
            ContentValues param = new ContentValues();
            param.put("clientId", params[0]);
            param.put("stbId", params[1]);
            param.put("fcmId", SharedPrefManager.getString("FCMId", ""));
            JSONParser parser = new JSONParser();
            JSONObject rootObj = parser.makeHttpRequest(Constants.GET_LAST_UPDATE, "POST", param);

            try {
                if (rootObj != null) {
                    String status = rootObj.getString(Constants.SVC_STATUS);
                    if (Constants.STATUS_SUCCESS.equals(status)) {
                        lastUpdateTm = rootObj.getString("updatedOn");
                        if (rootObj.has("inactiveDays")) {
                            int days= rootObj.getInt("inactiveDays");
                            SharedPrefManager.putLong("InactiveDays", (days* AlarmManager.INTERVAL_DAY));
                        }
                    }
                    return status;
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                if (Constants.STATUS_SUCCESS.equals(result)) {
                    try {
                        long latestTm = UtilityServices.getTimeInMillis(lastUpdateTm);
                        long lastTm = UtilityServices.getTimeInMillis(SharedPrefManager.getString("LastUpdatedOnServer", ""));
                        if (lastTm < latestTm) {
                            // download content
                            gotoGetContentActivity();
                        } else {
                            SharedPrefManager.putLong("LastUpdatedContent", System.currentTimeMillis());
                            checkAndGotoPlayActivity();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        gotoGetContentActivity();
                    }
                } else {
                    showToast(getString(R.string.msg_service_error));
                    checkAndGotoPlayActivity();
                }
            } else {
                // service call fail
                showToast(getString(R.string.msg_server_error));
                checkAndGotoPlayActivity();
            }
        }
    }

}
