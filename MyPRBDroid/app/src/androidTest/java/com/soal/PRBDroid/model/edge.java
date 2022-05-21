package com.soal.PRBDroid.model;
// edgeID, winID,tgtID,element
public class edge {
    private  int edgeID;
    private  int winID;
    private  int tgtID;
    private  element widget;
    public edge(){

    }
    public edge(int winID, int tgtID, element widget) {
        this.winID = winID;
        this.tgtID = tgtID;
        this.widget = widget;
    }
    /////////set/////////////

    public void setEdgeID(int edgeID) {
        this.edgeID = edgeID;
    }

    public void setWinID(int winID) {
        this.winID = winID;
    }

    public void setTgtID(int tgtID) {
        this.tgtID = tgtID;
    }

    public void setWidget(element widget) {
        this.widget = widget;
    }
    ////////////get////////////

    public int getEdgeID() {
        return edgeID;
    }

    public int getWinID() {
        return winID;
    }

    public int getTgtID() {
        return tgtID;
    }

    public element getWidget() {
        return widget;
    }

}
