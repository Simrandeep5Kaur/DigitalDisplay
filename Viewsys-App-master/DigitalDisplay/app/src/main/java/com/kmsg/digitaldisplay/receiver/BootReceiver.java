package com.kmsg.digitaldisplay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.kmsg.digitaldisplay.activity.SplashActivity;
import com.kmsg.digitaldisplay.util.SharedPrefManager;

/**
 * Created by ADMIN on 30-Oct-17.
 * received when device is booted
 * start app if device is configured and not expired
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        SharedPrefManager.getSharedPreferences(context);

//        System.out.println("BootReceiver is called");
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            if (!SharedPrefManager.getBoolean("Expired", false) && SharedPrefManager.getBoolean("Configured", false)) {
                Intent myIntent = new Intent(context, SplashActivity.class);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(myIntent);
            }
        }
    }


}