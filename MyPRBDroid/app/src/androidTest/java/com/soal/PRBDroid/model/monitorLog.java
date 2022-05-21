package com.soal.PRBDroid.model;


import android.os.Environment;

import com.soal.PRBDroid.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class monitorLog implements Runnable{

    private Thread runner = new Thread(this, "runner");

    public void start()
    {
        runner.start();
    }

    public void interrupt()
    {
        runner.interrupt();
    }

    private DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
    private  String LogDir= Environment.getExternalStorageDirectory().getAbsolutePath()+"/PABDroid/"; // Output path,,
    private  String Tag="Runtime";  // Tag
    private  boolean append=true;// if true, then bytes will be writtento the end of the file rather than the beginning

    public monitorLog(String PkgName, String tag,boolean append) {
        LogDir += PkgName;
        Tag = tag;
        this.append=append;
    }

    private String getFileName() {
        String time = formatter.format(new Date());
        return "Runtime_" + time + ".log";
    }
    public static void clear(){
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-c"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public  void logToFile( String filePath) {
        List<String> command = new ArrayList<>();
        command.add("logcat");
        command.add("-s");
        command.add(Tag);  // clickTag  or Runtime
        command.add("-v");
        command.add("time");
        FileOutputStream out = null;
        Process logcatProc = null;
        BufferedReader reader = null;
        InputStreamReader inputReader = null;
        try {
            File logFile = new File(filePath);
        	if(!logFile.exists())
        		logFile.createNewFile();
            out = new FileOutputStream(logFile, append);  // if true, then bytes will be written to the end of the file rather than the beginning
            logcatProc = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
            inputReader = new InputStreamReader(logcatProc.getInputStream());
            reader = new BufferedReader(inputReader, 1024);
            String line = null;
            while((line = reader.readLine()) != null) {
                if(runner.isInterrupted())
                {
                    LogUtil.V().logSql("shutdown "+Tag+" thread");
                    LogUtil.V().log("shutdown "+Tag+" thread");
                    break;
                }
                if(line == null || line.length() == 0) {
                    continue;
                }
                if(out != null) {
                    out.write((line + "\n").getBytes());
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }finally {
            if(logcatProc != null) {
                logcatProc.destroy();
                logcatProc = null;
            }
            if(reader != null) {
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(inputReader != null) {
                try {
                    inputReader.close();
                    inputReader = null;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if(out != null) {
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        String fileName;
        if(Tag.equals("sql")){
            fileName="staticInfo.log";
        }else{
            fileName=getFileName();
        }
        File file=new File(LogDir);     //Environment.getExternalStorageDirectory().getAbsolutePath()+"/PABDroid/"+PKGname
        if (!file.exists()) file.mkdirs();
        String filePath=LogDir+"/"+fileName;////Environment.getExternalStorageDirectory().getAbsolutePath()+"/PABDroid/"+PKGname+"/"+"Log-coverage-" + time + ".log"
        logToFile(filePath);
    }
}
