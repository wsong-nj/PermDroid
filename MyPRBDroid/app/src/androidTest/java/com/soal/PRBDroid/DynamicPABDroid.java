package com.soal.PRBDroid;

import android.app.Instrumentation;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;

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

@RunWith(AndroidJUnit4.class)
public class DynamicPABDroid {

    private Instrumentation mInstrumentation;
    private UiDevice mUidevice;
    private String mPackage;
    private String mainActivity;
    private window mainWin;
    private Set<Integer> AllWinIDset =new HashSet<>();
    private Set<Integer> queueIDSet=new HashSet<>();
    private Queue<window>  unFinishWins=new LinkedList<window>();
    private long testTimeout =  20;
    private long sleepTime=800;
    private long startTime;
    private long endTime;

    private monitorLog RuntimeMonitor;
    private monitorLog sqlThread;

    @Before
    public void setUp() throws IOException {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mUidevice = UiDevice.getInstance(mInstrumentation);
        mUidevice.setCompressedLayoutHeirarchy(true);
        mPackage = mUidevice.getCurrentPackageName();
        mainActivity= normalUtil.V().getRunningActivity(mUidevice);
        startTime=System.currentTimeMillis();
        LogUtil.V().log("————————Start Info——————————");
        Log.d("Runtime","package_Name:"+mPackage);
        Log.d("Runtime","main_Activity:"+mainActivity);
        LogUtil.V().log("————————Start Info——————————");

    }
    @After
    public void tearDown() throws Exception {
        LogUtil.V().log("————————Finish——————————");
        RuntimeMonitor.interrupt();
        sqlThread.interrupt();
    }


    public void preStage() {

        //mUidevice.executeShellCommand("logcat -c");  //clear log
        RuntimeMonitor=new monitorLog(mPackage,"Runtime",false); //"Runtime_" + time + ".log"
        RuntimeMonitor.start();
        sqlThread=new monitorLog(mPackage,"sql",false);// "staticinfo.log"
        sqlThread.start();
//        Context context=mInstrumentation.getContext();
//        CrashCat.getInstance(context, mPackage,"crashLog.txt");
//        staticInfo.V().getAllStaticInfo(mPackage);  //
    }


