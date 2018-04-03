package com.kmsg.digitaldisplay.util;

import android.os.Environment;

import java.io.File;

/**
 * Created by ADMIN on 14-Feb-18.
 */

public class ContentUtil {

    public static void copyToTemp() {
        UtilityServices.appendLog("copy content to temp");
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + "/DD");
            File newDir = new File(Environment.getExternalStorageDirectory() + "/DDTemp");
            if (dir.exists()) {
                dir.renameTo(newDir);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void copyFromTemp() {
        UtilityServices.appendLog("copy content from temp");
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + "/DDTemp");
            File newDir = new File(Environment.getExternalStorageDirectory() + "/DD");
            if (dir.exists()) {
                dir.renameTo(newDir);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static void deleteAllFromDir(File dir) throws Exception {
        UtilityServices.appendLog("deleting all content of dir");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files.length > 0) {// delete these files
                for (File file : files) {
                    file.delete();
                }
            }
        }

    }

    public static void deleteDir(File dir) {
        UtilityServices.appendLog("deleting dir");
        try {
            if (dir.exists() && dir.isDirectory()) {
                deleteAllFromDir(dir);
                if (dir.listFiles().length == 0) {
                    dir.delete();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
