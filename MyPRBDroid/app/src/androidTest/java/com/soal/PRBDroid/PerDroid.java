package com.soal.PRBDroid;

import android.app.Instrumentation;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;


import com.soal.PRBDroid.permissionUtils.permissionManager;
import com.soal.PRBDroid.model.Graph;
import com.soal.PRBDroid.model.edge;
import com.soal.PRBDroid.model.element;
import com.soal.PRBDroid.model.monitorLog;
import com.soal.PRBDroid.model.normalUtil;
import com.soal.PRBDroid.sqlUtils.staticInfo;
import com.soal.PRBDroid.model.window;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

//import com.soul.pabdroiddynamic.PABDroidTool.CrashCat;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PerDroid {

    private Instrumentation mInstrumentation;
    private UiDevice mUidevice;
    private String mPackage;
    private String mainActivity;
    private window mainWin;
    private Set<Integer> AllWinIDset =new HashSet<>();   // ID is unique, because id= (name + XPath).Hashcode();
    private Set<Integer> queueIDSet=new HashSet<>();    // ID that appears in the queue, to avoid the same state to join the queue repeatedly
    private Queue<window>  unFinishWins=new LinkedList<window>();
    private Queue<window>  jumpedWins=new LinkedList<window>();  // 6.29 changes   adderTest
    private long testTimeout = 30;
    private long sleepTime=800;
    private long startTime;
    private long endTime;
    private monitorLog RuntimeMonitor;
    private monitorLog sqlThread;
    private permissionManager aPermissionManager=new permissionManager();


    @Before
    public void setUp() throws IOException {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mUidevice = UiDevice.getInstance(mInstrumentation);
        mUidevice.setCompressedLayoutHeirarchy(true);
        mPackage = mUidevice.getCurrentPackageName();
        mainActivity= normalUtil.V().getRunningActivity(mUidevice);
        startTime=System.currentTimeMillis();
        try {
            mUidevice.executeShellCommand("logcat -c");  //clear log
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtil.V().log("————————START—————————");
        Log.d("Runtime","package_Name:"+mPackage);
        Log.d("Runtime","main_Activity:"+mainActivity);
        LogUtil.V().log("————————START——————————");

    }
    @After
    public void tearDown() throws Exception {
        LogUtil.V().log("————————FINAL——————————");
        LogUtil.V().log("All_State_Size："+AllWinIDset.size());
        LogUtil.V().log("Tested_State_Size："+queueIDSet.size());
        LogUtil.V().log("unFinishWins_Size："+unFinishWins.size());
        LogUtil.V().log("————————FINAL——————————");
        RuntimeMonitor.interrupt();
        sqlThread.interrupt();
    }
    //Notes

    /*

     *Tools: PermissionManager: revokepermissions, grantpermissions

     * monitorLog： "Runtime-。。。。-. log" , "staticInfo.log"

     * LogUtil : "Runtime", "sql"

     * normalUtil: getRunningActivity , startApp ,stopApp

     *Crashcat: (the purpose is not clear) record the crash information "crashinfo. Log" of this app

     *

     *Steps

     *0. Start monitoring thread

     *1. Read static information - > process static information (* *). Static information may not be useful

     *2. Obtain the current status (supplement of collection static information)

     *3. BFS exploration until the end

     *4. (write a coverage and crash monitor on the static side)

     * */

    @Test
    public void testBFS() {
        RuntimeMonitor=new monitorLog(mPackage,"Runtime",false);
        RuntimeMonitor.start();
        sqlThread=new  monitorLog(mPackage,"sql",false);
        sqlThread.start();
        LogUtil.V().log("————————BFSTesting——————————");
        /*
         * 1.从主活动开始建立节点,入队Queue: unFinishWins;
         * 2.当queue不为空时，取出一个状态 testAct，跳转到该状态。
         * 3.testAct内所有 可点击控件 测试一遍。   <每次点击后，判断当前状态是否发生变化，如果变化了：记录新状态，记录边。返回上一个状态>
         * 4.直到超时 或者queue is empty（）；
         * */
        //主活动建立状态，然后加入 queue，
        // winID, name, onClickElements, clickedElements, finished ,Xpath
        mainWin=getCurWindow();
        unFinishWins.offer(mainWin);   //main活动加入队列
        queueIDSet.add(mainWin.getWinID());
        while(!unFinishWins.isEmpty()){
            long isEmpty=0;  // 当前控件未找到UI元素的个数  ， 意思就是在构建状态时记录了ele信息， 下次测试该win时，找不到该ele的个数
            endTime=System.currentTimeMillis();
            long time=((endTime-startTime)/1000)/30;   // min
            if(time>=testTimeout){
                LogUtil.V().log("More than "+testTimeout+" minutes,close the test");
                break;
            }
            window testAct=unFinishWins.poll();  //出队
            LogUtil.V().log("待测状态信息："+testAct.toString());  //打印一下当前栈顶状态
            if(AllWinIDset.add(testAct.getWinID())==true ){    // 每个状态的ID都记录一下
                Graph.V().getWindows().add(testAct);
            }
            /**
             * 从当前窗口 跳转到testAct
             * */
            if(srcJumpToTgtWin(testAct)==false){
                testAct.setFinished(true);
                jumpedWins.add(testAct);  //6.29
                LogUtil.V().log("!!!!!!跳转不到待测状态:"+testAct.getWinID()+",只能继续下一个状态，但是加入jumpedWins");
                continue;
            }
            //********开始执行点击操作*********
            //当前状态分析  1.普通状态（测一遍就行了，跟以前一样）,isActTest =0 or permissions=Null;
            execGernalTest(testAct,true);


        } // 队列为空，结束

        while(!jumpedWins.isEmpty()){   //弥补测试...   6.29     增加一个状态位置，表示是弥补测试，int 9 ,圆满的意思
            //1.尝试跳到jumpedWins ,  如果失败了，放弃该结点，继续下一个结点， 2.如果成功了， 像点击其他结点一样，操作该结点内的控件，（更新STG，如果产生一个完全新的状态，则加入jumped队列中）
            long isEmpty=0;  // 当前控件未找到UI元素的个数  ， 意思就是在构建状态时记录了ele信息， 下次测试该win时，找不到该ele的个数
            endTime=System.currentTimeMillis();
            long time=((endTime-startTime)/1000)/30;   // min
            if(time>=testTimeout){
                LogUtil.V().log("More than "+testTimeout+" minutes,close the test");
                break;
            }
            window testAct=jumpedWins.poll();  // 被跳过的状态出队
            LogUtil.V().log("待测状态信息："+testAct.toString());  //打印一下当前栈顶状态
            if(AllWinIDset.add(testAct.getWinID())==true ){    // 每个状态的ID都记录一下
                Graph.V().getWindows().add(testAct);
            }
            /**
             * 从当前窗口 跳转到testAct
             * */
            if(srcJumpToTgtWin(testAct)==false){
                testAct.setFinished(true);
                LogUtil.V().log("!!!!!!弥补测试还是跳转不到:"+testAct.getWinID()+",只能继续下一个跳过状态");
                continue;
            }
            //********开始执行点击操作*********
            //当前状态分析  1.普通状态（测一遍就行了，跟以前一样）,isActTest =0 or permissions=Null;
            //             2.目标活动状态，isActTest=1，权限依赖测试（其实是不同条件下，多次普通测试）   （待完善，全排列）
            //             3.目标控件测试 ，对该控件进行权限依赖测试，然后在权限无关下(也就是拥有所有权限)进行普通的状态测试。  （等待完善）


                //为 0 表示，是普通状态

            execGernalTest(testAct,false);
        }  //弥补测试...
        LogUtil.V().log("————————BFSTesting——————————");
    }

    public void preStage()  {
        /*

         *Enable runtime + SQL monitoring for debugging

         *Turn on crash monitoring. I don't know if it's useful. Put it here first

         *Read static information from SQL

         *Processing static information

         * */
        RuntimeMonitor=new monitorLog(mPackage,"Runtime",false);
        RuntimeMonitor.start();
        sqlThread=new  monitorLog(mPackage,"sql",false);
        sqlThread.start();
        staticInfo.V().getAllStaticInfo(mPackage);
        staticInfo.V().handleStaticInfo();
        staticInfo.V().printFinalStaticInfo();
    }


    @Test
    public void testBFS12() {
        preStage();
        LogUtil.V().log("————————Testing——————————");
        /*

         *1. Create a node from the main activity and queue: unfinishwins;

         *2. When the queue is not empty, take out a state testact and jump to this state.

         *3. Test all clickable controls in testact< After each click, judge whether the current status has changed. If so, record the new status and record the side. Return to previous status >

         *4. Until timeout or queue is empty();

         * */

//The main activity establishes the status, and then joins the queue,
        // winID, name, onClickElements, clickedElements, finished ,Xpath
        mainWin=getCurWindow();
        unFinishWins.offer(mainWin);
        queueIDSet.add(mainWin.getWinID());
        while(!unFinishWins.isEmpty()){
            long isEmpty=0;
            endTime=System.currentTimeMillis();
            long time=((endTime-startTime)/1000)/30;   // min
            if(time>=testTimeout){
                LogUtil.V().log("More than "+testTimeout+" minutes,close the test");
                break;
            }
            window testAct=unFinishWins.poll();
            LogUtil.V().log("testAct info ："+testAct.toString());
            if(AllWinIDset.add(testAct.getWinID())==true ){
                Graph.V().getWindows().add(testAct);
            }

            if(srcJumpToTgtWin(testAct)==false){
                testAct.setFinished(true);
                jumpedWins.add(testAct);  //6.29
                LogUtil.V().log("!!!!!!connot jump to testAct:"+testAct.getWinID()+",jump to next screens ");
                continue;
            }


            if(staticInfo.V().getTestActState(testAct)==1){


                String [] PERS=staticInfo.V().getTestActPermissions(testAct.getName());

                for(int j=0;j<PERS.length;j++){
                    LogUtil.V().log(PERS[j]);
                }
                if (PERS==null){  //

                    //staticInfo.V().staticPermissions.toArray(PERS);
                    if (staticInfo.V().staticPermissions.size()>0){
                        String[] output=new String[staticInfo.V().staticPermissions.size()];
                        staticInfo.V().staticPermissions.toArray(output);
                        PERS=output;
                        execActTargetTest(testAct,PERS,true);
                    }else{
                        LogUtil.V().log("...");
                    }
                }else if(PERS.length>=1){

                    execActTargetTest(testAct,PERS,true);
                }else{
                    aPermissionManager.resetAllPermissions(mUidevice,mPackage);
                    execGernalTest(testAct,true);
                }

            }else if(staticInfo.V().getTestActState(testAct)==2){
                String [] PERS=staticInfo.V().getTestActPermissions(testAct.getName());
                //log

                for(int j=0;j<PERS.length;j++){
                    LogUtil.V().log(PERS[j]);
                }
                if (PERS==null){  //

                    //staticInfo.V().staticPermissions.toArray(PERS);
                    if (staticInfo.V().staticPermissions.size()>0){
                        String[] output=new String[staticInfo.V().staticPermissions.size()];
                        staticInfo.V().staticPermissions.toArray(output);
                        PERS=output;
                        execActTargetTest(testAct,PERS,true);
                    }else{
                        LogUtil.V().log("...");
                    }
                }else if(PERS.length>=1){

                    execActTargetTest(testAct,PERS,true);
                }else{
                    aPermissionManager.resetAllPermissions(mUidevice,mPackage);
                    execGernalTest(testAct,true);
                }
            }else {

                aPermissionManager.resetAllPermissions(mUidevice,mPackage);
                execGernalTest(testAct,true);
            }

        }

        while(!jumpedWins.isEmpty()){   //   6.29

            long isEmpty=0;
            endTime=System.currentTimeMillis();
            long time=((endTime-startTime)/1000)/30;   // min
            if(time>=testTimeout){
                LogUtil.V().log("More than "+testTimeout+" minutes,close the test");
                break;
            }
            window testAct=jumpedWins.poll();
            LogUtil.V().log("testAct info："+testAct.toString());
            if(AllWinIDset.add(testAct.getWinID())==true ){
                Graph.V().getWindows().add(testAct);
            }

            if(srcJumpToTgtWin(testAct)==false){
                testAct.setFinished(true);
                LogUtil.V().log("!!!!!!connot jump to test Act:"+testAct.getWinID());
                continue;
            }


            if(staticInfo.V().getTestActState(testAct)==1){

                String [] PERS=staticInfo.V().getTestActPermissions(testAct.getName());

                for(int j=0;j<PERS.length;j++){
                    LogUtil.V().log(PERS[j]);
                }
                if (PERS==null){  //

                    //staticInfo.V().staticPermissions.toArray(PERS);
                    if (staticInfo.V().staticPermissions.size()>0){
                        String[] output=new String[staticInfo.V().staticPermissions.size()];
                        staticInfo.V().staticPermissions.toArray(output);
                        PERS=output;
                        execActTargetTest(testAct,PERS,false);
                    }else{
                        LogUtil.V().log("...");
                    }
                }else if(PERS.length>=1){

                    execActTargetTest(testAct,PERS,false);
                }

            }else if(staticInfo.V().getTestActState(testAct)==2){
                String [] PERS=staticInfo.V().getTestActPermissions(testAct.getName());
                //log

                for(int j=0;j<PERS.length;j++){
                    LogUtil.V().log(PERS[j]);
                }
                if (PERS==null){  //

                    //staticInfo.V().staticPermissions.toArray(PERS);
                    if (staticInfo.V().staticPermissions.size()>0){
                        String[] output=new String[staticInfo.V().staticPermissions.size()];
                        staticInfo.V().staticPermissions.toArray(output);
                        PERS=output;
                        execActTargetTest(testAct,PERS,false);
                    }else{
                        LogUtil.V().log("..."); //
                    }
                }else if(PERS.length>=1){

                    execActTargetTest(testAct,PERS,false);
                }else{
                    aPermissionManager.resetAllPermissions(mUidevice,mPackage);
                    execGernalTest(testAct,false);
                }
            }else {

                aPermissionManager.resetAllPermissions(mUidevice,mPackage);
                execGernalTest(testAct,false);
            }
        }




        LogUtil.V().log("————————Testing——————————");
    }
    public void execGernalTest(window testAct ,boolean isFirstTest){
        LogUtil.V().log("+++++++++++++++++++++++");
        int isEmpty=0;
        int countSize=0;
        while(testAct.getEleIDs().size()>testAct.getClickedEleIDs().size() && countSize<=40){
            element nextEle=testAct.getNextElement();
            if (nextEle==null){
                Log.d("Runtime","The next clickable element found is null, the search failed");
                continue;
            }

            if(newClickByElement(nextEle)==false){
                isEmpty++;
                continue;
//                    if(clickByElement(nextEle)==false){
//                        isEmpty++;
//                        continue;
//                    }
            }
            countSize++;
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            window curWin=getCurWindow();
            if(testAct.getWinID()!= curWin.getWinID()){

                if(mUidevice.getCurrentPackageName().equals(mPackage)){
                    if(AllWinIDset.add(curWin.getWinID())==true){
                        Graph.V().getWindows().add(curWin);
                    }
                    edge aNewEdge=new edge(testAct.getWinID(),curWin.getWinID(),nextEle);
                    Graph.V().getEdges().add(aNewEdge);
                    if(!queueIDSet.contains(curWin.getWinID())  && curWin.getOnClickElements().size()>0){
                        if(isFirstTest==true){
                            unFinishWins.offer(curWin);
                        }else{
                            jumpedWins.offer(curWin);
                        }

                        queueIDSet.add(curWin.getWinID());
                    }
                }

                LogUtil.V().log("goback");
                if(backToTgtWin(testAct)==true){
                    continue;
                }else{
                    LogUtil.V().log("！！！ the original state to be tested cannot be reached, give up the state and test the next state in the queue");
                    break;
                }
            }

        }

        testAct.setFinished(true);
        LogUtil.V().log("+++++++++++++++++++++++");
    }
    public void execActTargetTest(window testAct,String [] PERS,boolean isFirstTest){

        if(PERS.length==1){

            aPermissionManager.revokePermissions(mUidevice,mPackage,PERS[0]);
            LogUtil.V().log("first：");
            execGernalTest(testAct,isFirstTest);
            testAct.resetClickedEleIDs();
            aPermissionManager.resetAllPermissions(mUidevice,mPackage);
            LogUtil.V().log("second：");

            srcJumpToTgtWin(testAct);
            execGernalTest(testAct,isFirstTest);
        }else  {

            aPermissionManager.revokeAllpermissions(mUidevice,mPackage,PERS);
            execGernalTest(testAct,isFirstTest);
            testAct.resetClickedEleIDs();
            int timess=2;
            for(int i=0;i<PERS.length;i++){
                if(i==0){
                    LogUtil.V().log(""+timess+"times：");
                    timess++;
                    aPermissionManager.revokePermissions(mUidevice,mPackage,PERS[0]);

                    srcJumpToTgtWin(testAct);
                    execGernalTest(testAct,isFirstTest);
                    testAct.resetClickedEleIDs();
                }else{
                    LogUtil.V().log(""+timess+"times：");
                    timess++;
                    aPermissionManager.grantPermissions(mUidevice,mPackage,PERS[i-1]);
                    aPermissionManager.revokePermissions(mUidevice,mPackage,PERS[i]);

                    srcJumpToTgtWin(testAct);
                    execGernalTest(testAct,isFirstTest);
                    testAct.resetClickedEleIDs();
                }
            }
            LogUtil.V().log(""+timess+"times：");
            aPermissionManager.resetAllPermissions(mUidevice,mPackage);

            srcJumpToTgtWin(testAct);
            execGernalTest(testAct,isFirstTest);
        }
    }

    public window getCurWindow(){

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LogUtil.V().log(" ~~~~~~~~~~~~~~~");
        if(!mUidevice.getCurrentPackageName().equals(mPackage)){
            window curWin1=new window();
            curWin1.setWinID(11111);
            LogUtil.V().log(" ~~~~~~~~~~~~~~~");
            return curWin1;
        }

        // winID, name, onClickElements, EleIDs,clickedEleIDs, finished ,Xpath
        window curWin=new window();
        curWin.setName(normalUtil.V().getRunningActivity(mUidevice));
        curWin.setFinished(false);
        List<UiObject2> clickedUIObject=mUidevice.findObjects(By.clickable(true));
        Iterator<UiObject2> iterator=clickedUIObject.iterator();

        while(iterator.hasNext()){
            UiObject2 ob=(UiObject2) iterator.next();
            List<element> elements=new ArrayList<>();
            if (ob.getChildCount()!=0){
                getSbuElement(ob,elements);
                curWin.addDifOnCliEle(elements);
                //curWin.getOnClickElements().addAll(elements);
            }else {
                // text, resourceName,class,content_desc,clickable，，，
                element e=new element();
                e.setClickable(true);//ob.isClickable()
                e.setContent_desc(ob.getContentDescription());
                e.setEleClass(ob.getClassName());
                e.setResName(ob.getResourceName());
                e.setPoint(ob.getVisibleCenter());
                e.setText(ob.getText());
                e.setXpath();
                e.setEleID();
                if(curWin.hasThisOnCliEle(e)==true){
                    continue;
                }
                curWin.getOnClickElements().add(e);

            }
        }
        curWin.setEleIDs();
        curWin.setXpaths();
        curWin.setWinID();

        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append(" [curWinID："+curWin.getWinID()+"]").append(" [name："+curWin.getName()+"]").append(" [OnclickEle："+curWin.getOnClickElements().size()+"]");
        stringBuffer.append(" [eleID："+curWin.getEleIDs().size()+"]");
        LogUtil.V().log(stringBuffer.toString());
        LogUtil.V().log(" ~~~~~~~~~~~~~~~");
        return  curWin;
    }
    public void  getSbuElement(UiObject2 object2,List<element> elements){

        if(object2.getChildCount()==0){
            element e=new element();
            e.setClickable(true);
            e.setContent_desc(object2.getContentDescription());
            e.setEleClass(object2.getClassName());
            e.setResName(object2.getResourceName());
            e.setPoint(object2.getVisibleCenter());
            e.setText(object2.getText());
            e.setXpath();
            e.setEleID();
            elements.add(e);
            return;
        }
        List<UiObject2> subObject2=object2.getChildren();
        Iterator<UiObject2> iterator=subObject2.iterator();
        while(iterator.hasNext()){
            UiObject2 ob=(UiObject2) iterator.next();
            getSbuElement(ob,elements);
        }
    }


    public boolean backToTgtWin(window tgtWin)  {

        LogUtil.V().log(" 《《《backToTgtWin》》》");

        mUidevice.pressBack();
        LogUtil.V().log("press back");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        window curWin = getCurWindow();
        if (tgtWin.getWinID()==curWin.getWinID()) {
            LogUtil.V().log("back succeed");
            LogUtil.V().log(" 《《《backToTgtWin》》》");
            return true;
        } else {

            if(srcJumpToTgtWin(tgtWin)==true){
                LogUtil.V().log(" 《《《backToTgtWin》》》");
                return true;
            }else{
                LogUtil.V().log(" 《《《backToTgtWin》》》");
                return false;
            }
        }

    }
    public boolean srcJumpToTgtWin(window tgtWin)  {

        LogUtil.V().log(" 《《srcJumpToTgtWin》》");

        if(!mUidevice.getCurrentPackageName().equals(mPackage)){
            normalUtil.V().startApp(mPackage,mInstrumentation);
        }
        window curWin=getCurWindow();
        if(tgtWin.getWinID()==curWin.getWinID()){
            LogUtil.V().log("jump succeed");
            return true;
        }
        if(curWin.getWinID()==mainWin.getWinID()){


            if (mainJumpToTgtWin(tgtWin)==true){
                LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                return true;
            }else{
                LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                return false;
            }
        }else{
            String paths=Graph.V().getPath(curWin.getWinID(),tgtWin.getWinID());
            if(paths.equals("null")){
                LogUtil.V().log("！！！The path cannot be found. Try to find the path from main");
                if (mainJumpToTgtWin(tgtWin)==true){
                    LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                    return true;
                }else{
                    LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                    return false;
                }

            }else if(!paths.contains("#")){
                LogUtil.V().log("direct connect ");
                int eleID=Integer.parseInt(paths);
                LogUtil.V().log("eleID："+eleID);
                element ELE= curWin.findEleByID(eleID);
                if (ELE==null){
                    LogUtil.V().log("！！！fail");
                }else {
                    if(newClickByElement(ELE)==true){
                        window curWin1=getCurWindow();
                        LogUtil.V().log("curWin1 ID"+curWin1.getWinID());
                        LogUtil.V().log("tgtWinID"+tgtWin.getWinID());
                        if(tgtWin.getWinID()==curWin1.getWinID()){

                            LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                            return true;
                        }else {
                            LogUtil.V().log("！！！....");
                        }
                    }
                }
            }else if(paths.contains("#")){
                LogUtil.V().log("paths："+paths);
                String[] ID= paths.split("#");
                LogUtil.V().log("size："+ID.length);
                for (int i=0;i<ID.length;i++){
                    int eleID1=Integer.parseInt(ID[i]);
                    LogUtil.V().log("eleID1："+eleID1);
                    element ELE1= curWin.findEleByID(eleID1);
                    if (ELE1==null){
                        LogUtil.V().log("！！！fail ");
                        break;
                    }else {
                        if(newClickByElement(ELE1)==true){
                            window curWin1=getCurWindow();
                            if(tgtWin.getWinID()==curWin1.getWinID()){
                                LogUtil.V().log("succeed");
                                LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                                return true;
                            }
                            curWin=curWin1;
                        }else{
                            LogUtil.V().log("！！！fail");
                            break;
                        }
                    }
                }
            }

            if (mainJumpToTgtWin(tgtWin)==true){
                LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                return true;
            }else{
                LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                return false;
            }
        }

    }

    public boolean mainJumpToTgtWin(window tgtWin){
        LogUtil.V().log(" 《mainJumpToTgtWin》");
        window curWin=getCurWindow();

        if (curWin.getWinID()!=mainWin.getWinID()){
            normalUtil.V().startApp(mPackage,mInstrumentation);
            curWin=getCurWindow();
            if(curWin.getWinID()!=mainWin.getWinID() && AllWinIDset.add(curWin.getWinID())==true){
                Graph.V().getWindows().add(curWin);
                unFinishWins.offer(curWin);
            }

        }
        LogUtil.V().log("from Main to tgtWin");
        LogUtil.V().log("curWinID"+curWin.getWinID());
        LogUtil.V().log("tgtWinID"+tgtWin.getWinID());
        if(tgtWin.getWinID()==curWin.getWinID()){
            LogUtil.V().log("succeed");
            LogUtil.V().log(" 《mainJumpToTgtWin》");
            return true;
        }
        if (curWin.getWinID()!=mainWin.getWinID()){
            LogUtil.V().log("！！！");
        }
        String paths=Graph.V().getPath(curWin.getWinID(),tgtWin.getWinID());// “null”  ,  “ID” ，“ID#ID#”
        if(paths.equals("null")){
            LogUtil.V().log("！！！find Path fail");
            LogUtil.V().log(" 《mainJumpToTgtWin》");
            return false;
        }else if(!paths.contains("#")){
            int eleID=Integer.parseInt(paths);
            LogUtil.V().log("eleID："+eleID);
            element ELE= curWin.findEleByID(eleID);
            if (ELE==null){
                LogUtil.V().log("！！！fail");
                LogUtil.V().log(" 《mainJumpToTgtWin》");
                return false;
            }else {
                if(newClickByElement(ELE)==true){
                    window curWin1=getCurWindow();
                    if(tgtWin.getWinID()==curWin1.getWinID()){
                        LogUtil.V().log("succeed");
                        LogUtil.V().log(" 《mainJumpToTgtWin》");
                        return true;
                    }else {
                        LogUtil.V().log(" 《mainJumpToTgtWin》");
                        return false;
                    }
                }else{
                    LogUtil.V().log(" 《mainJumpToTgtWin》");
                    return false;
                }
            }
        }else if(paths.contains("#")){
            LogUtil.V().log("paths："+paths);
            String[] ID= paths.split("#");
            LogUtil.V().log("ID.length："+ID.length);
            for (int i=0;i<ID.length;i++){
                int eleID1=Integer.parseInt(ID[i]);
                LogUtil.V().log("eleID1："+eleID1);
                element ELE1= curWin.findEleByID(eleID1);
                if (ELE1==null){
                    LogUtil.V().log("！！！");
                    LogUtil.V().log(" 《mainJumpToTgtWin》");
                    return false;
                }else {
                    if(newClickByElement(ELE1)==true){
                        window curWin1=getCurWindow();
                        if(tgtWin.getWinID()==curWin1.getWinID()){
                            LogUtil.V().log("");
                            LogUtil.V().log(" 《mainJumpToTgtWin》");
                            return true;
                        }
                        curWin=curWin1;
                    }else{
                        LogUtil.V().log("！！！");
                        LogUtil.V().log(" 《mainJumpToTgtWin》");
                        return false;
                    }
                }
            }
            LogUtil.V().log(" 《mainJumpToTgtWin》");
            return false;
        }else{
            LogUtil.V().log("！！！:"+paths);
            LogUtil.V().log(" 《mainJumpToTgtWin》");
            return false;
        }
    }


    public boolean newClickByElement(element ELE){

        //UiObject2 ui2 = mDevice.findObject(By.clazz(TextView.class).depth(10, 11));
        UiObject2  nextUI=null;
        BySelector by=By.clazz(ELE.getEleClass());
        if (ELE.getResName()!=null){
            by.res(ELE.getResName());

        }
        if(ELE.getContent_desc()!=null){
            by.descContains(ELE.getContent_desc());
        }
        if (ELE.getText()!=null){
            by.text(ELE.getText());
        }
        nextUI=mUidevice.findObject(by);
        if (nextUI==null){
            LogUtil.V().log("connot find UI element："+ELE.toString());
            return false;
        }
        LogUtil.V().logUIelement(nextUI);
        nextUI.click();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean clickByElement(element ELE){

        UiObject2  nextUI=null;
        BySelector by=By.clazz(ELE.getEleClass());
        if (ELE.getResName()!=null){
            Log.d("Runtime",ELE.getResName());
            nextUI=mUidevice.findObject(By.res(ELE.getResName()));
        }else if(ELE.getContent_desc()!=null){
            Log.d("Runtime",ELE.getContent_desc());
            nextUI=mUidevice.findObject(By.descContains(ELE.getContent_desc()));
        }else if (ELE.getText()!=null){
            Log.d("Runtime",ELE.getText());
            nextUI=mUidevice.findObject(By.textContains(ELE.getText()));
        }else{
            nextUI=mUidevice.findObject(by);
        }
        if (nextUI==null){
            LogUtil.V().log("connot find UI element："+ELE.toString());
            return false;
        }
        LogUtil.V().logUIelement(nextUI);
        nextUI.click();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }


}