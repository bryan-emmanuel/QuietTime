package com.piusvelte.quiettime.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.piusvelte.quiettime.BuildConfig;

import java.nio.ByteBuffer;

/**
 * Created by bemmanuel on 8/16/14.
 */
public class DataHelper {

    private static final String TAG = DataHelper.class.getSimpleName();

    public static final String PREFS_NAME = "settings";

    public static final String WEAR_PATH_ZEN_MODE = "/zenmode";
    public static final String WEAR_PATH_SETTINGS = "/settings";
    public static final String WEAR_PATH_DEBUG = "/debug";
    public static final String WEAR_PATH_START_ZEN_WATCHER = "/startzenwatcher";

    public static final String KEY_IN_ZEN_MODE = "in_zen_mode";

    public enum PREFERENCE {
        PREF_WEAR_TO_MOBILE_ENABLED("wear_to_mobile_enabled", true);

        public String value;
        public boolean isEnabledDefault;

        PREFERENCE(@NonNull String value, boolean isEnabledDefault) {
            this.value = value;
            this.isEnabledDefault = isEnabledDefault;
        }

        public boolean isEnabled(SharedPreferences sharedPreferences) {
            return sharedPreferences.getBoolean(value, isEnabledDefault);
        }

        public boolean isEnabled(Context context) {
            return isEnabled(getSharedPreferences(context));
        }

        public void setEnabled(SharedPreferences sharedPreferences, boolean isEnabled) {
            sharedPreferences
                    .edit()
                    .putBoolean(value, isEnabled)
                    .apply();
        }

        public void syncEnabled(@NonNull GoogleApiClient googleApiClient, boolean isEnabled) {
            if (googleApiClient.isConnected()) {
                PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEAR_PATH_SETTINGS);
                dataMapRequest.getDataMap().putBoolean(value, isEnabled);
                PutDataRequest request = dataMapRequest.asPutDataRequest();

                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                        .putDataItem(googleApiClient, request);
            }
        }
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void storeFromDataMap(@NonNull SharedPreferences sharedPreferences, @NonNull DataMap dataMap) {
        for (PREFERENCE key : PREFERENCE.values()) {
            if (dataMap.containsKey(key.value))
                key.setEnabled(sharedPreferences, dataMap.getBoolean(key.value, key.isEnabledDefault));
        }
    }

    public static void syncZenMode(@NonNull GoogleApiClient googleApiClient, boolean inZenMode) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEAR_PATH_ZEN_MODE);
        dataMapRequest.getDataMap().putBoolean(KEY_IN_ZEN_MODE, inZenMode);
        PutDataRequest request = dataMapRequest.asPutDataRequest();

        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                .putDataItem(googleApiClient, request);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "sync zen mode success? " + dataItemResult.getStatus().isSuccess());
            }
        });
    }

    public static void sendDebug(@NonNull GoogleApiClient googleApiClient, String nodeId, long homePreferencesSize) {
        if (googleApiClient.isConnected()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "sendDebug: " + homePreferencesSize);
            byte[] payload = ByteBuffer.allocate(8).putLong(homePreferencesSize).array();

            PendingResult<MessageApi.SendMessageResult> pendingResult = Wearable.MessageApi
                    .sendMessage(googleApiClient, nodeId, WEAR_PATH_DEBUG, payload);
            pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "send debug success? " + sendMessageResult.getStatus().isSuccess());
                    }
                }
            });
        }
    }
}