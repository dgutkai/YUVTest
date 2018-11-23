package com.real0168.base;

import android.app.Application;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.preference.PreferenceManager;

import com.real0168.yuvtest.config.Config;
import com.real0168.yuvtest.utils.Util;

import java.util.List;

public class BaseApplication extends Application {
    private static BaseApplication mApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
        if (Util.getSupportResolution(this).size() == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            Camera camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            List<Camera.Size> supportedPreviewSizes = camera.getParameters().getSupportedPreviewSizes();
            for (Camera.Size str : supportedPreviewSizes) {
                stringBuilder.append(str.width + "x" + str.height).append(";");
            }
            Util.saveSupportResolution(this, stringBuilder.toString());
            camera.release();
        }
    }

    public static BaseApplication getEasyApplication() {
        return mApplication;
    }


    public void saveStringIntoPref(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public String getPort() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String port = sharedPreferences.getString(Config.SERVER_PORT, Config.DEFAULT_SERVER_PORT);
        return port;
    }

    public String getId() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String id = sharedPreferences.getString(Config.STREAM_ID, Config.DEFAULT_STREAM_ID);

        saveStringIntoPref(Config.STREAM_ID, id);
        return id;
    }
}