    @Test
    public void testBFS() {
        preStage();  //only start thread ,not read STG
        LogUtil.V().log("————————Testing ——————————");

        // winID, name, onClickElements, clickedElements, finished ,Xpath
        mainWin=getCurWindow();
        unFinishWins.offer(mainWin);
        queueIDSet.add(mainWin.getWinID());
        while(!unFinishWins.isEmpty()){
            long isEmpty=0;

            endTime=System.currentTimeMillis();
            long time=((endTime-startTime)/1000)/60;
            if(time>=testTimeout){
                Log.d("Runtime","outOfTime");
                break;
            }
            window testAct=unFinishWins.poll();
            LogUtil.V().log(testAct.toString());
            if(AllWinIDset.add(testAct.getWinID())==true ){
                Graph.V().getWindows().add(testAct);
            }
            /**
             *
             * */
            if(srcJumpToTgtWin(testAct)==false){
                testAct.setFinished(true);
                LogUtil.V().log("!!!!!!Jump to the state to be tested, continue to the next state");
                continue;
            }
            LogUtil.V().log("+++++++++++++++++++++++");
            while(testAct.getEleIDs().size()>testAct.getClickedEleIDs().size()){
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
                            unFinishWins.offer(curWin);
                            queueIDSet.add(curWin.getWinID());
                        }
                    }

                    LogUtil.V().log("Ready to return to the original state");
                    if(backToTgtWin(testAct)==true){
                        continue;
                    }else{
                        LogUtil.V().log("！！！If the original state to be tested cannot be reached, give up the state and test the next state in the queue");
                        break;
                    }
                }

            }
            Log.d("Runtime","！！！Number of incorrect clicks in this test status："+isEmpty);
            testAct.setFinished(true);
            LogUtil.V().log("+++++++++++ENDEND++++++++++++");
        }
        LogUtil.V().log("——————————————————");
    }



    public window getCurWindow(){

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LogUtil.V().log(" ~~~~~~~~~~~~~~~");
        if(!mUidevice.getCurrentPackageName().equals(mPackage)){
            window curWin1=new window();
            curWin1.setWinID(11111);
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
            LogUtil.V().log("******");
            List<element> elements=new ArrayList<>();
            if (ob.getChildCount()!=0){
                getSbuElement(ob,elements);
                curWin.getOnClickElements().addAll(elements);
            }else {
                // text, resourceName,class,content_desc,clickable，，，
                element e=new element();
                e.setClickable(true);//ob.isClickable()
                e.setContent_desc(ob.getContentDescription());
                e.setEleClass(ob.getClassName());
                e.setResName(ob.getResourceName());
                e.setText(ob.getText());
                e.setXpath();
                e.setEleID();
                curWin.getOnClickElements().add(e);

            }
        }
        curWin.setEleIDs();
        curWin.setXpaths();
        curWin.setWinID();

        StringBuffer stringBuffer=new StringBuffer();
        stringBuffer.append(" [State ID："+curWin.getWinID()+"]").append(" [Name："+curWin.getName()+"]").append(" [OnclickEle："+curWin.getOnClickElements().size()+"]");
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
            LogUtil.V().log("back success");
            LogUtil.V().log(" 《《《backToTgtWin》》》");
            return true;
        } else {
            /*
            *
            * */
//            LogUtil.V().log("！！！Next back");
//
//            mUidevice.pressBack();
//            window curWin1 = getCurWindow();
//            if (tgtWin.getWinID()==curWin1.getWinID()) {
//                LogUtil.V().log("next back success");
//                LogUtil.V().log(" 《《《backToTgtWin》》》");
//                return true;
//            }else{
//                LogUtil.V().log("！！！");
//            }
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
            LogUtil.V().log("!!!The current status does not belong to the app, so restart the app");
            normalUtil.V().startApp(mPackage,mInstrumentation);
        }
        window curWin=getCurWindow();
        LogUtil.V().log("current win ID"+curWin.getWinID()+" current activity："+curWin.getName());
        LogUtil.V().log("target win ID"+tgtWin.getWinID()+"  target activity："+tgtWin.getName());
        if(tgtWin.getWinID()==curWin.getWinID()){
            LogUtil.V().log("succeed");
            return true;
        }
        if(curWin.getWinID()==mainWin.getWinID()){

            LogUtil.V().log("The current status is main. Try to go from main to tgtWin ");
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
            }else if(!paths.contains("#")){  //Direct connection
                LogUtil.V().log("Direct connection");
                int eleID=Integer.parseInt(paths);
                LogUtil.V().log("eleID："+eleID);
                element ELE= curWin.findEleByID(eleID);
                if (ELE==null){
                    LogUtil.V().log("！！！The element is not found in cur status. Jump failed");
                }else {
                    if(newClickByElement(ELE)==true){
                        window curWin1=getCurWindow();
                        LogUtil.V().log("current win ID"+curWin1.getWinID());
                        LogUtil.V().log("target win ID"+tgtWin.getWinID());
                        if(tgtWin.getWinID()==curWin1.getWinID()){
                            LogUtil.V().log("jump success");
                            LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                            return true;
                        }else {
                            LogUtil.V().log("！！！Click the element, but the jump is unsuccessful. Try to find the path from main");
                        }
                    }
                }
            }else if(paths.contains("#")){
                LogUtil.V().log("PathID："+paths);
                String[] ID= paths.split("#");
                LogUtil.V().log("Number of jumps to perform："+ID.length);
                for (int i=0;i<ID.length;i++){
                    int eleID1=Integer.parseInt(ID[i]);
                    LogUtil.V().log("eleID1："+eleID1);
                    element ELE1= curWin.findEleByID(eleID1);
                    if (ELE1==null){
                        LogUtil.V().log("！！！The element is not found in cur status. Jump failed");
                        break;
                    }else {
                        if(newClickByElement(ELE1)==true){
                            window curWin1=getCurWindow();
                            if(tgtWin.getWinID()==curWin1.getWinID()){
                                LogUtil.V().log("jump success");
                                LogUtil.V().log(" 《《srcJumpToTgtWin》》");
                                return true;
                            }
                            curWin=curWin1;
                        }else{
                            LogUtil.V().log("！！！There was a failure during the path jump");
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
        }
        LogUtil.V().log("Starting from main to tgtwain pathfinding");
        LogUtil.V().log("cur win ID"+curWin.getWinID());
        LogUtil.V().log("target win id"+tgtWin.getWinID());
        if(tgtWin.getWinID()==curWin.getWinID()){
            LogUtil.V().log("success");
            LogUtil.V().log(" 《mainJumpToTgtWin》");
            return true;
        }
        if (curWin.getWinID()!=mainWin.getWinID()){
            LogUtil.V().log("！！！After restart, it is not in the main state. There is a problem");
            LogUtil.V().log(" 《mainJumpToTgtWin》");
            return false;
        }
        String paths=Graph.V().getPath(curWin.getWinID(),tgtWin.getWinID());// “null”  ,  “ID” ，“ID#ID#”
        if(paths.equals("null")){
            LogUtil.V().log("！！！Path not found from main");
            LogUtil.V().log(" 《mainJumpToTgtWin》");
            return false;
        }else if(!paths.contains("#")){  //
            LogUtil.V().log("direct connect");
            int eleID=Integer.parseInt(paths);
            LogUtil.V().log("Control ID of the edge："+eleID);
            element ELE= curWin.findEleByID(eleID);
            if (ELE==null){
                LogUtil.V().log("！！！The element was not found. Failed to jump from main");
                LogUtil.V().log(" 《mainJumpToTgtWin》");
                return false;
            }else {
                if(newClickByElement(ELE)==true){
                    window curWin1=getCurWindow();
                    if(tgtWin.getWinID()==curWin1.getWinID()){
                        LogUtil.V().log("jump success");
                        LogUtil.V().log(" 《mainJumpToTgtWin》");
                        return true;
                    }else {
                        LogUtil.V().log("！！！Click the element, but the jump is unsuccessful. Try to find the path from main");
                        LogUtil.V().log(" 《mainJumpToTgtWin》");
                        return false;
                    }
                }else{
                    LogUtil.V().log("！！！The UI element cannot be found, so the jump from main failed");
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
                    LogUtil.V().log("！！！The element was not found. Failed to jump from main");
                    LogUtil.V().log(" 《mainJumpToTgtWin》");
                    return false;
                }else {
                    if(newClickByElement(ELE1)==true){
                        window curWin1=getCurWindow();
                        if(tgtWin.getWinID()==curWin1.getWinID()){
                            LogUtil.V().log("jump success");
                            LogUtil.V().log(" 《mainJumpToTgtWin》");
                            return true;
                        }
                        curWin=curWin1;
                    }else{
                        LogUtil.V().log("！！！fail");
                        LogUtil.V().log(" 《mainJumpToTgtWin》");
                        return false;
                    }
                }
            }
            LogUtil.V().log(" 《mainJumpToTgtWin》");
            return false;
        }else{
            LogUtil.V().log("！！！fail, paths::"+paths);
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
