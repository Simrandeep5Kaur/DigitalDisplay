package com.kmsg.viewsys.controller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.kmsg.viewsys.controller.services.CheckAppUpdateService;
import com.kmsg.viewsys.controller.util.SharedPrefManager;
import com.kmsg.viewsys.controller.util.UtilityServices;

/**
 * Created by ADMIN on 01-Feb-18.
 * receive reboot broadcast from system
 */

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPrefManager.getSharedPreferences(context);
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            SharedPrefManager.putBoolean("Booted", true);
        }

        if (SharedPrefManager.getBoolean("Booted", false) && UtilityServices.checkInternetConnection(context)) {
            context.startService(new Intent(context, CheckAppUpdateService.class)
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));

            SharedPrefManager.putBoolean("Booted", false);
        }
    }
}
