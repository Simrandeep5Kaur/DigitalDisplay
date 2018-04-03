package com.kmsg.viewsys.controller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kmsg.viewsys.controller.services.TakeScreenshotService;
import com.kmsg.viewsys.controller.util.UtilityServices;

/**
 * Created by ADMIN on 08-Mar-18.
 */

public class TakeScreenshotReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        UtilityServices.appendLog("TakeScreenshotReceiver: broadcast received");

        if (!UtilityServices.isMyServiceRunning(context, TakeScreenshotService.class))
            context.startService(new Intent(context, TakeScreenshotService.class)
            .putExtra("ClientId", intent.getStringExtra("ClientId"))
            .putExtra("StbId", intent.getStringExtra("StbId")));
    }
}
