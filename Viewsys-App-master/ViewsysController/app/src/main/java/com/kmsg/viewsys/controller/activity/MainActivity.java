package com.kmsg.viewsys.controller.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.kmsg.viewsys.controller.R;
import com.kmsg.viewsys.controller.libsu.Shell;
import com.kmsg.viewsys.controller.util.SharedPrefManager;
import com.kmsg.viewsys.controller.util.UtilityServices;

import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ADMIN on 01-Feb-18.
 * launcher activity
 * get super user permission from user
 */

public class MainActivity extends Activity {

    private static final int PERMISSIONS_MULTIPLE_REQUEST = 100;

    private static final String INSTALL_SCRIPT =
            "mount -o rw,remount /system\n" +
                    "cat %s > /system/priv-app/vsController.apk.tmp\n" +
                    "chmod 644 /system/priv-app/vsController.apk.tmp\n" +
                    "pm uninstall %s\n" +
                    "mv /system/priv-app/vsController.apk.tmp /system/priv-app/vsController.apk\n" +
                    "pm install -r /system/priv-app/vsController.apk\n" +
                    "sleep 5\n" +
                    "reboot\n" + ////
                    "am start -n com.kmsg.viewsys.controller/.activity.MainActivity";


/*
    // trial
    public static void deleteFromSystem(final String file) {
        try {
            if (new File(file).exists()) {
                String path = new File(file).getParent();
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("mount -o rw,remount /system; \n");
                os.writeBytes("chmod 777 " + path + "; \n");
                os.writeBytes("chmod 777 " + file + "; \n");
                os.writeBytes("rm -r " + file + "; \n");
                os.writeBytes("mount -o ro,remount /system; \n");
                os.writeBytes("reboot \n");
                os.flush();
                os.close();
                process.waitFor();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
*/

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.kmsg.viewsys.controller.R.layout.layout_main);
        SharedPrefManager.getSharedPreferences(this);
        UtilityServices.appendLog(SharedPrefManager.getString("FCMId", ""));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkAndRequestPermissions()) {
                setupView();
            }
        } else {
            setupView();
        }

        String version = SharedPrefManager.getString("ControllerVersion", "");
        UtilityServices.appendLog("version :"+ version);
        if (!TextUtils.isEmpty(version)) {
            TextView mVersion = (TextView)findViewById(R.id.tvVersion);
            mVersion.setText(getResources().getString(R.string.version_code_txt,version));
        }
    }

    private void setupView() {
        Toast.makeText(MainActivity.this,
                "please wait", Toast.LENGTH_SHORT).show();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                final boolean isRooted = Shell.SU.available();
                if (!isRooted) {
                    UtilityServices.appendLog("Device is unrooted! You won't be able to use" +
                            "this device as a server");
                }
                return null;
            }
        }.execute();


//        trial
//        to make controller a system app, copy this to system memory
//
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected void onPreExecute() {
//                Toast.makeText(MainActivity.this, "please wait", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            protected Void doInBackground(Void... voids) {
//                final boolean isRooted = Shell.SU.available();
//                if (isRooted) {
//                    if (!SharedPrefManager.getBoolean("has_system_privilege", false)) {
//                        Shell.SU.run(String.format(INSTALL_SCRIPT,
//                                new String[]{
//                                        MainActivity.this.getPackageCodePath(),
//                                        MainActivity.this.getPackageName()
//                                }));
//                        SharedPrefManager.putBoolean("has_system_privilege", true);
//                    }
//                } else {
//                    UtilityServices.appendLog("Device is unrooted! You won't be able to use" +
//                            "this device as a controller");
//                }
//                return null;
//            }
//        }.execute();

    }

    // permissions
    private boolean checkAndRequestPermissions() {
        int permissionStorage = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
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

}
