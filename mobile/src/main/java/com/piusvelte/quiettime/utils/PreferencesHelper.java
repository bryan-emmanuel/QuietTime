package com.piusvelte.quiettime.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.support.annotation.NonNull;

import com.google.android.gms.wearable.DataMap;

/**
 * Created by bemmanuel on 8/29/14.
 */
public class PreferencesHelper {

    public static final String PREFS_NAME = "settings";
    public static final String PREF_MUTE_PHONE_MODE = "mute_phone_mode";
    public static final String PREF_UNMUTE_PHONE_ENABLED = "unmute_phone_enabled";

    private static final int PREF_MUTE_PHONE_MODE_DEFAULT = AudioManager.RINGER_MODE_SILENT;
    private static final boolean PREF_UNMUTE_PHONE_ENABLED_DEFAULT = true;

    private PreferencesHelper() {
        // NO-OP
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getMutePhoneMode(@NonNull SharedPreferences sharedPreferences) {
        return sharedPreferences.getInt(PREF_MUTE_PHONE_MODE, PREF_MUTE_PHONE_MODE_DEFAULT);
    }

    public static boolean isMutePhoneEnabled(@NonNull SharedPreferences sharedPreferences) {
        return getMutePhoneMode(sharedPreferences) != AudioManager.RINGER_MODE_NORMAL;
    }

    public static boolean isUnmutePhoneEnabled(@NonNull SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(PREF_UNMUTE_PHONE_ENABLED, PREF_UNMUTE_PHONE_ENABLED_DEFAULT);
    }

    public static void storeFromDataMap(@NonNull SharedPreferences sharedPreferences, @NonNull DataMap dataMap) {
        if (dataMap.containsKey(PREF_MUTE_PHONE_MODE)) {
            int muteMode = dataMap.getInt(PREF_MUTE_PHONE_MODE, PREF_MUTE_PHONE_MODE_DEFAULT);

            if (muteMode != PreferencesHelper.getMutePhoneMode(sharedPreferences)) {
                sharedPreferences.edit()
                        .putInt(PREF_MUTE_PHONE_MODE, muteMode)
                        .apply();
            }
        }

        if (dataMap.containsKey(PREF_UNMUTE_PHONE_ENABLED)) {
            boolean isEnabled = dataMap.getBoolean(PREF_UNMUTE_PHONE_ENABLED, PREF_UNMUTE_PHONE_ENABLED_DEFAULT);

            if (isEnabled != PreferencesHelper.isUnmutePhoneEnabled(sharedPreferences)) {
                sharedPreferences.edit()
                        .putBoolean(PREF_UNMUTE_PHONE_ENABLED, isEnabled)
                        .apply();
            }
        }
    }
}
