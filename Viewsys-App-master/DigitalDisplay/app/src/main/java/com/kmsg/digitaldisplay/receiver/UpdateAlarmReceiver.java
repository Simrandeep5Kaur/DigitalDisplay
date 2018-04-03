package com.kmsg.digitaldisplay.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kmsg.digitaldisplay.services.AppStateUpdateService;

/**
 * Created by ADMIN on
 * received while content is playing
 * start service to send pulse in background
 */

public class UpdateAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//        UtilityServices.appendLog("update alarm received");
        context.startService(new Intent(context, AppStateUpdateService.class).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
    }

}
