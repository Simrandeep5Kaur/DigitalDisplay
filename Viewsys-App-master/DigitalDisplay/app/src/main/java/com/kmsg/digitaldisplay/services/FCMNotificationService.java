package com.kmsg.digitaldisplay.services;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.kmsg.digitaldisplay.activity.SplashActivity;
import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import java.util.Map;


public class FCMNotificationService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        UtilityServices.appendLog("message received");

        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            if (data.get("title").equalsIgnoreCase("Update")
                    || data.get("message").equalsIgnoreCase("Update")) {
                // update content
                // if app is not running do not do any thing
                // if app is running content : stop running content and update it

                getApplicationContext().sendOrderedBroadcast(new Intent(Constants.ACTION_UPDATE_NOTIFICATION), null);
            }
            if (data.get("title").equalsIgnoreCase("UpdateStats")
                    || data.get("message").equalsIgnoreCase("UpdateStats")) {
                // update stats i.e. device info, storage and RAM info

                startService(new Intent(getApplicationContext(), SaveStatsService.class));
            }
            if (data.get("title").equalsIgnoreCase("RestartApp")
                    || data.get("message").equalsIgnoreCase("RestartApp")) {
                // restart app
                SharedPrefManager.getSharedPreferences(getApplicationContext());
                SharedPrefManager.putBoolean("Expired", false);
                startService(new Intent(this, SaveRestartService.class));
                startActivity(new Intent(getApplicationContext(), SplashActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP|
                                Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }
            if (data.get("title").equalsIgnoreCase("TakeScreenshot")
                    || data.get("message").equalsIgnoreCase("TakeScreenshot")) {
                System.out.println("Simran: Screenshot Notif arrived");
                Log.d("Simran","Screenshot Notif arrived");
                SharedPrefManager.getSharedPreferences(this);

                // broadcast screenshot event for controller
                getApplicationContext().sendBroadcast(new Intent("com.kmsg.example.SCREENSHOT")
                                .putExtra("ClientId", SharedPrefManager.getString("ClientId"))
                                .putExtra("StbId", SharedPrefManager.getString("StbId")));
                System.out.println("Simran: Screenshot Notif arrived");
                Log.d("Simran","Screenshot Broadcast sent with clientId :"+SharedPrefManager.getString("ClientId")
                +" and StbId: "+SharedPrefManager.getString("StbId"));

            }
        }
    }

}
