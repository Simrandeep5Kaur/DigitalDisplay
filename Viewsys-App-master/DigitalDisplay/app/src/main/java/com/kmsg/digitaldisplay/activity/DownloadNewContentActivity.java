package com.kmsg.digitaldisplay.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kmsg.digitaldisplay.R;
import com.kmsg.digitaldisplay.data.Content;
import com.kmsg.digitaldisplay.database.DBHelper;
import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.ContentUtil;
import com.kmsg.digitaldisplay.util.JSONParser;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DownloadNewContentActivity extends Activity {

    List<Content> contents = new ArrayList<>();
    ProgressDialog pDialog = null;
    List<Long> downloadIds = new ArrayList<>();
    String lastUpdateTm = "";
    private ProgressBar mLoading;
    private JSONParser parser;
    private DBHelper dbHelper = null;
    private String msg = null;
    private String StbId = "";
    private String ClientId = "";
    private DownloadManager mDownloadManager;
    private Map<Long, String> downloadsMap = new ArrayMap<>();
    BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

                if (downloadsMap.containsKey(downloadId)) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);

                    Cursor cursor = mDownloadManager.query(query);
                    if (cursor.moveToFirst()) {
                        int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                            String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                            setContentLocalPath(downloadsMap.get(downloadId), uriString);
                            downloadIds.remove(downloadId);
                            downloadsMap.remove(downloadId);

                            if (downloadIds.size() > 0)
                                pDialog.setProgress(pDialog.getProgress() + 1);
                            else
                                finishProcess();
                        }
                        if (downloadStatus == DownloadManager.STATUS_FAILED || downloadStatus == DownloadManager.STATUS_PAUSED) {
                            UtilityServices.getReason(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)));
                            // cancel the process
                            cancelProcess();
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_content);
        SharedPrefManager.getSharedPreferences(this);
        parser = new JSONParser();
        dbHelper = new DBHelper(this);

        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        // register receivers
        registerReceivers();

        ClientId = SharedPrefManager.getString("ClientId");
        StbId = SharedPrefManager.getString("StbId");

        if (TextUtils.isEmpty(ClientId) || TextUtils.isEmpty(StbId)) {
            // stbId and clientId are missing so ask user to configure device
            startActivity(new Intent(this, RegisterSTBActivity.class));
            this.finish();
        } else {
            init();
            if (UtilityServices.checkInternetConnection(this)) {
                new GetContent().execute(ClientId, StbId);
            } else {
                showToast(getResources().getString(R.string.no_internet));
                checkAndGotoNextActivity();
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceivers();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.hide();
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage(R.string.leave_warning_gc_txt)
                    .setPositiveButton(R.string.txt_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelOnBackPressed();
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
        // register download broadcast receiver
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void unregisterReceivers() {
        try {
            unregisterReceiver(downloadReceiver);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    private void init() {
        mLoading = (ProgressBar) findViewById(R.id.pbLoading);
    }

    private void checkAndGotoNextActivity() {
        if (UtilityServices.checkOffRunTime(this)) {
            startActivity(new Intent(this, VideoActivity.class));
            this.finish();
        }
    }

    private void setContentLocalPath(String fileName, String path) {
        for (Content content : contents) {
            if (content.getContentName().equals(fileName)) {
                content.setPathLocalContent(path);
            }
        }
    }

    private void cancelOnBackPressed() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        ContentUtil.copyFromTemp();
        ////// optional
        for (Long downloadId : downloadIds) {
            mDownloadManager.remove(downloadId);
        }
        downloadIds.clear();
        ////// optional end
        // cancelled
        showToast(getString(R.string.txt_update_unsuccessful));
        UtilityServices.appendLog("********------------get new content cancelled------------*********");
    }

    private void cancelProcess() {
        cancelOnBackPressed();
        checkAndGotoNextActivity();
    }

    private void finishProcess() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
        }
        UtilityServices.appendLog("********------------new content updated------------*********");
        dbHelper.saveContents(contents);
        ContentUtil.deleteDir(new File(Environment.getExternalStorageDirectory() + "/DDTemp"));
        SharedPrefManager.putLong("LastUpdatedContent", System.currentTimeMillis());
        SharedPrefManager.putString("LastUpdatedOnServer", lastUpdateTm);
        checkAndGotoNextActivity();
        ///////
        downloadIds.clear();
    }


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
                ContentUtil.copyToTemp();

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
                cancelProcess();
            }
        } else {
            cancelProcess();
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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
            Log.d("Harry", "client=" + params[0]);
            Log.d("Harry", "stbid="+params[1]);

            JSONObject rootObj = parser.makeHttpRequest(Constants.GET_CONTENT, "POST", param);
            if ( rootObj == null ){
                Log.d("Harry","null found");
            }
            else
                Log.d("Harry",rootObj.toString());

            try {
                if (rootObj != null) {
                    String status = rootObj.getString(Constants.SVC_STATUS);
                    if (Constants.STATUS_SUCCESS.equals(status)) {
                        String basePtah = rootObj.getString("basePath");
                        lastUpdateTm = rootObj.getString("updatedOn");
                        JSONArray lstPlaylistContent = rootObj.getJSONArray("lstContent");

                        for (int i = 0; i < lstPlaylistContent.length(); i++) {
                            JSONObject playlistObj = lstPlaylistContent.getJSONObject(i);
                            contents.add(new Content(
                                    playlistObj.getInt("contentId"),
                                    playlistObj.getString("pathContent"),
                                    basePtah + "/" + ClientId + "/" + playlistObj.getString("pathContent")
                            ));
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
                        downloadAllContent();
                    } else {
                        showToast(getString(R.string.txt_no_content));
                        checkAndGotoNextActivity();
                    }
                } else {
                    showToast(msg);
                    checkAndGotoNextActivity();
                }
            } else {
                // service call fail
                showToast(getString(R.string.msg_server_error));
                checkAndGotoNextActivity();
            }
        }
    }

}
