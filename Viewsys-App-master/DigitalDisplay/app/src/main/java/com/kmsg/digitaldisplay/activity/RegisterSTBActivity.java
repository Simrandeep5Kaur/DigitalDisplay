package com.kmsg.digitaldisplay.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.kmsg.digitaldisplay.R;
import com.kmsg.digitaldisplay.data.Content;
import com.kmsg.digitaldisplay.data.STBData;
import com.kmsg.digitaldisplay.database.DBHelper;
import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.FourDigitCardFormatWatcher;
import com.kmsg.digitaldisplay.util.JSONParser;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegisterSTBActivity extends Activity {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;
    boolean waiting = false;
    String lastUpdateTm = "";
    private ProgressBar mLoading;
    private EditText mClientId;
    private EditText mStbId;
    private ConstraintLayout mlDetails;
    private TextView mAddress;
    private TextView mLocality;
    private TextView mContactPerson;
    private TextView mContactNo;
    private Button mBtnOk;
    private Button mBtnConnect;
    private JSONParser parser;
    private DBHelper dbHelper = null;
    private String msg = null;
    private STBData mStbData = null;
    private String StbLocId = "";
    private String StbId = "";
    private String ClientId = "";
    private List<Content> contents = new ArrayList<>();
    private String[] options = {"SERVER", "PEN DRIVE"};
    private ProgressDialog pDialog = null;
    // server content
    private DownloadManager mDownloadManager;
    private List<Long> downloadIds = new ArrayList<>();
    private Map<Long, String> downloadsMap = new ArrayMap<>();
    BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

                if (downloadsMap.containsKey(downloadId)) {
                    Log.d("Harry", "Id=" + downloadId);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);

                    Cursor cursor = mDownloadManager.query(query);
                    if (cursor.moveToFirst()) {
                        Log.d("Harry", "movefirst true");

                        int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                            Log.d("Harry", "download success");
                            String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                            setContentLocalPath(downloadsMap.get(downloadId), uriString);
                            Log.d("Harry", "sz=" + downloadIds.size());
                            downloadIds.remove(downloadId);
                            downloadsMap.remove(downloadId);

                            if (downloadIds.size() > 0)
                                pDialog.setProgress(pDialog.getProgress() + 1);
                            else{
                                Log.d("Harry", "calling finishConfiguration");
                                finishConfiguration();
                            }
                        }
                        if (downloadStatus == DownloadManager.STATUS_FAILED || downloadStatus == DownloadManager.STATUS_PAUSED) {
                            UtilityServices.getReason(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)));
                            // cancel the process
                            Log.d("Harry", "calling cancelConfiguration-2");
                            cancelConfiguration();
                        }
                    }
                }
            }
        }
    };
    // pen drive content
    private UsbManager mUsbManager;
    private UsbMassStorageDevice[] storageDevices = new UsbMassStorageDevice[0];
    private int currentDevice = 0;
    private FileSystem currentFs;
    // receivers
    private final BroadcastReceiver mPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (device != null) {
                            currentDevice = getUsbStorageDevicePos(device);
                            readContentFromDevice();
                        }
                    } else {
                        showToast(getString(R.string.txt_read_permission_denial));
                    }
                }
            }
        }
    };
    private AlertDialog mDeviceDialog;
    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (waiting) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    String action = intent.getAction();
                    UtilityServices.appendLog(action + ": " + device.toString());
                    getDevicesInfo();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_stb);
        SharedPrefManager.getSharedPreferences(this);
        parser = new JSONParser();
        dbHelper = new DBHelper(this);

        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);

        // for testing
