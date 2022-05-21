package com.soal.PRBDroid.model;

import com.soal.PRBDroid.LogUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;



// winID, name, onClickElements, EleIDs，clickedEleIDs, finished ,Xpath


public class window {
    private  int winID;
    private  String name;
    private List<element> onClickElements=new ArrayList<>();  //All clickable elements
    //Add a function to judge whether the next element is repeated and add a de duplication.
    private Set<Integer>  EleIDs=new HashSet<>(); //Clickable element ID set
    private Set<Integer>  clickedEleIDs=new HashSet<>();  //ID set that has been clicked
    private boolean finished;
    private String Xpaths=null;   //Used to store the status identifier, "or how to judge whether the two UI interfaces are consistent"

    //for static info
    private String permissions=null;
    private int isActivityTest=0;   //

    ////Avoid adding the same ele//
    public boolean hasThisOnCliEle(element ele1) {
        //Ideally, after comparing all existing nodes, false will be returned if they are not found. If one with the same ID is found halfway, true will be returned
        Iterator<element> iterator=onClickElements.iterator();
        while (iterator.hasNext()){
            element temp=iterator.next();
            if (ele1.getEleID()==temp.getEleID()){
                return true;
            }
        }
        return  false;
    }
    public void addDifOnCliEle(List<element> ele1) {

        Iterator<element> iteratorEle1=ele1.iterator();
        while (iteratorEle1.hasNext()){
            element temp=iteratorEle1.next();
            if(hasThisOnCliEle(temp)==true){
                continue;
            }else{
                onClickElements.add(temp);
            }
        }
    }

    //////set//////////
    public void setXpaths(){
        Iterator<element> iterator=onClickElements.iterator();
        while (iterator.hasNext()){
            element ele=(element) iterator.next();
            if (Xpaths!=null){
                Xpaths=Xpaths+"||"+ele.getXpath();
            }else{
                Xpaths=ele.getXpath();
            }

        }
    }

    public void setWinID() {
        String ss=name+Xpaths;
        this.winID =ss.hashCode();
    }
    public void setWinID(int a) {
        this.winID =a;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void addEleID(int eleID) {
        this.EleIDs.add(eleID);
    }
    public void setEleIDs(){
        Iterator<element> iterator=onClickElements.iterator();
        while (iterator.hasNext()){
            element ele=(element) iterator.next();
            EleIDs.add(ele.getEleID());
        }
    }
    public void resetClickedEleIDs(){
        clickedEleIDs.clear();
    }

    public void addClickedEleID(int eleID) {
        this.clickedEleIDs.add(eleID);
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public void setIsActivityTest(int isActivityTest) {
        this.isActivityTest = isActivityTest;
    }

    /////////get////////
    public String  getXpaths(){
        return Xpaths;
    }

    public int getWinID() {
        return winID;
    }

    public String getName() {
        return name;
    }

    public boolean isFinished() {
        return finished;
    }

    public List<element> getOnClickElements() {
        return onClickElements;
    }

    public Set<Integer> getEleIDs() {
        return EleIDs;
    }

    public Set<Integer> getClickedEleIDs() {
        return clickedEleIDs;
    }

    public String getPermissions() {
        return permissions;
    }
    public void addPermissions(String ps) {
        permissions=permissions+ps;
    }

    public int getIsActivityTest() {
        return isActivityTest;
    }

    public element getNextElement(){
        LogUtil.V().log("get the next element");
        Set<Integer>  result=new HashSet<>();
        result.addAll(EleIDs);
        result.removeAll(clickedEleIDs);
        Iterator<Integer> it= result.iterator();
        int nextID=(int)it.next();
        Iterator<element> itEles=onClickElements.iterator();
        while (itEles.hasNext()){
            element temp=(element) itEles.next();
            if (temp.getEleID()==nextID){
                clickedEleIDs.add(nextID);
                return  temp;
            }

        }
        return null;
    }

    public element findEleByID(int id){
        Iterator<element> iterator=onClickElements.iterator();
        while (iterator.hasNext()){
            element temp=iterator.next();
            if (temp.getEleID()==id){
                return temp;
            }
        }
        return null;
    }


    public boolean equal(window Bwin){

        if (this.name.equals(Bwin.name) && this.onClickElements.size()==Bwin.onClickElements.size()){
            if (this.Xpaths.equals(Bwin.Xpaths)){
                return  true;
            }else{
                return false;
            }
        }else{

            StringBuffer stringBuffer=new StringBuffer();
            stringBuffer.append("the current activity："+name+"\n");
            stringBuffer.append("need tested activity："+Bwin.getName()+"\n");
            stringBuffer.append("the current activity's clickEle size"+EleIDs.size());
            stringBuffer.append("the need tested activity's clickEle size："+Bwin.getEleIDs().size()+"\n");
            LogUtil.V().log(stringBuffer.toString());
            return  false;
        }
    }

    @Override
    public String toString() {
        return "window{" +
                "winID=" + winID +
                ", name='" + name + '\'' +
                ", onClickElements.size=" + onClickElements.size() +
                ", EleIDs.size=" + EleIDs.size() +
                ", clickedEleIDs.size=" + clickedEleIDs.size() +
                ", finished=" + finished +
                ", Xpaths='" + Xpaths + '\'' +
                '}';
    }
}
