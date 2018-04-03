package com.kmsg.digitaldisplay.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import com.kmsg.digitaldisplay.R;
import com.kmsg.digitaldisplay.data.Content;
import com.kmsg.digitaldisplay.data.ContentLog;
import com.kmsg.digitaldisplay.database.DBHelper;
import com.kmsg.digitaldisplay.receiver.UpdateAlarmReceiver;
import com.kmsg.digitaldisplay.services.GetNewContent;
import com.kmsg.digitaldisplay.util.Constants;
import com.kmsg.digitaldisplay.util.SharedPrefManager;
import com.kmsg.digitaldisplay.util.UtilityServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ADMIN on 27-Nov-17.
 * play content here
 */

public class VideoActivity extends Activity {

    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 200;
    private static final String TAG = "DD";
    View decorView;
    private VideoView mVideoView;

    private VideoActivity.LeanbackPlaybackState mPlaybackState = VideoActivity.LeanbackPlaybackState.IDLE;
    private String url;
    private List<Content> contentList = new ArrayList<>();
    private int pos = 0;

    private DBHelper dbHelper;
    private AlarmManager alarmManager = null;
    private PendingIntent alarmIntent = null;
    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Do something with this incoming message here
            // Since we will process the message and update the UI, we don't need to show a message in Status Bar
            // To do this, we call abortBroadcast()

            UtilityServices.appendLog("update notification is received so update the content");

            updateContent();
            abortBroadcast();
        }
    };
    private BroadcastReceiver mCopyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Do something with this incoming message here
            // Since we will process the message and update the UI, we don't need to show a message in Status Bar
            // To do this, we call abortBroadcast()

            UtilityServices.appendLog("copy new content broadcast is received.");

            copyNewContent(intent.getStringExtra("LastUpdatedOnServer"), intent.getStringExtra("Content"));
            abortBroadcast();
        }
    };
    private BroadcastReceiver mExpiryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Do something with this incoming message here
            // Since we will process the message and update the UI, we don't need to show a message in Status Bar
            // To do this, we call abortBroadcast()

            UtilityServices.appendLog("expired broadcast is received.");

            Toast.makeText(context, R.string.txt_device_expired, Toast.LENGTH_LONG).show();
            abortBroadcast();
            VideoActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        decorView = this.getWindow().getDecorView();
        SharedPrefManager.getSharedPreferences(this);

        init();
        scheduleAppStateUpdate();
        registerReceiver(mNotificationReceiver, new IntentFilter(Constants.ACTION_UPDATE_NOTIFICATION));
        registerReceiver(mExpiryReceiver, new IntentFilter(Constants.ACTION_SET_EXPIRED));
        registerReceiver(mCopyReceiver, new IntentFilter(Constants.ACTION_COPY_NEW));

        dbHelper = new DBHelper(this);
        contentList = dbHelper.getLocalContent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions()) {
                setData();
            } else {
                requestPermissions();
            }
        } else {
            setData();
        }


//                  /* testing only */
//        Toast.makeText(VideoActivity.this, "version code is: "+ UtilityServices.getAppVersionNo(VideoActivity.this), Toast.LENGTH_LONG).show();

    }

    @Override
    public void onDestroy() {
        mVideoView.suspend();
        cancelAppStateUpdate();
        unregisterReceiver(mNotificationReceiver);
        unregisterReceiver(mExpiryReceiver);
        unregisterReceiver(mCopyReceiver);
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                togglePlayback(false);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                togglePlayback(false);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mPlaybackState == VideoActivity.LeanbackPlaybackState.PLAYING) {
                    togglePlayback(false);
                } else {
                    togglePlayback(true);
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    public void togglePlayback(boolean playPause) {
        if (playPause) {
            onFragmentPlayPause(url, true);
//            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.PAUSE));
        } else {
            onFragmentPlayPause(url, false);
//            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.PLAY));
        }
    }

    public void onFragmentPlayPause(String url, Boolean playPause) {
        mVideoView.setVideoPath(url != null ? url : "");

        if (mPlaybackState == VideoActivity.LeanbackPlaybackState.IDLE) {
            setupCallbacks();
            mPlaybackState = VideoActivity.LeanbackPlaybackState.IDLE;
        }

        if (playPause && mPlaybackState != VideoActivity.LeanbackPlaybackState.PLAYING) {
            mPlaybackState = VideoActivity.LeanbackPlaybackState.PLAYING;
            mVideoView.start();
        } else {
            mPlaybackState = VideoActivity.LeanbackPlaybackState.PAUSED;
            mVideoView.pause();
        }
    }

    private void init() {
        mVideoView = (VideoView) findViewById(R.id.videoView2);
        mVideoView.setFocusable(false);
        mVideoView.setFocusableInTouchMode(false);
    }

    private void setData() {
        if (contentList.size() > 0) {
            url = contentList.get(pos).getPathLocalContent();
            mVideoView.setVideoPath(url != null ? url : "");
            mVideoView.requestFocus();
            mVideoView.start();
            dbHelper.saveContentLog(new ContentLog(contentList.get(pos).getContentId(),
                    DateFormat.format("dd-MM-yy hh:mm:ss", System.currentTimeMillis()).toString()));
            setupCallbacks();
        } else {
            showToast(getString(R.string.txt_no_content));
        }
    }

    private void setupCallbacks() {
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                String msg;
                if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                    msg = getString(R.string.video_error_media_load_timeout);
                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    msg = getString(R.string.video_error_server_inaccessible);
                } else {
                    msg = getString(R.string.video_error_unknown_error);
                }
                System.out.println("error: " + msg);
                mVideoView.stopPlayback();
                mPlaybackState = VideoActivity.LeanbackPlaybackState.IDLE;
                return false;
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mPlaybackState == VideoActivity.LeanbackPlaybackState.PLAYING) {
//                    System.out.println("view prepared");
                    mVideoView.start();
                }
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlaybackState = VideoActivity.LeanbackPlaybackState.IDLE;
                if (pos < contentList.size() - 1)
                    pos++;
                else
                    pos = 0;
                url = contentList.get(pos).getPathLocalContent();
                mVideoView.setVideoPath(url != null ? url : "");
                mVideoView.start();
                dbHelper.saveContentLog(new ContentLog(contentList.get(pos).getContentId(),
                        DateFormat.format("dd-MM-yy hh:mm:ss", System.currentTimeMillis()).toString()));
            }
        });
    }

    private void scheduleAppStateUpdate() {
//        update app state alarm
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, UpdateAlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        // Wake up the device to fire the alarm in 1 minutes, and every 10 minutes after that
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60000,
                600000,
                alarmIntent);
    }

    private void cancelAppStateUpdate() {
        // If the alarm has been set, cancel it.
        if (alarmManager != null && alarmIntent != null) {
            alarmManager.cancel(alarmIntent);
        }
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        try {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                setData();
            } else {
                // Permission denied.
                showToast(getString(R.string.permission_denied_explanation));
            }
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void updateContent() {
        // get new content and play content after that

        startService(new Intent(this, GetNewContent.class));

//        startActivity(new Intent(this, DownloadNewContentActivity.class));
//        this.finish();
    }

    private void copyNewContent(String lastUpdatedOnServer, String content) {
        startActivity(new Intent(this, CopyContentActivity.class)
                .putExtra("LastUpdatedOnServer", lastUpdatedOnServer)
                .putExtra("Content", content)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        this.finish();
    }

    private enum LeanbackPlaybackState {
        PLAYING, PAUSED, IDLE
    }


}