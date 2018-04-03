package com.kmsg.viewsys.controller.services;

import android.content.ContentValues;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.kmsg.viewsys.controller.util.Constants;
import com.kmsg.viewsys.controller.util.JSONParser;
import com.kmsg.viewsys.controller.util.SharedPrefManager;
import com.kmsg.viewsys.controller.util.UtilityServices;

public class FCMInstanceId extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String fcmId = FirebaseInstanceId.getInstance().getToken();
        UtilityServices.appendLog("fcmId: " + fcmId);
        SharedPrefManager.getSharedPreferences(getApplicationContext());
        SharedPrefManager.putString("FCMId", fcmId);

        saveFcmInBackend(fcmId);
    }

    private void saveFcmInBackend(String fcmId) {
        ContentValues param = new ContentValues();
        param.put("fcmId", fcmId);

        JSONParser jsonParser = new JSONParser();

        jsonParser.makeHttpRequest(Constants.SAVE_FCM_ID, "POST", param);
    }

}
