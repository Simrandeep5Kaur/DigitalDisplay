package com.kmsg.viewsys.controller.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.http.multipart.FilePart;
import com.android.internal.http.multipart.Part;
import com.android.internal.http.multipart.StringPart;
import com.kmsg.viewsys.controller.util.Constants;
import com.kmsg.viewsys.controller.util.JSONParser;
import com.kmsg.viewsys.controller.util.UtilityServices;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ADMIN on 08-Mar-18.
 */

public class TakeScreenshotService extends IntentService{

    private String clientId;
    private String stbId;

    public TakeScreenshotService(){
        super(TakeScreenshotService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Toast.makeText(this, "service called", Toast.LENGTH_SHORT).show();
        if (intent!= null) {
            clientId = intent.getStringExtra("ClientId");
            stbId = intent.getStringExtra("StbId");

            UtilityServices.appendLog(clientId + ", " + stbId);
            takeScreenshot();
        }
    }
    private void takeScreenshot() {
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + "/DDC");

            if (!dir.exists()) {
                dir.mkdirs();
                System.out.println("Simran: New Directory created at External Storage /DDC");
            }

            String path= dir.getAbsolutePath()+"/screenshot.png";
            UtilityServices.appendLog("file : "+ path);
            System.out.println("Simran: screenshot path: "+path);

            Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "screencap -p "+path});
            int result = proc.waitFor();
            UtilityServices.appendLog("results : " + result);
            System.out.println("Simran: result of capturing screenshot: "+result);
            File file = new File(path);
            if (file.exists()) {
                System.out.println("saveScreenshotInBackend called..");
                saveScreenshotInBackend(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            UtilityServices.appendLog("error occurred in taking screenshot  : " + e.getMessage());
        }
    }

    private void saveScreenshotInBackend(File file) throws Exception {
        List<Part> parts = new ArrayList<>();
        parts.add(new StringPart("clientId", clientId));
        parts.add(new StringPart("stbId", stbId));
        parts.add(new FilePart("file", file));

        JSONParser parser = new JSONParser();
        JSONObject rootObj = parser.makeHttpRequestWithMultipart(Constants.SAVE_SCREENSHOT, parts);

        System.out.println("Simran: API to Save Screenshot in backend hit");
        if (rootObj!= null) {
            System.out.println("Simran: rootObj not null");
            if (rootObj.getString(Constants.SVC_STATUS).equals(Constants.STATUS_SUCCESS)) {
                file.delete();
                System.out.println("Simran: SvcStatus success So file deleted");
            }
        }

    }
}
