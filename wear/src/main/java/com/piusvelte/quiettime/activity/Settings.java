package com.piusvelte.quiettime.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.wearable.view.WatchViewStub;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.piusvelte.quiettime.R;
import com.piusvelte.quiettime.service.ZenModeWatcher;
import com.piusvelte.quiettime.utils.DataHelper;
import com.piusvelte.quiettime.utils.PreferencesHelper;

public class Settings extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener,RadioGroup.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener {

    private SharedPreferences mSharedPreferences;
    private RadioGroup mGroupMute;
    private CheckBox mChkUnmute;
    private CheckBox mChkConfirm;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Wearable.API)
                .build();

        mSharedPreferences = PreferencesHelper.getSharedPreferences(this);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mGroupMute = (RadioGroup) findViewById(R.id.grp_phone_ringer);
                setMuteSelection(PreferencesHelper.getMutePhoneMode(mSharedPreferences));

                mChkUnmute = (CheckBox) findViewById(R.id.chk_watch_unmute);
                mChkUnmute.setChecked(PreferencesHelper.isUnmutePhoneEnabled(mSharedPreferences));
                mChkUnmute.setOnCheckedChangeListener(Settings.this);

                mChkConfirm = (CheckBox) findViewById(R.id.chk_confirm);
                mChkConfirm.setChecked(PreferencesHelper.isPhoneVibrateConfirmEnabled(mSharedPreferences));
                mChkConfirm.setOnCheckedChangeListener(Settings.this);
            }
        });

        // make sure the service gets started
        startService(new Intent(this, ZenModeWatcher.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void setMuteSelection(int preferenceValue) {
        mGroupMute.setOnCheckedChangeListener(null);

        switch (preferenceValue) {
            case AudioManager.RINGER_MODE_SILENT:
                mGroupMute.check(R.id.rdb_silent);
                break;

            case AudioManager.RINGER_MODE_VIBRATE:
                mGroupMute.check(R.id.rdb_vibrate);
                break;

            default:
                mGroupMute.check(R.id.rdb_none);
                break;
        }

        mGroupMute.setOnCheckedChangeListener(this);
    }

    private int getMuteMode(@IdRes int resourceId) {
        switch (resourceId) {
            case R.id.rdb_silent:
                return AudioManager.RINGER_MODE_SILENT;

            case R.id.rdb_vibrate:
                return AudioManager.RINGER_MODE_VIBRATE;

            default:
                return AudioManager.RINGER_MODE_NORMAL;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesHelper.PREF_MUTE_PHONE_MODE.equals(key)) {
            setMuteSelection(PreferencesHelper.getMutePhoneMode(sharedPreferences));
        } else if (PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED.equals(key)) {
            mChkUnmute.setOnCheckedChangeListener(null);
            mChkUnmute.setChecked(PreferencesHelper.isUnmutePhoneEnabled(sharedPreferences));
            mChkUnmute.setOnCheckedChangeListener(this);
        } else if (PreferencesHelper.PREF_PHONE_VIBRATE_CONFIRM_ENABLED.equals(key)) {
            mChkConfirm.setOnCheckedChangeListener(null);
            mChkConfirm.setChecked(PreferencesHelper.isPhoneVibrateConfirmEnabled(sharedPreferences));
            mChkConfirm.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        int muteMode = getMuteMode(checkedId);

        if (muteMode != PreferencesHelper.getMutePhoneMode(mSharedPreferences)) {
            mSharedPreferences.edit()
                    .putInt(PreferencesHelper.PREF_MUTE_PHONE_MODE, muteMode)
                    .apply();
            DataHelper.syncIntegerSetting(mGoogleApiClient, PreferencesHelper.PREF_MUTE_PHONE_MODE, muteMode);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mChkUnmute) {
            if (isChecked != PreferencesHelper.isUnmutePhoneEnabled(mSharedPreferences)) {
                mSharedPreferences.edit()
                        .putBoolean(PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED, isChecked)
                        .apply();
                DataHelper.syncBooleanSetting(mGoogleApiClient, PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED, isChecked);
            }
        } else if (buttonView == mChkConfirm) {
            if (isChecked != PreferencesHelper.isPhoneVibrateConfirmEnabled(mSharedPreferences)) {
                mSharedPreferences.edit()
                        .putBoolean(PreferencesHelper.PREF_PHONE_VIBRATE_CONFIRM_ENABLED, isChecked)
                        .apply();
                DataHelper.syncBooleanSetting(mGoogleApiClient, PreferencesHelper.PREF_PHONE_VIBRATE_CONFIRM_ENABLED, isChecked);
            }
        }
    }
}
