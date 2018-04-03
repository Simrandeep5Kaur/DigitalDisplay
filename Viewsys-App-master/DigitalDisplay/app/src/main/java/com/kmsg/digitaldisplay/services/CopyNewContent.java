package com.kmsg.digitaldisplay.services;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kmsg.digitaldisplay.data.Content;
import com.kmsg.digitaldisplay.database.DBHelper;
import com.kmsg.digitaldisplay.util.ContentUtil;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ADMIN on 14-Feb-18.
 * copy all new downloaded content
 * this service will be started if app is closed during GetNewContent service was downloading the content
 */

public class CopyNewContent extends IntentService {
    private DBHelper dbHelper = null;

    private List<Content> contents = new ArrayList<>();
    private String lastUpdateTm = "";


    public CopyNewContent() {
        super(CopyNewContent.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SharedPrefManager.getSharedPreferences(this);
        dbHelper = new DBHelper(this);
        String contentStr = intent.getStringExtra("Content");
        lastUpdateTm = intent.getStringExtra("LastUpdatedOnServer");
        if (!TextUtils.isEmpty(contentStr)) {
            contents = getListFromString(contentStr);
            if (contents.size() > 0) {
                copyContent();
            }
        }
    }


    private void copyContent() {
        ContentUtil.copyToTemp();
        try {
            copyNewToOriginal();
            ContentUtil.deleteDir(new File(Environment.getExternalStorageDirectory() + "/DDTemp"));

            dbHelper.saveContents(contents);

            SharedPrefManager.putLong("LastUpdatedContent", System.currentTimeMillis());
            SharedPrefManager.putString("LastUpdatedOnServer", lastUpdateTm);
        } catch (Exception ex) {
            ex.printStackTrace();
            ContentUtil.copyFromTemp();
        }
    }


    private void copyNewToOriginal() throws Exception {
        UtilityServices.appendLog("copy new content to main");
        File newDir = new File(Environment.getExternalStorageDirectory() + "/DD");
        if (!newDir.exists()) {
            newDir.mkdirs();
        }
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);

            File newFileLoc = new File(newDir, content.getContentName());
            UtilityServices.appendLog("temp is " + content.getPathTempContent());
            File tempFileLoc = new File(Uri.parse(content.getPathTempContent()).getPath());
            if (tempFileLoc.exists()) {
                tempFileLoc.renameTo(newFileLoc);
                UtilityServices.appendLog("local is " + newFileLoc.getAbsolutePath());
                contents.get(i).setPathLocalContent(newFileLoc.getAbsolutePath());
            } else {
                UtilityServices.appendLog("temp does not exists");
                throw new Exception("file not Found");
            }
        }
    }


    private List<Content> getListFromString(String contentStr) {
        List<Content> contents = new ArrayList<>();
        try {
            JSONArray lstContent = new JSONArray(contentStr);
            for (int i = 0; i < lstContent.length(); i++) {
                JSONObject contentObj = lstContent.getJSONObject(i);
                contents.add(new Content(
                        contentObj.getInt("contentId"),
                        contentObj.getString("contentName"),
                        contentObj.getString("contentPath"),
                        contentObj.getString("contentTempPath")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return contents;
    }

}
