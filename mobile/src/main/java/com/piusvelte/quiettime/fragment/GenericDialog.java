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
package com.piusvelte.quiettime.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;

/**
 * Created by bemmanuel on 8/24/14.
 */
public class GenericDialog extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";

    public static GenericDialog newInstance(@StringRes int title, @StringRes int message) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_TITLE, title);
        arguments.putInt(ARG_MESSAGE, message);

        GenericDialog genericDialog = new GenericDialog();
        genericDialog.setArguments(arguments);
        return genericDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getInt(ARG_TITLE))
                .setMessage(getArguments().getInt(ARG_MESSAGE))
                .create();
    }
}
