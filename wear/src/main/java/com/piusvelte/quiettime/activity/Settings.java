package com.piusvelte.quiettime.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.CheckBox;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.piusvelte.quiettime.R;
import com.piusvelte.quiettime.service.ZenModeWatcher;
import com.piusvelte.quiettime.utils.DataHelper;

public class Settings extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {

    private SharedPreferences mSharedPreferences;
    private CheckBox mChkWearToMobile;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Wearable.API)
                .build();

        mSharedPreferences = DataHelper.getSharedPreferences(this);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mChkWearToMobile = (CheckBox) findViewById(R.id.chk_wear_to_mobile);
                mChkWearToMobile.setChecked(DataHelper.PREFERENCE.PREF_WEAR_TO_MOBILE_ENABLED.isEnabled(mSharedPreferences));
                mChkWearToMobile.setOnClickListener(Settings.this);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (DataHelper.PREFERENCE.PREF_WEAR_TO_MOBILE_ENABLED.value.equals(key)) {
            mChkWearToMobile.setChecked(DataHelper.PREFERENCE.PREF_WEAR_TO_MOBILE_ENABLED.isEnabled(mSharedPreferences));
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mChkWearToMobile) {
            DataHelper.PREFERENCE prefKey = DataHelper.PREFERENCE.PREF_WEAR_TO_MOBILE_ENABLED;
            prefKey.setEnabled(mSharedPreferences, mChkWearToMobile.isChecked());
            prefKey.syncEnabled(mGoogleApiClient, mChkWearToMobile.isChecked());
        }
    }
}
