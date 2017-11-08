package com.wzf.imagepicker;

import android.support.multidex.MultiDexApplication;


/**
 * Created by zhenfei.wang on 2016/8/8.
 */
public class MyApplication extends MultiDexApplication {
    private final String TAG = getClass().getSimpleName();
    private static MyApplication application;
    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }



    public  static  MyApplication getAppInstance(){
        return application;
    }
}
