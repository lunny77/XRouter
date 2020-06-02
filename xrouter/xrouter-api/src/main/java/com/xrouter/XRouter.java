package com.xrouter;

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
    private static final String TAG = "XRouter";
    private static Context context;

    public static void init(@NonNull Application application) {
        Objects.requireNonNull(application);
        context = application.getApplicationContext();
        try {
            DexFile dexFile = DexFile.loadDex(application.getPackageCodePath(), application.getCacheDir().toString(), 0);
            Log.d(TAG, dexFile.getName());
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements()) {
                String classFile = entries.nextElement();
                if (classFile.startsWith(Constants.PACKAGE_OF_GENERATED_CODE)) {
                    Log.d(TAG, "XRouter generated class: " + classFile);
                    loadIntoRouteRoot(classFile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "WareHouse ROUTE_ROOT:" + Warehouse.ROUTE_ROOT.toString());
    }

    private static void loadIntoRouteRoot(String classFile) {
        Objects.requireNonNull(classFile);
        if (classFile.startsWith(Constants.ROUTE_ROOT_PREFIX)) {
            try {
                Class<? extends IRouteRoot> rootClass = (Class<? extends IRouteRoot>) Class.forName(classFile);
                IRouteRoot routeRoot = rootClass.newInstance();
                routeRoot.loadInto(Warehouse.ROUTE_ROOT);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    public static void navigate(String path) {
        String group = Utils.getGroupFromPath(path);
        Class<? extends IRouteGroup> groupClass = Warehouse.ROUTE_ROOT.get(group);
        if (groupClass == null) {
            Toast.makeText(context, "Cannot find route for path:" + path, Toast.LENGTH_SHORT).show();
            return;
        }

        RouteMeta routeMeta = Warehouse.ROUTE_GROUP.get(path);
        if (routeMeta == null) {
            try {
                IRouteGroup routeGroup = groupClass.newInstance();
                routeGroup.loadInto(Warehouse.ROUTE_GROUP);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            routeMeta = Warehouse.ROUTE_GROUP.get(path);
        }

        startActivity(routeMeta.target);
    }

    private static void startActivity(String className) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(context.getPackageName(), className));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
