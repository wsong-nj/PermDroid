package com.soal.PRBDroid.model;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.test.uiautomator.UiDevice;

import com.soal.PRBDroid.LogUtil;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
* startAPP
* stopAPP
* getRunningActivity
* V
* */
public class normalUtil {
    private static normalUtil instance=null;

    public normalUtil() {
    }

    public static normalUtil V(){
        if(instance==null){
            synchronized (normalUtil.class){
                if (instance==null){
                    instance=new normalUtil();
                }
            }
        }
        return instance;
    }
    
    public void startApp(String pkgName, Instrumentation mInstrumentation){
        UiDevice mDevice=UiDevice.getInstance(mInstrumentation);
        Context context = mInstrumentation.getContext();
        //sets the intent to start your app
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //starts the app
        context.startActivity(intent);
        //mDevice.wait(Until.hasObject(By.pkg(pkgName).depth(0)), 3000);
        try {
            Thread.sleep(3000);
            if (!mDevice.getCurrentPackageName().equals(pkgName)) {
                Thread.sleep(3000);
                LogUtil.V().log("restart the app failed");
            }
        } catch (InterruptedException e) {
        e.printStackTrace();
    }
    }

    public void stopApp(String pkgName,Instrumentation mInstrumentation) throws IOException {
        UiDevice mDevice=UiDevice.getInstance(mInstrumentation);
        mDevice.executeShellCommand("am force-stop "+pkgName);
        LogUtil.V().log("stop APP");
    }
    public String getRunningActivity(UiDevice mUidevice) {
        String cmd = "dumpsys activity activities";
        String result = null;
        try {
            result = mUidevice.executeShellCommand(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("run adb shell dumpsys activity error", e.getMessage());
        }
        //mFocusedActivity: ActivityRecord{ff26d63 u0 fr.masciulli.drinks/.ui.activity.LicensesActivity t148}
        String pattern = "mFocusedActivity: ActivityRecord.*\n";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(result);
        if (m.find()) {
            String group = m.group(0);
//            LogUtil.V().log("the current activity "+group);//mFocusedActivity: ActivityRecord{a5abe96 u0 org.asdtm.goodweather/.SettingsActivity t224}
            String[] strs = group.split("/");
//            LogUtil.V().log("length "+strs.length);//
            String currentActivity = strs[strs.length - 1].split(" ")[0];
            return currentActivity.substring(1);//remove "."   SettingsActivity
        }
        //return null;
        return "null";
    }


}
