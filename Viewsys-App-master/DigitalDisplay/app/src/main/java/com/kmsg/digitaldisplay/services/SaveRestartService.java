package com.kmsg.digitaldisplay.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.JSONParser;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

/**
 * Created by ADMIN on 09-Mar-18.
 */

public class SaveRestartService extends IntentService {

    public SaveRestartService(){
        super(SaveRestartService.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SharedPrefManager.getSharedPreferences(this);
        saveRestartState();
    }


    private void saveRestartState() {
        JSONParser parser = new JSONParser();
        ContentValues param= new ContentValues();
        param.put("clientId", SharedPrefManager.getString("ClientId", ""));
        param.put("stbId", SharedPrefManager.getString("StbId", ""));
        parser.makeHttpRequest(Constants.SAVE_RESTART_STATE, "POST", param);
    }

}
