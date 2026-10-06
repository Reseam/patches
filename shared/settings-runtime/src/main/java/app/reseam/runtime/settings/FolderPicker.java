// SPDX-FileCopyrightText: 2026 Cossale <hello@auna.li>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.reseam.runtime.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Picks a folder for a folder setting. The settings have no activity of their own, and a framework
 * fragment is the only way to receive an activity result from any host activity without the host
 * forwarding it.
 */
@SuppressWarnings("deprecation")
public final class FolderPicker extends Fragment {
    private static final String TAG = "ReseamSettings";
    private static final String KEY = "key";
    private static final int REQUEST_CODE = 0x57C4;

    static void pick(Activity activity, String key) {
        FolderPicker picker = new FolderPicker();
        Bundle arguments = new Bundle();
        arguments.putString(KEY, key);
        picker.setArguments(arguments);
        activity.getFragmentManager().beginTransaction().add(picker, TAG).commitNow();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        picker.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getFragmentManager().beginTransaction().remove(this).commit();
        if (requestCode != REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        try {
            getActivity().getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            Log.w(TAG, "Could not persist permission for " + uri, e);
        }
        ReseamSettings.setString(getArguments().getString(KEY), uri.toString());
    }
}
