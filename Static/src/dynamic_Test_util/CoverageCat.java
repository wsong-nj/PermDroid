package dynamic_Test_util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CoverageCat implements Runnable {
	private Thread runner1 = new Thread(this, "runner1");

    public void start()
    {
        runner1.start();
    }

    public void interrupt()
    {
        runner1.interrupt();
    }
	
	
	private static String LogDir = startMyDynamic.outputLogDir; // output path
    private String Tag = "clickTag";
//    private static String apkName="apkName";   
    private static Set<String> apiSet = new HashSet<>();  //��¼�Ѿ������ġ�clickTag�� API�� �����ӡ�
    private static Set<String> allTargetMethods=new HashSet<String>(); //����Ŀ��API��stepOne   or  TestingPoint ������ĸ��
    public CoverageCat() { 
        
    }
    public static void clear() {
        try {
            Runtime.getRuntime().exec( " adb logcat -c");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void readCovAPIs() {
		//  先读已经存在的 CoveragedAPI.txt  
    	
    	File testPoint = new File(LogDir, "CoveragedAPI.txt");
    	if (!testPoint.exists()) {
    		System.out.println("Nothing CoveragedAPI.txt, so we create a new one.");
			return;
		}
		try(FileReader fr = new FileReader(testPoint);
        	BufferedReader br = new BufferedReader(fr)) {
            String line = br.readLine();
            while(line != null){
            	apiSet.add(line);
                line = br.readLine();
            }
            System.out.println("已经触发的调用数： "+apiSet.size());
            Iterator<String> iterator=apiSet.iterator();
            while (iterator.hasNext()) {
				String string = (String) iterator.next();
				System.out.println("API"+string);
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
	} 
    public void readALLAPIs() {  //这是分母用于计算覆盖率的，但是我不需要了，手动计算更合适
    	File testPoint = new File(LogDir, "TestingPonit.txt");
		try(FileReader fr = new FileReader(testPoint);
        	BufferedReader br = new BufferedReader(fr)) {
            String line = br.readLine();
            while(line != null){
                allTargetMethods.add(line);
                line = br.readLine();
            }
            System.out.println("Target size: "+allTargetMethods.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
	} 
    
  //adb logcat  | find "org.asdtm."//adb shell " logcat | grep clickTag "
    public void logToFile(String filePath) {     
        List<String> command = new ArrayList<>();
        command.add("adb");
        command.add("logcat");
        command.add("-s");
        command.add(Tag);

        FileOutputStream out = null;
        Process logcatProc = null;
        BufferedReader reader = null;
        InputStreamReader inputReader = null;
        try {
            File logFile = new File(filePath);
            if (!logFile.exists())
                logFile.createNewFile();
            out = new FileOutputStream(logFile, true);  // if true, then bytes will be written to the end of the file rather than the beginning
            logcatProc = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
            inputReader = new InputStreamReader(logcatProc.getInputStream());
            reader = new BufferedReader(inputReader, 1024);
            String line = null;
            while ((line = reader.readLine()) != null) {
            	if(runner1.isInterrupted())
                {
                    System.out.println("CoverageCat thread exit.");
                    break;
                }
                if (line == null || line.length() == 0) {
                    continue;
                }
                if (out != null) {    
                    if (line.contains("clickTag:")==true){
                        line=line.substring(line.indexOf("clickTag:")+9);
                        System.out.println(line);
                        if(apiSet.add(line)==true){
                            out.write((line + "\n").getBytes());
                        }
                    }
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            if (logcatProc != null) {
                logcatProc.destroy();
                logcatProc = null;
            }
            if (reader != null) {
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (inputReader != null) {
                try {
                    inputReader.close();
                    inputReader = null;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (out != null) {
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
    	readCovAPIs();  //
        String filePath = LogDir + "/CoveragedAPI.txt";// G:\APK\socialApps\goodweather_13\CoveragedAPI.txt"
        logToFile(filePath);
    }
	
}
