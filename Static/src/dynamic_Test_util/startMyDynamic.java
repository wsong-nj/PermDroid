package dynamic_Test_util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import soot.JastAddJ.ThisAccess;

public class startMyDynamic {
	public static String apkName="bbc";
	public static String pkgName="bbc.mobile.news.ww";  //ru.gismeteo.gismeteo
	public static String outputLogDir = "F:\\APK-ysh\\socialApps33\\"+apkName;
	

	public static void main(String[] args) {
		
		
		////   
		Push(pkgName);
		CoverageCat myCovMonitorCat=new CoverageCat();  //CoveragedAPI.txt
		myCovMonitorCat.clear();  
		myCovMonitorCat.start();
		CrashCat myCrashCat=new CrashCat(pkgName);		//ErrorLog.txt
		myCrashCat.start();
//		monitorLog RuntimeMonitor=new monitorLog("Runtime",false);  //Runtime_data_.txt   
//		RuntimeMonitor.start();										
//		try {
//			Thread.sleep(1000);  //10 s
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		//
		
		runPermDroid();   
		//runMyBFS();
		//runAPE(pkgName);				  
		//runMonkey("14");				  
		//runGESDA();					
		myCovMonitorCat.interrupt();
		myCrashCat.interrupt();
//		RuntimeMonitor.interrupt();
		System.out.println("finish.");
	}
	public static void runPermDroid() {
//		String cmd = "adb shell am instrument -w -r    -e debug false -e pkgName "
//				+ pkgName + " -e class 'com.soul.pabdroiddynamic.PerDroid#testBFS12' com.soul.pabdroiddynamic.test/androidx.test.runner.AndroidJUnitRunner";
		String cmd = "adb shell am instrument -w -r    -e debug false "
				 + " -e class 'com.soal.PRBDroid.PerDroid#testBFS12' com.soal.PRBDroid.test/androidx.test.runner.AndroidJUnitRunner";
		//adb shell am instrument -w -r    -e debug false -e class 'com.soul.pabdroiddynamic.PerDroid#testBFS12' com.soul.pabdroiddynamic.test/androidx.test.runner.AndroidJUnitRunner
		try {
			Process mLogcatReader = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(mLogcatReader.getInputStream()));
			String line = null;
			while((line = reader.readLine())!=null) {
				if(line.length() > 0)
					System.out.println("Result:"+line);
			}
			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void runMyBFS() {
//		String cmd = "adb shell am instrument -w -r    -e debug false -e pkgName "
//				+ pkgName + " -e class 'com.soul.pabdroiddynamic.PerDroid#testBFS12' com.soul.pabdroiddynamic.test/androidx.test.runner.AndroidJUnitRunner";
		String cmd = "adb shell am instrument -w -r    -e debug false "
				 + " -e class 'com.soal.PRBDroid.PerDroid#testBFS' com.soal.PRBDroid.test/androidx.test.runner.AndroidJUnitRunner";
		//adb shell am instrument -w -r    -e debug false -e class 'com.soul.pabdroiddynamic.PerDroid#testBFS12' com.soul.pabdroiddynamic.test/androidx.test.runner.AndroidJUnitRunner
		try {
			Process mLogcatReader = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(mLogcatReader.getInputStream()));
			String line = null;
			while((line = reader.readLine())!=null) {
				if(line.length() > 0)
					System.out.println("Result:"+line);
			}
			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void runAPE(String pkg) {
		String cmd = "python ape.py -p " + pkg + " --running-minutes 7 --ape sata";
		try {
			Process mLogcatReader = Runtime.getRuntime().exec(cmd);
			BufferedReader reader = new BufferedReader(new InputStreamReader(mLogcatReader.getInputStream()));
			String line = null;
			while((line = reader.readLine())!=null) {
//				if(line.length() > 0)
					System.out.println(line);
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void runMonkey(String time) {  
		try {
			String events = Integer.parseInt(time) * 60 * 5 + "";
			//adb shell monkey -v -v -v -s 8888 --throttle 300 --pct-touch 30 --pct-motion 25 --pct-appswitch 25 --pct-majornav 5 --pct-nav 0 --pct-trackball 0 -p com.wwdy.app 10000 >D:\monkey.txt
			//"--ignore-crashes",
			Process mLogcatReader = Runtime.getRuntime().exec(new String[] { "adb", "shell", "monkey","-v", "-p", pkgName,
					"--throttle", "500",  "--ignore-timeouts", events });
			BufferedReader reader = new BufferedReader(new InputStreamReader(mLogcatReader.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				System.out.println("monkey Log:" + line);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void runGESDA() {
		try {
			//adb shell am instrument -w -r    -e debug false [-e pkgName ""] -e class 'com.example.u2demo.TestCase.MyDynamicTest#dynamicTest' com.example.u2demo.test/androidx.test.runner.AndroidJUnitRunner
			Process mLogcatReader = Runtime.getRuntime().exec(new String[] {"adb","shell","am","instrument","-w","-r","-e","debug","false",
					//"-e","pkgName",pkgName,"-e","time",time,
					"-e","class","'com.fdu.uiautomatortest.DynamicTest#dfsStatic'","com.fdu.uiautomatortest.test/android.support.test.runner.AndroidJUnitRunner"});
			BufferedReader reader = new BufferedReader(new InputStreamReader(mLogcatReader.getInputStream()));
			String line = null;
			while((line = reader.readLine())!=null) {
				System.out.println("Result:"+line);
			}
			//flag = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void Push(String destination) {
		//adb push G:\APK\socialApps3\indigenous_64 /data
		try {
			String fileString=outputLogDir+"/dangerousPermissions.txt";
			String deString=" /storage/emulated/0/PABDroid/"+destination+"/";
			Process process=Runtime.getRuntime().exec("adb push "+fileString+deString);
			InputStreamReader inputReader = new InputStreamReader(process.getInputStream());
			BufferedReader reader=new BufferedReader(inputReader);
			String line = null;
            while((line = reader.readLine()) != null) {
            	System.out.println(line);
            }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
