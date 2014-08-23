package com.piusvelte.quiettime.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.piusvelte.quiettime.BuildConfig;

import java.util.List;

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

    @Nullable
    public static void requestConnectedNodes(@NonNull GoogleApiClient googleApiClient,
                                             @NonNull ResultCallback<NodeApi.GetConnectedNodesResult> callback) {
        Wearable.NodeApi
                .getConnectedNodes(googleApiClient)
                .setResultCallback(callback);
    }

    public static void requestDebug(@NonNull GoogleApiClient googleApiClient, @NonNull List<Node> nodes,
                                    @NonNull ResultCallback<MessageApi.SendMessageResult> callback) {
        for (Node node : nodes) {
            Wearable.MessageApi
                    .sendMessage(googleApiClient, node.getId(), WEAR_PATH_DEBUG, null)
                    .setResultCallback(callback);
        }
    }

    public static void startZenWatcher(@NonNull final GoogleApiClient googleApiClient) {
        Wearable.NodeApi
                .getConnectedNodes(googleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        if (getConnectedNodesResult != null) {
                            List<Node> nodes = getConnectedNodesResult.getNodes();
                            ResultCallback<MessageApi.SendMessageResult> callback = new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                    if (BuildConfig.DEBUG) {
                                        Log.d(TAG, "start zen watcher result: " + sendMessageResult.getStatus().isSuccess());
                                    }
                                }
                            };

                            for (Node node : nodes) {
                                if (googleApiClient.isConnected()) {
                                    Wearable.MessageApi
                                            .sendMessage(googleApiClient, node.getId(), WEAR_PATH_START_ZEN_WATCHER, null)
                                            .setResultCallback(callback);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                });
    }
}