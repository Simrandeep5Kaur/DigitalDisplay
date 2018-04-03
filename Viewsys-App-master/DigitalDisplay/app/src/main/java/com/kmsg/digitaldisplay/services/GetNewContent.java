package com.kmsg.digitaldisplay.services;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.kmsg.digitaldisplay.data.Content;
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

/**
 * Created by ADMIN on 12-Feb-18.
 * download new content when from server
 * started from VideoActivity when Update notification is received
 */

public class GetNewContent extends IntentService {
    List<Content> contents = new ArrayList<>();
    List<Long> downloadIds = new ArrayList<>();
    String lastUpdateTm = "";
    private JSONParser parser;
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
                        UtilityServices.appendLog("status: " + downloadStatus);
                        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                            String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                            setContentTempPath(downloadsMap.get(downloadId), uriString);
                            downloadIds.remove(downloadId);
                            downloadsMap.remove(downloadId);

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

    public GetNewContent() {
        super(GetNewContent.class.getName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceivers();
    }

    @Override
    public void onDestroy() {
        UtilityServices.appendLog("get new content destroyed");
        unregisterReceivers();
        super.onDestroy();
    }

    private void registerReceivers() {
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void unregisterReceivers() {
        try {
            unregisterReceiver(downloadReceiver);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        UtilityServices.appendLog("get new content started");
        parser = new JSONParser();
        SharedPrefManager.getSharedPreferences(this);
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (UtilityServices.checkInternetConnection(this)) {
            String result = getContent();
            processResult(result);
        } else {
            checkAndGotoNextActivity();
        }
    }

    private String getContent() {
        String ClientId = SharedPrefManager.getString("ClientId");
        String StbId = SharedPrefManager.getString("StbId");

        ContentValues param = new ContentValues();
        param.put("clientId", ClientId);
        param.put("stbId", StbId);
        JSONObject rootObj = parser.makeHttpRequest(Constants.GET_CONTENT, "POST", param);

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
                return status;
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void processResult(String result) {
        if (result != null && Constants.STATUS_SUCCESS.equals(result) && contents.size() > 0) {
            downloadAllContent();
            waitForCompletion();
        } else {
            // service call fail
            checkAndGotoNextActivity();
        }
    }

    private void waitForCompletion() {
        if (downloadIds.size() > 0) {
            WaitForCompletion obj = new WaitForCompletion();
            Thread th = new Thread(obj);
            th.start();
            try {
                th.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadAllContent() {
        if (UtilityServices.checkInternetConnection(this)) {
            try {
                for (Content content : contents) {
                    String path = content.getPathContent().replaceAll(" ", "%20");
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(path));

                    File dir = new File(Environment.getExternalStorageDirectory() + "/DDNew");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    } else {
                        ContentUtil.deleteAllFromDir(dir);
                    }

                    File destinationFile = new File(dir, content.getContentName());
                    request.setDestinationUri(Uri.fromFile(destinationFile));

                    long downloadId = mDownloadManager.enqueue(request);
                    downloadIds.add(downloadId);
                    downloadsMap.put(downloadId, content.getContentName());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                cancelProcess();
            }
        }
    }

    private void setContentTempPath(String fileName, String path) {
        for (Content content : contents) {
            if (content.getContentName().equals(fileName)) {
                content.setPathTempContent(path);
            }
        }
    }

    private void cancelProcess() {
        for (Long downloadId : downloadIds) {
            mDownloadManager.remove(downloadId);
        }
        downloadIds.clear();
        try {
            ContentUtil.deleteAllFromDir(new File(Environment.getExternalStorageDirectory() + "/DDNew"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        UtilityServices.appendLog("********------------get new content cancelled------------*********");
        checkAndGotoNextActivity();
    }

    private void finishProcess() {
        ///////
        downloadIds.clear();
        UtilityServices.appendLog("********------------new content updated------------*********");
        checkAndGotoNewActivity();
    }

    private String getJsonString(List<Content> contents) throws JSONException {
        JSONArray lstContent = new JSONArray();
        for (Content content : contents) {
            JSONObject contentObj = new JSONObject();
            contentObj.put("contentId", content.getContentId());
            contentObj.put("contentName", content.getContentName());
            contentObj.put("contentPath", content.getPathContent());
            contentObj.put("contentTempPath", content.getPathTempContent());
            lstContent.put(contentObj);
        }
        return lstContent.toString();
    }

    private void checkAndGotoNewActivity() {
        if (UtilityServices.checkOffRunTime(this)) {
            String jsonStr = null;
            try {
                jsonStr = getJsonString(contents);
                UtilityServices.appendLog(jsonStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jsonStr != null) {
                getApplicationContext().sendOrderedBroadcast(new Intent(Constants.ACTION_COPY_NEW)
                        .putExtra("LastUpdatedOnServer", lastUpdateTm)
                        .putExtra("Content", jsonStr), null);
            }
        } else {
            // mark as expired
            UtilityServices.appendLog("mark as expired");
            getApplicationContext().sendOrderedBroadcast(new Intent(Constants.ACTION_SET_EXPIRED), null);
        }
        deleteAll();
    }

    private void checkAndGotoNextActivity() {
        if (!UtilityServices.checkOffRunTime(this)) {
            // mark as expired
            UtilityServices.appendLog("mark as expired");
            getApplicationContext().sendOrderedBroadcast(new Intent(Constants.ACTION_SET_EXPIRED), null);
        }
        deleteAll();
    }

    private void deleteAll() {
        downloadIds.clear();
        contents.clear();
        downloadsMap.clear();
    }


    private class WaitForCompletion implements Runnable {
        @Override
        public void run() {
            while (downloadIds.size() > 0) {
                for (int dx = 0; dx < 90000000; dx++) {
                }
                UtilityServices.appendLog("Waiting..." + downloadIds.size() + ": of :" + contents.size());
            }
            if (downloadIds.size() == 0)
                finishProcess();
        }
    }

}
