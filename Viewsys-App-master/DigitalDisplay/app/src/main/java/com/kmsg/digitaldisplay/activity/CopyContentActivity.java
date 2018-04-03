package com.kmsg.digitaldisplay.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;

import com.kmsg.digitaldisplay.R;
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

public class CopyContentActivity extends Activity {

    private DBHelper dbHelper = null;

    private List<Content> contents = new ArrayList<>();
    private String lastUpdateTm = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_stb);
        SharedPrefManager.getSharedPreferences(this);
        dbHelper = new DBHelper(this);
        String contentStr = getIntent().getStringExtra("Content");
        lastUpdateTm = getIntent().getStringExtra("LastUpdatedOnServer");
        if (!TextUtils.isEmpty(contentStr)) {
            contents = getListFromString(contentStr);
            if (contents.size() > 0) {
                copyContent();
            }
        }
    }

    private void copyContent() {
        ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setMessage(getString(R.string.txt_msg_downloading));
        pDialog.setIndeterminate(false);
        pDialog.setMax(contents.size());
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setCancelable(false);
        pDialog.show();

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
        pDialog.dismiss();
        gotoVideoActivity();
    }

    private void gotoVideoActivity() {
        startActivity(new Intent(this, VideoActivity.class));
        this.finish();
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
            File tempFileLoc = new File(Uri.parse(content.getPathTempContent()).getPath());
            if (tempFileLoc.exists()) {
                tempFileLoc.renameTo(newFileLoc);
                contents.get(i).setPathLocalContent(newFileLoc.getAbsolutePath());
            } else {
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
