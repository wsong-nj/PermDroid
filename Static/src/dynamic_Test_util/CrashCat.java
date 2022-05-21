package dynamic_Test_util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CrashCat implements Runnable  {
	
	private static String LogDir = startMyDynamic.outputLogDir; // output path
//    private static String apkName="apkName";    //其实应该是 apkName
    public static String pkgName="org.asdtm.goodweather";
    
	private Thread runner2 = new Thread(this, "runner2");

    public void start()
    {
        runner2.start();
    }

    public void interrupt()
    {
        runner2.interrupt();
    }
	
    
	public CrashCat(String pkgName ) {
//		LogDir+=apkName;
//		this.apkName=apkName;
		this.pkgName=pkgName;
	}

	public static int getPidfromPs(String appPackage) {
		String s = null;
		try {
			Process logcat = Runtime.getRuntime().exec("adb shell ps");

			BufferedReader output = new BufferedReader(new InputStreamReader(logcat.getInputStream()));

			while ((s = output.readLine()) != null) {
				int ret = matchPid(".+?\\s+([0-9]+)\\s*.+" + appPackage, s);

				if (ret != -1) {
					System.out.println("PID:"+ret);
					return ret;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public  void executeLogcatCommand( String pid) {
		String s = null;
		boolean found = false;
		String filePath = LogDir + "/ErrorLog.txt";// G:\APK\socialApps\goodweather_13\ErrorLog.txt"
		FileOutputStream out = null;
		try {
			File logFile = new File(filePath);
            if (!logFile.exists())
                logFile.createNewFile();
            out = new FileOutputStream(logFile, true);  // if true, then bytes will be written to the end of the file rather than the beginning
			Process logcat = Runtime.getRuntime().exec("adb logcat -v long *:E");

			BufferedReader output = new BufferedReader(new InputStreamReader(logcat.getInputStream()));

			System.out.println("starting output.APP's  log *:W.");

			while ((s = output.readLine()) != null) {
				if(runner2.isInterrupted())
                {
                    System.out.println("Crash cat thread exit.");
                    break;
                }
				
				if ((!s.matches("^$")) && (!s.matches("^\\s*\n$")) && (!s.matches("^\\s$"))
						&& (!s.matches("^\\s*\r\n$"))) {
					int ret = matchString("\\[\\s+.+\\s+.+\\s+([0-9]+):.+\\s+.+\\]", s, pid);
					if (ret == 1) {
						found = true;
						System.out.println(s);
						if (out != null) {  
							out.write((s + "\n").getBytes());
						}
					} else if ((found) && (ret == 3)) {
						System.out.println(s);
						if (out != null) {  
							out.write((s + "\n").getBytes());
						}
					} else if (ret == 2) {
						found = false;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int matchString(String regex, String matchLine, String groupMatchLine) {
		Pattern pattern = Pattern.compile(regex);

		Matcher matcher = pattern.matcher(matchLine);

		if (matcher.find()) {
			if ((groupMatchLine != null) && (groupMatchLine.equals(matcher.group(1))))
				return 1;

			return 2;
		}

		return 3;
	}

	public static int matchPid(String regex, String matchLine) {
		Pattern pattern = Pattern.compile(regex);

		Matcher matcher = pattern.matcher(matchLine);

		if (matcher.find()) {
			System.out.println("============found==============");
			System.out.print("Start index: " + matcher.start());
			System.out.print(" End index: " + matcher.end() + " ");
			System.out.println(matcher.group());
			System.out.println(matcher.group(1));
			return Integer.valueOf(matcher.group(1)).intValue();
		}

		return -1;
	}

	@Override
	public void run() {
		int pid = getPidfromPs(pkgName);
		if (pid != -1)
			executeLogcatCommand(String.valueOf(pid));
		else
			System.out.println("this app not found");
        
    }
	
}
