package com.soal.PRBDroid.permissionUtils;


import androidx.test.uiautomator.UiDevice;

import com.soal.PRBDroid.LogUtil;
import com.soal.PRBDroid.sqlUtils.staticInfo;

import java.io.IOException;
import java.util.Iterator;


public class permissionManager {


    public void revokePermissions(UiDevice mUiDevice,String pkgName,String pemissions) {
        //adb shell pm revoke org.asdtm.goodweather  android.permission.ACCESS_FINE_LOCATION    android.permission.ACCESS_COARSE_LOCATION
        String cmd="pm revoke "+pkgName+" "+pemissions;
        String result=null;
        try {
            result=mUiDevice.executeShellCommand(cmd);
            LogUtil.V().log("revoke:"+pemissions);
            LogUtil.V().logSql("revoke:"+pemissions);
        } catch (IOException e) {
            LogUtil.V().log("revoke permissions error"+pemissions);
            LogUtil.V().logSql("revoke permissions error"+pemissions);
            e.printStackTrace();
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void grantPermissions(UiDevice mUiDevice,String pkgName,String pemissions) {
        //adb shell pm grant org.asdtm.goodweather  android.permission.ACCESS_FINE_LOCATION    android.permission.ACCESS_COARSE_LOCATION
        String cmd="pm grant "+pkgName+" "+pemissions;
        String result=null;
        try {
            result=mUiDevice.executeShellCommand(cmd);
            LogUtil.V().log("grant:"+pemissions);
            LogUtil.V().logSql("grant:"+pemissions);
        } catch (IOException e) {
            LogUtil.V().log("grant permissions error:"+pemissions);
            LogUtil.V().logSql("grant permissions error:"+pemissions);
            e.printStackTrace();
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resetAllPermissions(UiDevice mUiDevice,String pkgName){

        Iterator<String> iterator= staticInfo.V().PPermissions.iterator();
        while (iterator.hasNext()){
            String per=iterator.next();
            grantPermissions(mUiDevice,pkgName,per);
        }
    }
    public void revokeAllpermissions(UiDevice mUiDevice,String pkgName,String [] PERS){
        for (int i=0;i<PERS.length;i++){
            revokePermissions(mUiDevice,pkgName,PERS[i]);
        }
    }

}
