package com.ironxiao.ipcheck;

import android.app.Application;
import android.os.StrictMode;

public class IpCheckApp extends Application {
    public IpCheckApp() {
        if (BuildConfig.DEBUG)
            StrictMode.enableDefaults();
    }


}
