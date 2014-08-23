package com.piusvelte.quiettime.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.piusvelte.quiettime.R;

/**
 * Created by bemmanuel on 8/24/14.
 */
public class AboutDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.action_about)
                .setMessage(R.string.about_message)
                .create();
    }
}
