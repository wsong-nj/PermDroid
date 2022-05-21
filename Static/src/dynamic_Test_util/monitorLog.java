package dynamic_Test_util;

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

    private Thread runner3 = new Thread(this, "runner3");

    public void start()
    {
        runner3.start();
    }

    public void interrupt()
    {
        runner3.interrupt();
    }

    private DateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
    private  String LogDir= startMyDynamic.outputLogDir; // 输出的路径路径,
    private  String Tag="Runtime";  //监控的 Tag
    private  boolean append=true;// if true, then bytes will be writtento the end of the file rather than the beginning

    public monitorLog( String tag,boolean append) {
//        LogDir += apkName;
        Tag = tag;
        this.append=append;
    }

    private String getFileName() {
        String time = formatter.format(new Date());
        return "Runtime_" + time + ".txt";
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
        command.add("adb");
        command.add("logcat");
        command.add("-s");
        command.add(Tag);  // sql  or Runtime(YSH)
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
                if(runner3.isInterrupted())
                {
                    System.out.println("Runtime log exits");
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
            e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
            if(inputReader != null) {
                try {
                    inputReader.close();
                    inputReader = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(out != null) {
                try {
                    out.close();
                    out = null;
                } catch (IOException e) {
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
        File file=new File(LogDir);     
        if (!file.exists()) file.mkdirs();
        String filePath=LogDir+"/"+fileName;
        logToFile(filePath);
    }
}