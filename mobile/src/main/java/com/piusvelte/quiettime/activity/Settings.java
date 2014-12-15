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
package com.piusvelte.quiettime.activity;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.piusvelte.quiettime.BuildConfig;
import com.piusvelte.quiettime.R;
import com.piusvelte.quiettime.fragment.GenericDialog;
import com.piusvelte.quiettime.fragment.SyncDialog;
import com.piusvelte.quiettime.utils.DataHelper;
import com.piusvelte.quiettime.utils.PreferencesHelper;

import java.nio.ByteBuffer;
import java.util.List;


public class Settings extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener, MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, RadioGroup.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener {

    private static final String TAG = Settings.class.getSimpleName();
    private static final int NUE_VERSION = 1;

    private SharedPreferences mSharedPreferences;
    private RadioGroup mGroupMute;
    private CheckBox mChkUnmute;
    private CheckBox mChkConfirm;

    private GoogleApiClient mGoogleApiClient;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mSharedPreferences = PreferencesHelper.getSharedPreferences(this);

        mGroupMute = (RadioGroup) findViewById(R.id.grp_phone_ringer);
        setMuteSelection(PreferencesHelper.getMutePhoneMode(mSharedPreferences));

        mChkUnmute = (CheckBox) findViewById(R.id.chk_watch_unmute);
        mChkUnmute.setChecked(PreferencesHelper.isUnmutePhoneEnabled(mSharedPreferences));
        mChkUnmute.setOnCheckedChangeListener(this);

        mChkConfirm = (CheckBox) findViewById(R.id.chk_confirm);
        mChkConfirm.setChecked(PreferencesHelper.isPhoneVibrateConfirmEnabled(mSharedPreferences));
        mChkConfirm.setOnCheckedChangeListener(this);

        GoogleAnalytics ga = GoogleAnalytics.getInstance(this);
        mTracker = ga.newTracker("UA-57650363-1");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSharedPreferences.getInt(PreferencesHelper.PREF_SHOW_NUE, 0) < NUE_VERSION) {
            GenericDialog.newInstance(R.string.nue_title, R.string.nue_message)
                    .show(getSupportFragmentManager(), "dialog:nue");
            mSharedPreferences.edit()
                    .putInt(PreferencesHelper.PREF_SHOW_NUE, NUE_VERSION)
                    .apply();
        }

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_about:
                GenericDialog.newInstance(R.string.action_about, R.string.about_message)
                        .show(getSupportFragmentManager(), "dialog:about");
                return true;

            case R.id.action_sync:
                new SyncDialog()
                        .show(getSupportFragmentManager(), "dialog:sync");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "muteMode changed to: " + PreferencesHelper.getMutePhoneMode(sharedPreferences));
            }

            setMuteSelection(PreferencesHelper.getMutePhoneMode(sharedPreferences));
        } else if (PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED.equals(key)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "unmute changed to: " + PreferencesHelper.isUnmutePhoneEnabled(sharedPreferences));
            }

            mChkUnmute.setOnCheckedChangeListener(null);
            mChkUnmute.setChecked(PreferencesHelper.isUnmutePhoneEnabled(sharedPreferences));
            mChkUnmute.setOnCheckedChangeListener(this);
        } else if (PreferencesHelper.PREF_PHONE_VIBRATE_CONFIRM_ENABLED.equals(key)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "confirm changed to: " + PreferencesHelper.isPhoneVibrateConfirmEnabled(sharedPreferences));
            }

            mChkConfirm.setOnCheckedChangeListener(null);
            mChkConfirm.setChecked(PreferencesHelper.isPhoneVibrateConfirmEnabled(sharedPreferences));
            mChkConfirm.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (BuildConfig.DEBUG) Log.d(TAG, "GoogleApiClient connected");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        DataHelper.startZenWatcher(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // NO-OP
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(Settings.this, R.string.error_not_connected, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onMessageReceived: " + messageEvent.getPath());

        if (DataHelper.WEAR_PATH_SYNC.equals(messageEvent.getPath())) {
            long homePreferencesSize = ByteBuffer.wrap(messageEvent.getData()).getLong();

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "received homePreferencesSize: " + homePreferencesSize);
            }

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(Settings.class.getSimpleName())
                    .setAction("Sync")
                    .setLabel("home_preferences")
                    .setValue(homePreferencesSize)
                    .build());
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        int muteMode = getMuteMode(checkedId);

        if (muteMode != PreferencesHelper.getMutePhoneMode(mSharedPreferences)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "save muteMode: " + muteMode);
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
                if (BuildConfig.DEBUG) Log.d(TAG, "save unmute: " + isChecked);
                mSharedPreferences.edit()
                        .putBoolean(PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED, isChecked)
                        .apply();
                DataHelper.syncBooleanSetting(mGoogleApiClient, PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED, isChecked);
            }
        } else if (buttonView == mChkConfirm) {
            if (isChecked != PreferencesHelper.isPhoneVibrateConfirmEnabled(mSharedPreferences)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "save confirm: " + isChecked);
                mSharedPreferences.edit()
                        .putBoolean(PreferencesHelper.PREF_PHONE_VIBRATE_CONFIRM_ENABLED, isChecked)
                        .apply();
                DataHelper.syncBooleanSetting(mGoogleApiClient, PreferencesHelper.PREF_PHONE_VIBRATE_CONFIRM_ENABLED, isChecked);
            }
        }
    }

    public void startSync() {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(Settings.this, R.string.error_not_connected, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(Settings.this, R.string.message_request_debug, Toast.LENGTH_SHORT).show();
            ResultCallback<NodeApi.GetConnectedNodesResult> connectedNodesResultResultCallback =
                    new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                        @Override
                        public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                            if (getConnectedNodesResult == null) {
                                Toast.makeText(Settings.this, R.string.error_no_watches, Toast.LENGTH_SHORT).show();
                            } else {
                                List<Node> nodes = getConnectedNodesResult.getNodes();
                                int connectedNodesSize = nodes.size();

                                if (connectedNodesSize == 0) {
                                    Toast.makeText(Settings.this, R.string.error_no_watches, Toast.LENGTH_SHORT).show();
                                } else if (mGoogleApiClient.isConnected()) {
                                    if (BuildConfig.DEBUG) {
                                        Log.d(TAG, "found " + connectedNodesSize + " connected nodes");
                                    }

                                    ResultCallback<MessageApi.SendMessageResult> sendMessageResultResultCallback =
                                            new ResultCallback<MessageApi.SendMessageResult>() {
                                                @Override
                                                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                                    if (BuildConfig.DEBUG) {
                                                        Log.d(TAG, "send message result success: " + sendMessageResult.getStatus()
                                                                .isSuccess());
                                                    }

                                                    if (!sendMessageResult.getStatus().isSuccess()) {
                                                        if (BuildConfig.DEBUG) {
                                                            Log.d(TAG, "sent message failed");
                                                        }
                                                    }
                                                }
                                            };

                                    DataHelper.requestSync(mGoogleApiClient, nodes, sendMessageResultResultCallback);
                                } else {
                                    Toast.makeText(Settings.this, R.string.error_not_connected, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    };

            DataHelper.requestConnectedNodes(mGoogleApiClient, connectedNodesResultResultCallback);
        }
    }
}
