# Getting Started #

## Environment info ##

1. eclipse (for staticAnalysis)

2. AndroidStudio (for dynamicAnalysis)

3. Mysql (5.6<version <8.0)

4. Android emulator (version >=6.0)

5. Android SDK

## Static Analysis Module ##

1. Import staticAnalysis into eclipse.The dependence jars files are in the "libs_soot" and "libs_flowdroid" folders.

2. Create a database named 'STG' in Mysql. Import the given three MySQL files (in the folder MySQL files) into the database 'STG'. Open src/mySql/DBUtil.java in static analysis module, and set the values of these two variables: 'USERNAME','PASSWORD'.

3. Open "src/zzg.staticanalysis/Main.java".

3.1 Set the values of these three variables: 'apk path','apk name',and 'androidPlaformLocation'.

3.2 Run "Main.java". This step takes about one minute.
![avatar](https://gitee.com/jsg1999/pictures/raw/master/2.png)
The output path: 'apkDir'+'apkName'.

## Dynamic Analysis Module ##

1.Import dynamicAnalysis into AndroidStudio.

2.Connect to Android emulator to install APK with “_resigned” suffix obtained after step 3.2. Then open the tested app.

3.Make sure your MySQL account has remote access rights.
open src/androidTest/java/com/soal/PRBDroid/sqlUtils/DBUtil.java, and replace the IP address in the URL with your local IP address.

4.Install ape.Execute "adb push ape.jar /data/local/tmp/" in your terminal. This step takes about one minute.

5.Open "PerDroid.java",and then run "testBFS12()" function. 
![avatar](https://gitee.com/jsg1999/pictures/raw/master/6.png)
It will take about ten minutes. After this step, an automated test APK (i.e. dynamic explorer) will be installed on the emulator.

6.When the third step is executed, the dynamic explorer can be run again in the following two ways.

(1) Adb command line:

"adb shell am instrument -w -r -e debug false -e class 'com.soal.PRBDroid.PerDroid#testBFS12' com.soal.PRBDroid.test/androidx.test.runner.AndroidJUnitRunner".

(2)open "src/dynamic_Test_util/startMyDynamic.java" in static analysis module, to set the values of these three variables: 'apkname','pkgname',and 'outputLogDir'. Then you can run it. When this step is executed, you can get CoveragedAPI.txt and ErrorLog.txt in the output path. It will take about ten minutes.
![avatar](https://gitee.com/jsg1999/pictures/raw/master/1.png)


# Detailed Description #
This paper proposes an automatic testing method of permission association behavior, PermDroid, which is compared with three methods (monkey, ape, gesda) and three strategies (all and none, pairwise, empirical). These experimental results are analyzed based on the permission-related API invocation statement coverage rate (SCR), defect detection ability, and testing efficiency, which can demonstrate effectiveness and efficiency.

The apps used in our evaluation are available at: https://figshare.com/s/57a699e248cc58ee6460

## For permission-related API invocation statement coverage rate (SCR) ##
This paper identifies all permission-related APIs calling statements and sets them as targets. Assume that the aggregate S represents the four-tuple information of all permission-related APIs call statements obtained during static analysis. By running the instrumented application, this paper collects the logs of the triggered API call statements through the monitor and uses it as a aggregate S’..

After running step 3.2 in Static Analysis Module, there will be APILocation.txt in the output path. This file will record the four-tuple information of all permission-related APIs call statements. We can get the aggregate S from this file. After running step 6.2 in Dynamic Analysis Module, there will be CoveragedAPI.txt in the output path, which records the triggered API call statement, from which the aggregate S’ can be obtained. Then we can calculate SCR and compare it with other tools. Higher SCR means higher effectiveness.
![avatar](https://gitee.com/jsg1999/pictures/raw/master/8.png)
![avatar](https://gitee.com/jsg1999/pictures/raw/master/4.png)
## For defect detection ability ##
This paper analyzes and reproduces the discovered application defects through the output of PermDroid, including the logs output by the Error monitor and the Runtime monitor.
After running step 6.2 in Dynamic Analysis Module, there will be ErrorLog.txt in the output path, from which you can get the number of defects of this tested app and compare it with other tools. The higher the number of defects found, the more effective.
![avatar](https://gitee.com/jsg1999/pictures/raw/master/3.png)
![avatar](https://gitee.com/jsg1999/pictures/raw/master/5.png)
Click the button in the red box in the above picture in turn, and the APP will crash.
## For testing efficiency ##
This paper records the final test time of the three comparison tools and PermDroid for comparison. The shorter the test time, the more efficient.