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
package com.piusvelte.quiettime.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.piusvelte.quiettime.BuildConfig;
import com.piusvelte.quiettime.utils.DataHelper;
import com.piusvelte.quiettime.utils.PreferencesHelper;

import java.util.List;

/**
 * Created by bemmanuel on 8/16/14.
 */
public class WearDataListenerService extends WearableListenerService {

    private static final String TAG = WearDataListenerService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        SharedPreferences sharedPreferences = PreferencesHelper.getSharedPreferences(this);

        for (DataEvent event : events) {
            DataItem dataItem = event.getDataItem();
            String path = dataItem.getUri().getPath();

            if (DataHelper.WEAR_PATH_SETTINGS.equals(path)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap dataMap = dataMapItem.getDataMap();
                PreferencesHelper.storeFromDataMap(sharedPreferences, dataMap);
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if (BuildConfig.DEBUG) Log.d(TAG, "message received: " + messageEvent.getPath());

        if (DataHelper.WEAR_PATH_SYNC.equals(messageEvent.getPath())) {
            final String nodeId = messageEvent.getSourceNodeId();
            final long homePreferencesLength = ZenModeWatcher.getHomePreferencesLength();
            SharedPreferences sharedPreferences = PreferencesHelper.getSharedPreferences(this);
            PreferencesHelper.setInZenModeHomePreferencesLength(sharedPreferences, homePreferencesLength);

            if (mGoogleApiClient != null) {
                if (mGoogleApiClient.isConnected()) {
                    DataHelper.sendSync(mGoogleApiClient, nodeId, homePreferencesLength);
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "connect client");
                    mGoogleApiClient.connect();
                }
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "create client");
                mGoogleApiClient = new GoogleApiClient
                        .Builder(this)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                DataHelper.sendSync(mGoogleApiClient, nodeId, homePreferencesLength);
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // NO-OP
                            }
                        })
                        .build();

                mGoogleApiClient.connect();
            }
        } else if (DataHelper.WEAR_PATH_START_ZEN_WATCHER.equals(messageEvent.getPath())) {
            startService(new Intent(this, ZenModeWatcher.class));
        }
    }
}