//        SharedPrefManager.putBoolean("Configured", false);

        // register receivers
        registerReceivers();

        init();
        setListeners();
    }

    @Override
    protected void onDestroy() {
        unregisterReceivers();
        try {
            storageDevices[currentDevice].close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.hide();
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(R.string.leave_warning_config_txt)
                    .setPositiveButton(R.string.txt_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelConfiguration();
                            leave();
                        }
                    })
                    .setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            pDialog.show();
                        }
                    });
            builder.create().show();
        } else {
            leave();
        }
    }

    private void leave() {
        super.onBackPressed();
    }

    private void registerReceivers() {
        // required while downloading content from server
        // register download broadcast downloadReceiver
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


        // required while copying content from server
        //register the broadcast receiver for permission to read connected device
        registerReceiver(mPermissionReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        // register broadcast receiver for attach and detach device
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));


        // pending intent send with usb read permission request
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    }

    private void unregisterReceivers() {
        try {
            unregisterReceiver(downloadReceiver);
            unregisterReceiver(mPermissionReceiver);
            unregisterReceiver(mUsbDeviceReceiver);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    private void init() {
        mLoading = (ProgressBar) findViewById(R.id.pbLoading);
        mlDetails = (ConstraintLayout) findViewById(R.id.clDetails);
        mClientId = (EditText) findViewById(R.id.edtClientId);
        mStbId = (EditText) findViewById(R.id.edtStbId);
        mAddress = (TextView) findViewById(R.id.txtAddress);
        mLocality = (TextView) findViewById(R.id.txtLocality);
        mContactPerson = (TextView) findViewById(R.id.txtContactPerson);
        mContactNo = (TextView) findViewById(R.id.txtContactNo);
        mBtnOk = (Button) findViewById(R.id.btnOk);
        mBtnConnect = (Button) findViewById(R.id.btnConnect);

        mStbId.addTextChangedListener(new FourDigitCardFormatWatcher());
    }

    private void setListeners() {
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String clientId = mClientId.getText().toString().trim();
                String stbLocId = mStbId.getText().toString().trim().replaceAll("-", "");
                if (validate(clientId, stbLocId, 1)) {
                    if (UtilityServices.checkInternetConnection(RegisterSTBActivity.this)) {
                        new GetSTBData().execute(clientId, stbLocId);
                    } else {
                        showToast(getResources().getString(R.string.no_internet));
                    }
                }
            }
        });

        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String clientId = mClientId.getText().toString().trim();
                String stbLocId = mStbId.getText().toString().trim().replaceAll("-", "");
                if (validate(clientId, stbLocId, 2)) {
                    if (UtilityServices.checkInternetConnection(RegisterSTBActivity.this)) {
                        new GetContent().execute(clientId, StbId);
                    } else {
                        showToast(getResources().getString(R.string.no_internet));
                    }
                }
            }
        });
    }

    private boolean validate(String clientId, String stbLocId, int flag) {
        mClientId.setError(null);
        mStbId.setError(null);
        if (TextUtils.isEmpty(clientId.trim())) {
            mClientId.setError(getString(R.string.ui_no_client_id));
            return false;
        }
        if (TextUtils.isEmpty(stbLocId.trim())) {
            mStbId.setError(getString(R.string.ui_no_stb_id));
            return false;
        }
        if (flag == 2) {
            if (!clientId.equals(ClientId)) {
                mClientId.setError(getString(R.string.ui_changed_client_id));
                return false;
            }
            if (!stbLocId.equals(StbLocId)) {
                mStbId.setError(getString(R.string.ui_changed_stb_id));
                return false;
            }
        }
        return true;
    }

    private void setData() {
        if (mStbData != null) {
            mlDetails.setVisibility(View.VISIBLE);
            mAddress.setText(mStbData.getAddress());
            mLocality.setText(getResources().getString(R.string.loc_city_state_txt, mStbData.getLocality(), mStbData.getCity(), mStbData.getState()));
            mContactPerson.setText(mStbData.getL1());
            mContactNo.setText(mStbData.getL1PhoneNo());
        }
    }

    private void askUserForContentSource() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.txt_choose_source)
                .setSingleChoiceItems(options, 2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        dialog.dismiss();
                        if (position == 0) {
                            // server
                            downloadAllContent();
                        } else if (position == 1) {
                            // pen drive
                            copyContent();
                        }
                    }
                });
        builder.create().show();
    }

    private void setContentLocalPath(String fileName, String path) {
        for (Content content : contents) {
            if (content.getContentName().equals(fileName)) {
                content.setPathLocalContent(path);
            }
        }
    }

    private void cancelConfiguration() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        ////// optional
        for (Long downloadId : downloadIds) {
            mDownloadManager.remove(downloadId);
        }
        downloadIds.clear();
        ////// optional end
        // cancelled
        showToast(getString(R.string.txt_retry_to_config));
        UtilityServices.appendLog("********------------configuration cancelled------------*********");
    }

    private void finishConfiguration() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        UtilityServices.appendLog("********------------device configured------------*********");
        dbHelper.saveContents(contents);
        new ConnectSTB().execute(ClientId, StbId);
        ///////
        downloadIds.clear();
    }

    private void gotoNextActivity() {
        startActivity(new Intent(this, VideoActivity.class));
        this.finish();
    }

    // server process
    private void downloadAllContent() {
        if (UtilityServices.checkInternetConnection(this)) {
            pDialog = new ProgressDialog(this);
            pDialog.setMessage(getString(R.string.txt_msg_downloading));
            pDialog.setIndeterminate(false);
            pDialog.setMax(contents.size());
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setCancelable(false);
            pDialog.show();

            try {
                for (Content content : contents) {
                    String path = content.getPathContent().replaceAll(" ", "%20");
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(path));

                    File dir = new File(Environment.getExternalStorageDirectory() + "/DD");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    File destinationFile = new File(dir, content.getContentName());
                    if (destinationFile.exists()) {
                        try {
                            destinationFile.delete();
                        } catch (SecurityException ex) {
                            ex.printStackTrace();
                        }
                    }
                    request.setDestinationUri(Uri.fromFile(destinationFile));

                    long downloadId = mDownloadManager.enqueue(request);
                    downloadIds.add(downloadId);
                    downloadsMap.put(downloadId, content.getContentName());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    // pen drive process
    private void copyContent() {
        boolean hasFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST);

        if (hasFeature) {
            waiting = true;
            getDevicesInfo();
        } else {
            showToast(getString(R.string.txt_usb_feature_not_found));
        }
    }

    private void getDevicesInfo() {
        if (mDeviceDialog != null && mDeviceDialog.isShowing()) {
            mDeviceDialog.dismiss();
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        storageDevices = UsbMassStorageDevice.getMassStorageDevices(this);
        if (storageDevices.length > 0) {
            String deviceNames[] = getDevicesName(storageDevices);

            dialogBuilder.setTitle(R.string.txt_choose_device)
                    .setSingleChoiceItems(deviceNames, deviceNames.length, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int pos) {
                            currentDevice = pos;
                            setupDevice();
                            dialog.dismiss();
                        }
                    });

        } else {
            dialogBuilder.setMessage(R.string.txt_no_device_found)
                    .setPositiveButton(R.string.ok_txt, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        }
        mDeviceDialog = dialogBuilder.create();
        mDeviceDialog.show();
    }

    private String[] getDevicesName(UsbMassStorageDevice[] storageDevices) {
        String deviceNames[] = new String[storageDevices.length];
        for (int i = 0; i < storageDevices.length; i++) {
            deviceNames[i] = storageDevices[i].getUsbDevice().getDeviceName();
        }
        return deviceNames;
    }

    private void setupDevice() {
        UsbMassStorageDevice selectedDevice = storageDevices[currentDevice];
        UsbDevice usbDevice = selectedDevice.getUsbDevice();
        if (mUsbManager.hasPermission(usbDevice)) {
            // has permission  so read this device
            readContentFromDevice();
        } else {
            // ask read permission
            mUsbManager.requestPermission(usbDevice, mPermissionIntent);
        }
    }

    private void readContentFromDevice() {
        UsbMassStorageDevice selectedDevice = storageDevices[currentDevice];
        boolean initialized = false;
        try {
            selectedDevice.init();
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        UtilityServices.appendLog("initialized: " + initialized);
        if (initialized) {
            List<UsbFile> usbFiles = new ArrayList<>();
            currentFs = selectedDevice.getPartitions().get(0).getFileSystem();

            UsbFile root = currentFs.getRootDirectory();

            UsbFile[] files = null;
            try {
                files = root.listFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (files != null) {
                for (UsbFile file : files) {
                    if (!file.isDirectory()) {
                        for (Content content : contents) {
                            if (file.getName().equals(content.getContentName()))
                                usbFiles.add(file);
                        }
                    }
                }
            }

            if (usbFiles.size() == contents.size()) {
                CopyFilesTaskParam param = new CopyFilesTaskParam();
                param.from = usbFiles;
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DD");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                param.to = dir;
                new CopyFilesTask().execute(param);
                waiting = false;
            } else {
                cancelConfiguration();
            }
        }
    }

    private int getUsbStorageDevicePos(UsbDevice device) {
        for (int i = 0; i < storageDevices.length; i++) {
            if (storageDevices[i].getUsbDevice().getDeviceName().equalsIgnoreCase(device.getDeviceName())) {
                return i;
            }
        }
        return 0;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private static class CopyFilesTaskParam {
        /* package */ List<UsbFile> from = new ArrayList<>(); // dir
        /* package */ File to; // dir
    }

    private class CopyFilesTask extends AsyncTask<CopyFilesTaskParam, Integer, String> {

        private CopyFilesTaskParam param;

        private CopyFilesTask() {
            pDialog = new ProgressDialog(RegisterSTBActivity.this);
            pDialog.setMessage(getString(R.string.txt_msg_copying));
            pDialog.setIndeterminate(false);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            pDialog.show();
        }

        @Override
        protected String doInBackground(CopyFilesTaskParam... params) {
            param = params[0];
            pDialog.setMax(param.from.size());
            try {
                for (UsbFile usbFile : param.from) {
                    InputStream inputStream = new UsbFileInputStream(usbFile);
                    File outFile = new File(param.to, usbFile.getName());
                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outFile));
                    byte[] buffer = new byte[currentFs.getChunkSize()];
                    int length;

                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    outputStream.close();
                    inputStream.close();
                    setContentLocalPath(usbFile.getName(), outFile.getPath());
                    pDialog.setProgress(pDialog.getProgress() + 1);
                }
                return "success";
            } catch (IOException e) {
                UtilityServices.appendLog("error copying! " + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && result.equalsIgnoreCase("success")) {
                waiting = false;
                finishConfiguration();
            } else {
                waiting = true;
                cancelConfiguration();
            }
        }

    }

    private class GetSTBData extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            mlDetails.setVisibility(View.GONE);
            mBtnOk.setClickable(false);
            mLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            ContentValues param = new ContentValues();
            param.put("clientId", params[0]);
            param.put("stbLocId", params[1]);
            JSONObject rootObj = parser.makeHttpRequest(Constants.GET_STB_DATA, "POST", param);

            try {
                if (rootObj != null) {
                    /*SharedPrefManager.putString("stbLocId",params[1]);*/
                    String status = rootObj.getString(Constants.SVC_STATUS);

                    if (Constants.STATUS_SUCCESS.equals(status)) {
                        JSONObject stbObj = rootObj.getJSONObject("stbData");

                        mStbData = new STBData(
                                stbObj.getString("stbId"),
                                stbObj.getString("address"),
                                stbObj.getString("locality"),
                                stbObj.getString("city"),
                                stbObj.getString("state"),
                                stbObj.getString("l1"),
                                stbObj.getString("l1PhoneNo")
                        );

                        ClientId = params[0];
                        StbLocId = params[1];
                        StbId = mStbData.getStbId();
                    }
                    if (rootObj.has(Constants.SVC_MSG))
                        msg = rootObj.getString(Constants.SVC_MSG);
                    return status;
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mBtnOk.setClickable(true);
            mLoading.setVisibility(View.GONE);
            if (result != null) {
                if (Constants.STATUS_SUCCESS.equals(result)) {
                    setData();
                } else {
                    showToast(msg);
                }
            } else {
                // service call fail
                showToast(getString(R.string.msg_server_error));
            }
        }
    }

    private class GetContent extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            contents.clear();
            mLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            ContentValues param = new ContentValues();
            param.put("clientId", params[0]);
            param.put("stbId", params[1]);
            Log.d("Harry", "client R=" + params[0]);
            Log.d("Harry", "stbid R="+params[1]);
            JSONObject rootObj = parser.makeHttpRequest(Constants.GET_CONTENT, "POST", param);

            if ( rootObj == null ){
                Log.d("Harry", "json is null");
            }
            try {
                if (rootObj != null) {
                    String status = rootObj.getString(Constants.SVC_STATUS);
                    if (Constants.STATUS_SUCCESS.equals(status)) {
                        String appVersion = rootObj.getString("appVersion");
                        SharedPrefManager.putString("AppVersion", appVersion);
                        UtilityServices.appendLog("AppVersion: " + SharedPrefManager.getString("AppVersion",""));
                        Log.d("Harry", "status is success");

                        String basePath = rootObj.getString("basePath");
                        lastUpdateTm = rootObj.getString("updatedOn");
                        JSONArray lstPlaylistContent = rootObj.getJSONArray("lstContent");
                        Log.d("Harry", "lstPlaylistContent length="+lstPlaylistContent.length());

                        for (int i = 0; i < lstPlaylistContent.length(); i++) {
                            JSONObject playlistObj = lstPlaylistContent.getJSONObject(i);
                            contents.add(new Content(
                                    playlistObj.getInt("contentId"),
                                    playlistObj.getString("pathContent"),
                                    basePath + "/" + ClientId + "/" + playlistObj.getString("pathContent")
                            ));
                            Log.d("Harry", "path is"+ basePath + "/" + ClientId + "/" + playlistObj.getString("pathContent"));
                        }
                    }
                    if (rootObj.has(Constants.SVC_MSG))
                        msg = rootObj.getString(Constants.SVC_MSG);
                    return status;
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mLoading.setVisibility(View.GONE);
            if (result != null) {
                if (Constants.STATUS_SUCCESS.equals(result)) {
                    if (contents.size() > 0) {
                        askUserForContentSource();
                    } else {
                        showToast(getString(R.string.txt_no_content));
                    }
                } else {
                    showToast(msg);
                }
            } else {
                // service call fail
                showToast(getString(R.string.msg_server_error));
            }
        }
    }

    private class ConnectSTB extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            mBtnConnect.setClickable(false);
            mBtnOk.setClickable(false);
            mLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            ContentValues param = new ContentValues();
            param.put("clientId", params[0]);
            param.put("stbId", params[1]);
            param.put("fcmId", SharedPrefManager.getString("FCMId", ""));
            Log.d("Harry", "clientid===" + params[0]);
            Log.d("Harry", "stbid===" + params[1]);
            Log.d("Harry", "fcmid===" + SharedPrefManager.getString("FCMId", ""));

            JSONObject rootObj = parser.makeHttpRequest(Constants.CONNECT_STB, "POST", param);
            Log.d("Harry", "return:" + rootObj + " by "+Constants.CONNECT_STB);

            try {
                if (rootObj != null) {
                    String status = rootObj.getString(Constants.SVC_STATUS);
                    Log.d("Harry", "Status="+ status+ " Last UpdTm=" + lastUpdateTm);
                    if (Constants.STATUS_SUCCESS.equals(status)) {
                        SharedPrefManager.putString("ClientId", ClientId);
                        SharedPrefManager.putString("StbId", StbId);
                        SharedPrefManager.putBoolean("Configured", true);
                        SharedPrefManager.putLong("LastUpdatedContent", System.currentTimeMillis());
                        SharedPrefManager.putString("LastUpdatedOnServer", lastUpdateTm);
                    }
                    if (rootObj.has(Constants.SVC_MSG))
                        msg = rootObj.getString(Constants.SVC_MSG);
                    return status;
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mBtnConnect.setClickable(true);
            mBtnOk.setClickable(true);
            mLoading.setVisibility(View.GONE);
            if (result != null) {
                if (Constants.STATUS_SUCCESS.equals(result)) {
                    gotoNextActivity();
                } else {
                    showToast(msg);
                    mlDetails.setVisibility(View.GONE);
                }
            } else {
                // service call fail
                showToast(getString(R.string.msg_server_error));
            }
        }
    }

}
