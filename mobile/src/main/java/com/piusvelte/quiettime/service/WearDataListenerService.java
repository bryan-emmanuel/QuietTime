package com.piusvelte.quiettime.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.piusvelte.quiettime.BuildConfig;
import com.piusvelte.quiettime.utils.DataHelper;

import java.util.List;

/**
 * Created by bemmanuel on 8/16/14.
 */
public class WearDataListenerService extends WearableListenerService {

    private static final String TAG = WearDataListenerService.class.getSimpleName();

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        if (BuildConfig.DEBUG) Log.d(TAG, "onDataChanged, events: " + events.size());
        SharedPreferences sharedPreferences = DataHelper.getSharedPreferences(this);

        for (DataEvent event : events) {
            DataItem dataItem = event.getDataItem();
            String path = dataItem.getUri().getPath();

            if (DataHelper.WEAR_PATH_SETTINGS.equals(path)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap dataMap = dataMapItem.getDataMap();
                DataHelper.storeFromDataMap(sharedPreferences, dataMap);
            } else if (DataHelper.WEAR_PATH_ZEN_MODE.equals(path) && DataHelper.PREFERENCE.PREF_WEAR_TO_MOBILE_ENABLED.isEnabled(this)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap dataMap = dataMapItem.getDataMap();
                boolean inZenMode = dataMap.getBoolean(DataHelper.KEY_IN_ZEN_MODE);
                int ringerMode = inZenMode ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL;
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                if (ringerMode != audioManager.getRingerMode()) {
                    audioManager.setRingerMode(ringerMode);
                }
            }
        }
    }
}