/*
 * Quiet Time - Wear Ringer Mode Sync
 * Copyright (C) 2014 Bryan Emmanuel
 *
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.quiettime.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
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

    public static final String WEAR_PATH_ZEN_MODE = "/zenmode";
    public static final String WEAR_PATH_SETTINGS = "/settings";
    public static final String WEAR_PATH_DEBUG = "/debug";
    public static final String WEAR_PATH_START_ZEN_WATCHER = "/startzenwatcher";

    public static final String KEY_IN_ZEN_MODE = "in_zen_mode";

    public static void syncBooleanSetting(@NonNull GoogleApiClient googleApiClient, @NonNull String key, boolean value) {
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEAR_PATH_SETTINGS);
            dataMapRequest.getDataMap().putBoolean(key, value);
            PutDataRequest request = dataMapRequest.asPutDataRequest();

            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                    .putDataItem(googleApiClient, request);
        }
    }

    public static void syncIntegerSetting(@NonNull GoogleApiClient googleApiClient, @NonNull String key, int value) {
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEAR_PATH_SETTINGS);
            dataMapRequest.getDataMap().putInt(key, value);
            PutDataRequest request = dataMapRequest.asPutDataRequest();

            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                    .putDataItem(googleApiClient, request);
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