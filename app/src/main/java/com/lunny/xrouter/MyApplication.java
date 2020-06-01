package com.lunny.xrouter;

import android.app.Application;

import com.xrouter.XRouter;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        XRouter.init(this);
    }
}
