package com.soal.PRBDroid;
import android.util.Log;

import androidx.test.uiautomator.UiObject2;

public class LogUtil {
    private static final String TAG = "Runtime";
    private static final String SQL="sql";
    private static LogUtil instance=null;
    public static LogUtil V(){
        if (instance==null){
            synchronized (LogUtil.class){
                if (instance==null){
                    instance=new LogUtil();
                }
            }
        }
        return  instance;
    }
    public LogUtil() {
    }


    public void logSql(String s){
        Log.d(SQL,s);
    }
    public void log(String s){
        Log.d(TAG,s);
    }
    public void logUIelement(UiObject2 object2){
        String s="UIelement{" +
                "class" + object2.getClassName() +
                ", resName='" + object2.getResourceName() + '\'' +
                ", PKGname=" + object2.getApplicationPackage() +
                ", ContentDesc=" + object2.getContentDescription() +
                ", Text=" + object2.getText() +
                ", chileCount=" + object2.getChildCount() +
                ", isChickable='" + object2.isClickable() + '\'' +
                '}';
        Log.d(TAG,s);
    }
}
