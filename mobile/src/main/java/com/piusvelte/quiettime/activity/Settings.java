package com.piusvelte.quiettime.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class Settings extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener,
        MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = Settings.class.getSimpleName();

    private SharedPreferences mSharedPreferences;
    private CheckBox mChkWearToMobile;

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

        mSharedPreferences = DataHelper.getSharedPreferences(this);

        mChkWearToMobile = (CheckBox) findViewById(R.id.chk_wear_to_mobile);
        mChkWearToMobile.setChecked(DataHelper.PREFERENCE.PREF_WEAR_TO_MOBILE_ENABLED.isEnabled(mSharedPreferences));
        mChkWearToMobile.setOnClickListener(this);
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
}
