package com.kmsg.digitaldisplay.util;

import android.app.AlarmManager;

/**
 * Created by ADMIN on 27-Nov-17.
 * constants used in code
 */

public class Constants {

    public static final String ACTION_COPY_NEW = "com.kmsg.example.COPY_NEW";
    public static final String ACTION_SET_EXPIRED = "com.kmsg.example.SET_EXPIRED";
    public static final String ACTION_UPDATE_NOTIFICATION = "com.kmsg.example.UPDATE_CONTENT";
    public static final long FIVE_DAYS = (AlarmManager.INTERVAL_DAY * 5);

    public static final String SVC_STATUS = "SvcStatus";
    public static final String SVC_MSG = "SvcMsg";


    public static final String STATUS_SUCCESS = "Success";
//    public static final String STATUS_FAIL= "Failure";


 //   private static final String SERVER_URL= "http://192.168.1.5:8080/digitaldisplay";
//    private static final String SERVER_URL= "http://www.digsig.kmsgtech.com";
    private static final String SERVER_URL = "http://viewsys.in";
//    private static final String SERVER_URL = "http://dd.kmsgtech.com";

    public static final String GET_STB_DATA = SERVER_URL + "/cl/ml/location";
    public static final String CONNECT_STB = SERVER_URL + "/cl/ml/connect_db";

    public static final String GET_LAST_UPDATE = SERVER_URL + "/cl/ml/content_update_tm";
    public static final String GET_CONTENT = SERVER_URL + "/cl/ml/content";

    public static final String SAVE_APP_ACTIVE_STATE = SERVER_URL + "/cl/ml/last_active_location";
    public static final String SAVE_STATS = SERVER_URL + "/cl/ml/save_stats";
    public static final String SAVE_RESTART_STATE = SERVER_URL + "/cl/ml/app_restarted";


}
