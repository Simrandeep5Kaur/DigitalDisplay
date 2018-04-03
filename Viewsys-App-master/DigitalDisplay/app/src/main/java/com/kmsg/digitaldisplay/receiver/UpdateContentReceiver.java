package com.kmsg.digitaldisplay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kmsg.digitaldisplay.services.CopyNewContent;
import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.UtilityServices;

/**
 * Created by ADMIN on 16-Jan-18.
 * received when update notification is received but content is not playing in foreground
 */

public class UpdateContentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        UtilityServices.appendLog("update broadcast is received this means activity is not running");

        if (intent.getAction().equals(Constants.ACTION_COPY_NEW)) {

            String lastUpdatedOnServer = intent.getStringExtra("LastUpdatedOnServer");
            String content = intent.getStringExtra("Content");

            context.startService(new Intent(context, CopyNewContent.class)
                    .putExtra("LastUpdatedOnServer", lastUpdatedOnServer)
                    .putExtra("Content", content));
        }
    }
}