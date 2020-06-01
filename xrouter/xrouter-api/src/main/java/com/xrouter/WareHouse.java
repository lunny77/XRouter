package com.xrouter;

import android.app.Activity;

import java.util.HashMap;
import java.util.Map;

public class WareHouse {

    public static Map<String, String> activityMap = new HashMap<>();

    public static void putActivity(String path, String activityName) {
        activityMap.put(path,activityName);
    }
}
