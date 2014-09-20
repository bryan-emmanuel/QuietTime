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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.piusvelte.quiettime.utils.DataHelper;
import com.piusvelte.quiettime.utils.PreferencesHelper;

import java.util.List;

/**
 * Created by bemmanuel on 8/16/14.
 */
public class WearDataListenerService extends WearableListenerService {

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
            } else if (DataHelper.WEAR_PATH_ZEN_MODE.equals(path)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap dataMap = dataMapItem.getDataMap();
                boolean inZenMode = dataMap.getBoolean(DataHelper.KEY_IN_ZEN_MODE);
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                if (inZenMode) {
                    if (PreferencesHelper.isMutePhoneEnabled(sharedPreferences)) {
                        int muteMode = PreferencesHelper.getMutePhoneMode(sharedPreferences);

                        if (muteMode != audioManager.getRingerMode()) {
                            alertRingerChange(sharedPreferences);
                            audioManager.setRingerMode(muteMode);
                        }
                    }
                } else if (PreferencesHelper.isUnmutePhoneEnabled(sharedPreferences)
                        && audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                    alertRingerChange(sharedPreferences);
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
            }
        }
    }

    private void alertRingerChange(@NonNull SharedPreferences sharedPreferences) {
        if (PreferencesHelper.isPhoneVibrateConfirmEnabled(sharedPreferences)) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(150L);
        }
    }
}