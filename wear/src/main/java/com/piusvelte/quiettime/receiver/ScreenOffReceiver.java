package com.piusvelte.quiettime.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.piusvelte.quiettime.service.ZenModeWatcher;

/**
 * Created by bemmanuel on 8/23/14.
 */
public class ScreenOffReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(intent.setClass(context, ZenModeWatcher.class));
    }
}
