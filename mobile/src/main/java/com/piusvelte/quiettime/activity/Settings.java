package com.piusvelte.quiettime.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

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
import com.piusvelte.quiettime.fragment.AboutDialog;
import com.piusvelte.quiettime.utils.DataHelper;
import com.piusvelte.quiettime.utils.PreferencesHelper;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class Settings extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener, MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, RadioGroup.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener {

    private static final String TAG = Settings.class.getSimpleName();

    private SharedPreferences mSharedPreferences;
    private RadioGroup mGroupMute;
    private CheckBox mChkUnmute;

    private GoogleApiClient mGoogleApiClient;

    private int mConnectedNodesSize = 0;
    private List<Long> mDebugSizes = new ArrayList<Long>();

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
                new AboutDialog()
                        .show(getSupportFragmentManager(), "dialog:about");
                return true;

            case R.id.action_send_debug:
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
                                        mConnectedNodesSize = nodes.size();

                                        if (mConnectedNodesSize == 0) {
                                            Toast.makeText(Settings.this, R.string.error_no_watches, Toast.LENGTH_SHORT).show();
                                        } else if (mGoogleApiClient.isConnected()) {
                                            if (BuildConfig.DEBUG) {
                                                Log.d(TAG, "found " + mConnectedNodesSize + " connected nodes");
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
                                                                mConnectedNodesSize--;

                                                                if (BuildConfig.DEBUG) {
                                                                    Log.d(TAG, "sent message failed");
                                                                }
                                                            }
                                                        }
                                                    };

                                            DataHelper.requestDebug(mGoogleApiClient, nodes, sendMessageResultResultCallback);
                                        } else {
                                            Toast.makeText(Settings.this, R.string.error_not_connected, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            };

                    mDebugSizes.clear();
                    DataHelper.requestConnectedNodes(mGoogleApiClient, connectedNodesResultResultCallback);
                }
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
            setMuteSelection(PreferencesHelper.getMutePhoneMode(sharedPreferences));
        } else if (PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED.equals(key)) {
            mChkUnmute.setOnCheckedChangeListener(null);
            mChkUnmute.setChecked(PreferencesHelper.isUnmutePhoneEnabled(sharedPreferences));
            mChkUnmute.setOnCheckedChangeListener(this);
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

        if (DataHelper.WEAR_PATH_DEBUG.equals(messageEvent.getPath())) {
            long homePreferencesSize = ByteBuffer.wrap(messageEvent.getData()).getLong();

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "received homePreferencesSize: " + homePreferencesSize);
            }

            mDebugSizes.add(homePreferencesSize);
            mConnectedNodesSize--;

            if (mConnectedNodesSize == 0) {
                StringBuilder debugMessage = new StringBuilder("mailto:piusvelte@gmail.com")
                        .append("?subject=")
                        .append(getString(R.string.app_name))
                        .append(" Debug")
                        .append("&body=");

                debugMessage.append(TextUtils.join(",", mDebugSizes));

                if (BuildConfig.DEBUG) Log.d(TAG, "uri string: " + debugMessage.toString());

                Uri emailUri = Uri.parse(debugMessage.toString());
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(emailUri);
                startActivity(Intent.createChooser(emailIntent, "Send E-mail"));
            }
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
                        .putBoolean(PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED, mChkUnmute.isChecked())
                        .apply();
                DataHelper.syncBooleanSetting(mGoogleApiClient, PreferencesHelper.PREF_UNMUTE_PHONE_ENABLED, mChkUnmute.isChecked());
            }
        }
    }
}
