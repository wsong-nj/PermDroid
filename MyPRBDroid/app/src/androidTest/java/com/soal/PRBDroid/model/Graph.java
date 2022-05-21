package com.soal.PRBDroid.model;

import com.soal.PRBDroid.LogUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class Graph {
    private Set<window> windows=new HashSet<>();
    private Set<edge> edges=new HashSet<>();
    private static Graph instance;
    private int[] IDMap;
    private Map<Integer,Integer> WinIDtoIndex;
    private int[][] maxtrix;   //The architecture of this diagram will overwrite the previous value, keep the last eleid of [i] [J], I - > J, and click ele. Ideally, record all through an array or string
    private int[][] tmpMaxtrix; //The above is the recorded ∞ or elementid, and this matrix is 1000 or 1 (use this matrix to avoid errors);
     Stack<Integer> stack=new Stack<>();
    public Graph() {
    }
    public static Graph V(){
        if (instance==null){
            synchronized (Graph.class){
                if (instance==null){
                    instance=new Graph();
                }
            }
        }
        return instance;
    }

    ///////geter////////
    public Set<window> getWindows() {
        return windows;
    }

    public Set<edge> getEdges() {
        return edges;
    }
    ///////geter////////

    //Generate a map, and 0 ~ win. Size () - 1 store their respective IDs
    public  void getIDMap(){
        int [] map=new int[windows.size()];
        Map<Integer,Integer> tempWinIDtoIndex=new HashMap<>();
        Iterator<window> iterator=windows.iterator();
        int index=0;
        while (iterator.hasNext()){
            window tempWin=iterator.next();
            tempWinIDtoIndex.put(tempWin.getWinID(),index);  //from zero start
            map[index]=tempWin.getWinID();
            index=index+1;
        }
        WinIDtoIndex=tempWinIDtoIndex;
        IDMap=map;
    }
    public void getMatrix(){
        getIDMap();
        int[][] maxtrix1=new int[IDMap.length][IDMap.length];
        int[][] tmpMaxtrix1=new int[IDMap.length][IDMap.length];
        for (int i=0;i<IDMap.length;i++){
            for (int j=0;j<IDMap.length;j++){
                maxtrix1[i][j]=Integer.MAX_VALUE;
                tmpMaxtrix1[i][j]=1000;
            }
        }
        Iterator<edge> iterator=edges.iterator();
        while (iterator.hasNext()){
            edge tempEdge=iterator.next();
            int srcID=tempEdge.getWinID();
            int tgtID=tempEdge.getTgtID();
            int srcIndex=WinIDtoIndex.get(srcID);
            int tgtIndex=WinIDtoIndex.get(tgtID);
            maxtrix1[srcIndex][tgtIndex]=tempEdge.getWidget().getEleID();
            tmpMaxtrix1[srcIndex][tgtIndex]=1;
        }
        maxtrix=maxtrix1;
        tmpMaxtrix=tmpMaxtrix1;
    }
    /*
    * Dijkstra:
    *
    * */


    public String getPath(int srcID,int tgtID){   // window.getWinID();
        // prepare state
        getMatrix(); //
        printMaxtrix();//Print
        int [] dis=new int[IDMap.length];   // Record the initial distance from srcid to the remaining vertices
        int [] T=new int[IDMap.length];  //Save the vertices of the shortest path that has been found, 1 = true, 0 = false;
        int [] preIndex=new int[IDMap.length];  //Each time the shortest path is updated, the precursor node ID is recorded
        if (WinIDtoIndex.get(srcID)==null){
            LogUtil.V().log("STG con't find srcID node ");
            return "null";
        }
        int srcIndex=WinIDtoIndex.get(srcID);
        if (WinIDtoIndex.get(tgtID)==null){
            LogUtil.V().log("STG con't find tgtID node,getPath() fail");
            return "null";
        }
        int tgtIndex=WinIDtoIndex.get(tgtID);
        int maxVlaue=1000;

        //SRC - > TGT direct edge (special case)
        if(maxtrix[srcIndex][tgtIndex]!=Integer.MAX_VALUE){
            String eleID=String.valueOf(maxtrix[srcIndex][tgtIndex]);  //
            return  eleID;
        }
        //If there is no direct connection, the initial dis [i] value is assigned. If there is srcindex - > xindex edge, it is assigned 1. Otherwise, it is assigned 1000, indicating infinity
        for(int j=0;j<IDMap.length;j++){
            if(tmpMaxtrix[srcIndex][j]==maxVlaue){
                dis[j]=1000;
            }else {
                dis[j]=tmpMaxtrix[srcIndex][j];  //
                preIndex[j]=srcIndex;
            }
        }
        dis[srcIndex]=0; //
        T[srcIndex]=1; //
        for (int i=1;i<IDMap.length;i++){   //

            int minDis=1001;
            int minIndex = 0;
            for(int q=0;q<IDMap.length;q++){
                if(T[q]==0 && dis[q]<minDis){
                    minDis=dis[q];
                    minIndex=q;  //The node subscript of the shortest distance is found
                }
            }
            T[minIndex]=1;
            if(minIndex==tgtIndex){  //
                if(minDis==1000){// ==1000
                    return "null";
                }
                int tempIndex=minIndex;
//                int[] pathss=new int[20]; //
//                int count=0;
//
//                while (preIndex[tempIndex]!=srcIndex){
//                        int qianquID=preIndex[tempIndex];
//                        pathss[count++]=maxtrix[qianquID][tempIndex];
//                        tempIndex=qianquID;
//                }
//                pathss[count]=maxtrix[srcIndex][tempIndex];
//                StringBuffer stringBuffer=new StringBuffer();
//                for(int ii=count;ii>=0;ii--) {
//                    stringBuffer.append(pathss[ii]).append("#"); //Indicates that the path length is > 1
//                }
//
//                return stringBuffer.toString();
                Stack<Integer> stack=new Stack<>();
                while (preIndex[tempIndex]!=srcIndex){
                        int qianquID=preIndex[tempIndex];
                        stack.push(maxtrix[qianquID][tempIndex]);
                        tempIndex=qianquID;
                }
                stack.push(maxtrix[srcIndex][tempIndex]);
                StringBuffer stringBuffer=new StringBuffer();
                while (!stack.isEmpty()){
                    stringBuffer.append(stack.pop()).append("#"); //Indicates that the path length is > 1
                }
                stack.clear();
                return stringBuffer.toString();

            }else{  //update  the dis and  the preIndex

                for (int w=0;w<IDMap.length;w++){
                    if (T[w]==0 && tmpMaxtrix[minIndex][w]!=1000 && tmpMaxtrix[minIndex][w]+dis[minIndex]<dis[w]){
                        dis[w]=tmpMaxtrix[minIndex][w]+dis[minIndex];
                        preIndex[w]=minIndex;
                    }
                }
            }
        }
        return "null";

    }



    public void printMaxtrix(){
        LogUtil.V().log("winID:"+IDMap.length);
        StringBuffer row=new StringBuffer();
        row.append("<");
        for (int i=0;i<IDMap.length;i++){
            if (i==IDMap.length-1){
                row.append(IDMap[i]).append(">");
            }else{
                row.append(IDMap[i]).append("#");
            }
        }
        LogUtil.V().log(row.toString());
        LogUtil.V().log("edge.size："+edges.size());
        LogUtil.V().log("adjacency matrix:");
        for (int i=0;i<maxtrix.length;i++){
            StringBuffer line=new StringBuffer();
            line.append("《");
            for (int j=0;j<maxtrix.length;j++){
                if (j==maxtrix.length-1){
                    if (maxtrix[i][j]==Integer.MAX_VALUE){
                        line.append("∞").append("》");
                    }else{
                        line.append(maxtrix[i][j]).append("》");
                    }
                }else{
                    if (maxtrix[i][j]==Integer.MAX_VALUE){
                        line.append("∞").append("#");
                    }else{
                        line.append(maxtrix[i][j]).append("#");
                    }
                }
            }
            LogUtil.V().log(line.toString());
        }
    }



}
