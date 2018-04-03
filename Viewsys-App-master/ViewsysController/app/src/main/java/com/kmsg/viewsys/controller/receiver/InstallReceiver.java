package com.kmsg.viewsys.controller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.kmsg.viewsys.controller.services.CheckAppUpdateService;

/**
 * Created by ADMIN on 01-Feb-18.
 * receive installation broadcast from system
 */

public class InstallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri data = intent.getData();
        String packageName = data.getSchemeSpecificPart();

        if (packageName != null) {
            if (packageName.equals(context.getPackageName())) {
                // own package
                // means app is just installed so check for other main app version update
                context.startService(new Intent(context, CheckAppUpdateService.class)
                        .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
            }
        }
    }
}
