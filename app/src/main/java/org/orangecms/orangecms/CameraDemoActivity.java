package org.orangecms.orangecms;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import ly.img.android.pesdk.backend.model.constant.Directory;
import ly.img.android.pesdk.backend.model.state.EditorSaveSettings;
import ly.img.android.pesdk.backend.model.state.CameraSettings;
import ly.img.android.pesdk.backend.model.state.manager.*;
import ly.img.android.pesdk.assets.font.basic.FontPackBasic;
import ly.img.android.pesdk.ui.activity.*;
import ly.img.android.pesdk.ui.model.state.UiConfigText;
import ly.img.android.pesdk.ui.utils.*;

public class CameraDemoActivity extends Activity implements PermissionRequest.Response {

    // Important permission request for Android 6.0 and above, don't forget to add this!
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void permissionGranted() {}

    @Override
    public void permissionDenied() {
        /* TODO: The Permission was rejected by the user. The Editor was not opened,
         * Show a hint to the user and try again. */
    }

    public static int PESDK_RESULT = 1;

    private SettingsList createPesdkSettingsList() {
        SettingsList settingsList = new SettingsList();

        settingsList.getSettingsModel(UiConfigText.class).setFontList(
          FontPackBasic.getFontPack()
        );

        // Set custom camera image export settings
        settingsList.getSettingsModel(CameraSettings.class)
          .setExportDir(Directory.DCIM, "Orange CMS")
          .setExportPrefix("camera_");

        // Set custom editor image export settings
        settingsList.getSettingsModel(EditorSaveSettings.class)
          .setExportDir(Directory.DCIM, "Orange CMS")
          .setExportPrefix("result_")
          .setSavePolicy(EditorSaveSettings.SavePolicy.RETURN_ALWAYS_ONLY_OUTPUT);

        return settingsList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        openCamera();
    }

    private void openCamera() {
        SettingsList settingsList = createPesdkSettingsList();

        new CameraPreviewBuilder(this)
          .setSettingsList(settingsList)
          .startActivityForResult(this, PESDK_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PESDK_RESULT) {
            // Editor has saved an Image.
            Uri resultURI = data.getParcelableExtra(ImgLyIntent.RESULT_IMAGE_URI);
            Uri sourceURI = data.getParcelableExtra(ImgLyIntent.SOURCE_IMAGE_URI);

            // Scan result uri to show it up in the Gallery
            if (resultURI != null) {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(resultURI));
            }

            // Scan source uri to show it up in the Gallery
            if (sourceURI != null) {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(sourceURI));
            }

            Log.i("PESDK", "Source image is located here " + sourceURI);
            Log.i("PESDK", "Result image is located here " + resultURI);

            // TODO: Do something with the result image
        } else if (resultCode == RESULT_CANCELED && requestCode == PESDK_RESULT) {

            // Editor was canceled
            Uri sourceURI = data.getParcelableExtra(ImgLyIntent.SOURCE_IMAGE_URI);
            // TODO: Do something...
        }
    }
}
