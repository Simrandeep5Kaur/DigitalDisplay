package com.kmsg.viewsys.controller.services;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.kmsg.viewsys.controller.util.UtilityServices;

import java.util.Map;


public class FCMNotificationService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        UtilityServices.appendLog("message received");

        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            if (data.get("title").equalsIgnoreCase("updateApp")
                    || data.get("message").equalsIgnoreCase("updateApp")) {

                getApplicationContext().startService(new Intent(this, CheckAppUpdateService.class));
            }
        }
    }

}
