package com.xrouter;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Objects;

import dalvik.system.DexFile;

public class XRouter {
    private static Context context;

    public static void init(@NonNull Application application) {
        Objects.requireNonNull(application);
        context = application.getApplicationContext();
        try {
            DexFile dexFile = DexFile.loadDex(application.getPackageCodePath(),
                    application.getCacheDir().toString(), 0);
            Log.d("XRouter", dexFile.toString());
            Log.d("XRouter", dexFile.getName());
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements()) {
                String s = entries.nextElement();
                Log.d("XRouter", "entry: " + s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void navigate(String path) {
        String activityName = WareHouse.activityMap.get(path);
        if (activityName == null) {
            Toast.makeText(context, "Cannot find route for path:" + path, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(activityName.substring(0, activityName.lastIndexOf(".")), activityName));
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
