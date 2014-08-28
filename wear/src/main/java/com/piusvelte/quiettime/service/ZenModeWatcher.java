package com.piusvelte.quiettime.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.piusvelte.quiettime.BuildConfig;
import com.piusvelte.quiettime.receiver.ScreenReceiver;
import com.piusvelte.quiettime.utils.DataHelper;

import java.io.File;

/**
 * Created by bemmanuel on 8/22/14.
 */
public class ZenModeWatcher extends Service implements GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = ZenModeWatcher.class.getSimpleName();

    /**
     * known byte sizes for home_preferences.xml when in_zen_mode is set to true
     * 118 is when in_zen_mode is the only preference set
     * 165 is when peek_privacy_mode is also set
     */
    private static final long[] ZEN_MODE_TRUE = new long[]{118, 165};

    /**
     * path for home_preferences.xml
     */
    @SuppressLint("SdCardPath")
    private static final File HOME_PREFERENCES = new File("/data/data/com.google.android.wearable.app/shared_prefs/home_preferences.xml");

    /**
     * attempt to speed up sync by checking zen mode after the screen goes on, by this delay
     */
    private static final long SCREEN_ON_RUNNABLE_DELAY = DateUtils.SECOND_IN_MILLIS;

    private boolean mPreviouslyInZenMode;
    private Boolean mPendingZenModeChange;

    private BroadcastReceiver mScreenOffReceiver;

    private GoogleApiClient mGoogleApiClient;

    private Runnable mScreenOnRunnable = new Runnable() {
        @Override
        public void run() {
            checkZenMode();
            // continue to check this every second, until the screen turns off
            mScreenHandler.postDelayed(mScreenOnRunnable, SCREEN_ON_RUNNABLE_DELAY);
        }
    };

    private Handler mScreenHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        setInZenMode(inZenMode());

        IntentFilter filter = new IntentFilter();
        // for faster syncing, use screen off to post a delayed runnable
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // use screen off as a trigger to check zen mode
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenOffReceiver = new ScreenReceiver();
        registerReceiver(mScreenOffReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mScreenHandler.removeCallbacks(mScreenOnRunnable);
                checkZenMode();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mScreenHandler.postDelayed(mScreenOnRunnable, SCREEN_ON_RUNNABLE_DELAY);
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mScreenHandler.removeCallbacks(mScreenOnRunnable);
        unregisterReceiver(mScreenOffReceiver);
        if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    public static long getHomePreferencesSize() {
        return HOME_PREFERENCES.length();
    }

    private void checkZenMode() {
        boolean inZenMode = inZenMode();

        if (inZenMode ^ mPreviouslyInZenMode) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "zen mode change from " + mPreviouslyInZenMode + " to " + inZenMode);
            }

            setInZenMode(inZenMode);
        }
    }

    private static boolean inZenMode() {
        long homePreferencesSize = getHomePreferencesSize();
        if (BuildConfig.DEBUG) Log.d(TAG, "inZenMode, file size: " + homePreferencesSize);

        for (long zenModeValue : ZEN_MODE_TRUE) {
            if (homePreferencesSize == zenModeValue) return true;
        }

        return false;
    }

    private void setInZenMode(boolean inZenMode) {
        mPreviouslyInZenMode = inZenMode;

        if (DataHelper.PREFERENCE.PREF_WEAR_TO_MOBILE_ENABLED.isEnabled(this)) {
            if (mGoogleApiClient.isConnected()) {
                mPendingZenModeChange = null;
                DataHelper.syncZenMode(mGoogleApiClient, mPreviouslyInZenMode);
            } else {
                mPendingZenModeChange = mPreviouslyInZenMode;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mPendingZenModeChange != null && mGoogleApiClient.isConnected()) {
            DataHelper.syncZenMode(mGoogleApiClient, mPendingZenModeChange);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // NO-OP
    }
}
