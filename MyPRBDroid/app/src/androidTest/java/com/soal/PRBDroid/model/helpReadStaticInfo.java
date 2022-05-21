package com.soal.PRBDroid.model;

import android.os.Environment;

import com.soal.PRBDroid.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class helpReadStaticInfo {
    private String LogDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PABDroid/"; // output path
    private String Tag = "clickTag";

    public helpReadStaticInfo(String PkgName) {
        LogDir += PkgName;
    }


    public String getStaticDanPermissions() {

        File file = new File(LogDir,"dangerousPermissions.txt");     //Environment.getExternalStorageDirectory().getAbsolutePath()+"/PABDroid/"+PKGname+"/dangerousPermissions.txt";
        StringBuffer sb=new StringBuffer();
        if (!file.exists()) {
            LogUtil.V().logSql(" StaticDanPermissions.txt isn't exists.");
            return null;
        }else{
            try {
                FileReader fileReader=new FileReader(file);
                BufferedReader reader=new BufferedReader(fileReader);
                String line = reader.readLine();
                while(line != null){
                    sb.append(line).append(" ");
                    line = reader.readLine();
                }
                return sb.toString();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        return null;
    }
}
