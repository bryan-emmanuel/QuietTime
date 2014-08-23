package com.piusvelte.quiettime.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.piusvelte.quiettime.BuildConfig;
import com.piusvelte.quiettime.service.ZenModeWatcher;

/**
 * Created by bemmanuel on 8/22/14.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) Log.d(TAG, "received: " + (intent != null ? intent.getAction() : null));
        context.startService(intent.setClass(context, ZenModeWatcher.class));
    }
}
