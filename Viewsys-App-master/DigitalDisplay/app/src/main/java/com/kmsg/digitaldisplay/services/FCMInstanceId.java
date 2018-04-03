package com.kmsg.digitaldisplay.services;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

public class FCMInstanceId extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String fcmId = FirebaseInstanceId.getInstance().getToken();
        UtilityServices.appendLog("fcmId: " + fcmId);
        SharedPrefManager.getSharedPreferences(getApplicationContext());
        SharedPrefManager.putString("FCMId", fcmId);
    }

}
