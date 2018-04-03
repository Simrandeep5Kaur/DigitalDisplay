package com.kmsg.digitaldisplay.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.kmsg.digitaldisplay.data.Content;
import com.kmsg.digitaldisplay.data.ContentLog;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ADMIN on 27-Nov-17.
 * database file
 */

public class DBHelper extends SQLiteOpenHelper {

    private String CREATE_CONTENT_TABLE = "CREATE TABLE IF NOT EXISTS Content( " +
            "contentId INTEGER, " +
            "pathContent VARCHAR," +
            "pathLocalContent VARCHAR );";

    private String CREATE_CONTENT_LOG = "CREATE TABLE IF NOT EXISTS ContentLog(" +
            "contentId INTEGER, " +
            "dtTm VARCHAR );";


    private SQLiteDatabase tm;

    public DBHelper(Context context) {
        super(context, "DigitalDisplayApp", null, 10965);
        tm = this.getWritableDatabase();

        // check if table exists
        Cursor cur = tm.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Content';", null);
        if (cur != null) {
            if (cur.getCount() <= 0) {
                UtilityServices.appendLog("table does not exists");
                onCreate(tm);
            }
            cur.close();
        }
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CONTENT_TABLE);
        db.execSQL(CREATE_CONTENT_LOG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
//            db.execSQL("DROP TABLE IF EXISTS Content");
//            db.execSQL("DROP TABLE IF EXISTS ContentLog");
            onCreate(db);
            /// reset time flag so app will get latest content as old content will be lost when we drop
//            SharedPrefManager.putString("LastUpdatedOnServer", "");
        }
    }

    public void saveContents(List<Content> contents) {
        UtilityServices.appendLog("save content");
        try {
            tm = this.getWritableDatabase();

            tm.execSQL("DELETE FROM Content");
            ContentValues values = new ContentValues();
            tm.beginTransaction();
            for (Content content : contents) {
                values.clear();
                values.put("contentId", content.getContentId());
                values.put("pathContent", content.getPathContent());
                values.put("pathLocalContent", content.getPathLocalContent());
                tm.insert("Content", null, values);
            }
            tm.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            tm.endTransaction();
        }
        if (tm != null) {
            tm.close();
        }
    }

    public void saveContentLog(ContentLog log) {
        UtilityServices.appendLog("save log");
        try {
            tm = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("contentId", log.getContentId());
            values.put("dtTm", log.getDtTm());
            tm.insert("ContentLog", null, values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (tm != null) {
            tm.close();
        }
    }


    public List<Content> getLocalContent() {
        UtilityServices.appendLog("get local content");

        List<Content> contentList = new ArrayList<>();
        try {

            tm = this.getReadableDatabase();

            Cursor cur = tm.rawQuery("SELECT contentId, pathLocalContent FROM Content", null);
            if (cur.moveToFirst()) {
                do {
                    contentList.add(new Content(
                            cur.getInt(cur.getColumnIndex("contentId")),
                            cur.getString(cur.getColumnIndex("pathLocalContent")))
                    );
                } while (cur.moveToNext());
                cur.close();
            }
            if (tm != null) {
                tm.close();
            }
        } catch (Exception e) {
            UtilityServices.appendLog("exception in db: " + e.getMessage());
            e.printStackTrace();
        }
        return contentList;
    }

    public List<ContentLog> getContentLog() {
        UtilityServices.appendLog("get content log");

        List<ContentLog> contentLogs = new ArrayList<>();
        try {
            tm = this.getReadableDatabase();

            Cursor cur = tm.rawQuery("SELECT contentId, dtTm FROM ContentLog", null);
            if (cur.moveToFirst()) {
                do {
                    contentLogs.add(new ContentLog(
                            cur.getInt(cur.getColumnIndex("contentId")),
                            cur.getString(cur.getColumnIndex("dtTm")))
                    );
                } while (cur.moveToNext());
                cur.close();
            }
            if (tm != null) {
                tm.close();
            }
        } catch (Exception e) {
            UtilityServices.appendLog("exception in db: " + e.getMessage());
            e.printStackTrace();
        }
        return contentLogs;
    }

    public void deleteContentLog() {
        try {
            tm = this.getReadableDatabase();
            tm.execSQL("DELETE FROM ContentLog");

            if (tm != null) {
                tm.close();
            }
        } catch (Exception e) {
            UtilityServices.appendLog("exception in db: " + e.getMessage());
            e.printStackTrace();
        }
    }


//    public List<String> getLocalContent() {
//        UtilityServices.appendLog("get local content");
//
//        List<String> contentList = new ArrayList<>();
//        try {
//
//            tm = this.getReadableDatabase();
//
//            Cursor cur = tm.rawQuery("SELECT pathLocalContent FROM Content", null);
//            if (cur.moveToFirst()) {
//                do {
//                    contentList.add(
//                            cur.getString(cur.getColumnIndex("pathLocalContent"))
//                    );
//                } while (cur.moveToNext());
//                cur.close();
//            }
//            if (tm != null) {
//                tm.close();
//            }
//        } catch (Exception e) {
//            UtilityServices.appendLog("exception in db: " + e.getMessage());
//            e.printStackTrace();
//        }
//        return contentList;
//    }

}
