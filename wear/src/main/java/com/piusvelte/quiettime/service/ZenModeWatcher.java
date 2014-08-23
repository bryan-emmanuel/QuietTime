package com.piusvelte.quiettime.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.piusvelte.quiettime.BuildConfig;
import com.piusvelte.quiettime.receiver.ScreenOffReceiver;
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
    private static final long[] ZEN_MODE_TRUE = new long[] {118, 165};

    /** path for home_preferences.xml */
    @SuppressLint("SdCardPath")
    private static final File HOME_PREFERENCES = new File("/data/data/com.google.android.wearable.app/shared_prefs/home_preferences.xml");

    private boolean mPreviouslyInZenMode;
    private Boolean mPendingZenModeChange;

    private BroadcastReceiver mScreenOffReceiver;

    private GoogleApiClient mGoogleApiClient;

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

        // use screen off as a trigger to check zen mode
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenOffReceiver = new ScreenOffReceiver();
        registerReceiver(mScreenOffReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            boolean inZenMode = inZenMode();

            if (inZenMode ^ mPreviouslyInZenMode) {
                if (BuildConfig.DEBUG) Log.d(TAG, "zen mode change from " + mPreviouslyInZenMode + " to " + inZenMode);
                setInZenMode(inZenMode);
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mScreenOffReceiver);
        if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    public static long getHomePreferencesSize() {
        return HOME_PREFERENCES.length();
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
