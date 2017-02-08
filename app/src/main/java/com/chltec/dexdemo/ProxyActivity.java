package com.chltec.dexdemo;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class ProxyActivity extends AppCompatActivity {
    private static final String TAG = "ProxyActivity";

    public static final String EXTRA_DEX_PATH = "extra.dex.path";
    public static final String EXTRA_CLASS = "extra.class";
    public static final String FROM = "extra.from";
    public static final int FROM_EXTERNAL = 0;
    private String mClass;
    private String mDexPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proxy);
        mDexPath = getIntent().getStringExtra(EXTRA_DEX_PATH);
        mClass = getIntent().getStringExtra(EXTRA_CLASS);
        if (mClass == null) {
            launchTargetActivity();
        } else {
            launchTargetActivity(mClass);
        }
    }


    protected void launchTargetActivity() {
        PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(
                mDexPath, PackageManager.GET_ACTIVITIES);
        if ((packageInfo.activities != null)
                && (packageInfo.activities.length > 0)) {
            String activityName = packageInfo.activities[0].name;
            mClass = activityName;
            launchTargetActivity(mClass);
        }
    }


    protected void launchTargetActivity(final String className) {
        Log.d(TAG, "start launchTargetActivity, className=" + className);
        File dexOutputDir = this.getDir("dex", 0);
        final String dexOutputPath = dexOutputDir.getAbsolutePath();
        ClassLoader localClassLoader = ClassLoader.getSystemClassLoader();
        DexClassLoader dexClassLoader = new DexClassLoader(mDexPath,
                dexOutputPath, null, localClassLoader);
        try {
            Class<?> localClass = dexClassLoader.loadClass(className);
            Constructor<?> localConstructor = localClass
                    .getConstructor(new Class[] {});
            Object instance = localConstructor.newInstance(new Object[] {});
            Log.d(TAG, "instance = " + instance);

            Method setProxy = localClass.getMethod("setProxy",
                    new Class[] { Activity.class });
            setProxy.setAccessible(true);
            setProxy.invoke(instance, new Object[] { this });

            Method onCreate = localClass.getDeclaredMethod("onCreate",
                    new Class[] { Bundle.class });
            onCreate.setAccessible(true);
            Bundle bundle = new Bundle();
            bundle.putInt(FROM, FROM_EXTERNAL);
            onCreate.invoke(instance, new Object[] { bundle });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}