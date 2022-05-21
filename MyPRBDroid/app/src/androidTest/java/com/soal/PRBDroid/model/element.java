package com.soal.PRBDroid.model;



import android.graphics.Point;

// text, resourceName,class,content_desc,clickable
public class element {
    private int eleID;
    private String eleClass;
    private String content_desc;
    private String text;
    private String resName;
    private boolean isClickable;
    private String Xpath;
    private Point point;



    /////set////

    public void setEleClass(String eleClass) {
        this.eleClass = eleClass;
    }

    public void setContent_desc(String content_desc) {
        this.content_desc = content_desc;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setResName(String resName) {
        this.resName = resName;
    }

    public void setClickable(boolean clickable) {
        isClickable = clickable;
    }

    public void setEleID() {
        this.eleID = Xpath.hashCode();
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public void setXpath(){

        Xpath="class:"+eleClass+",contant_desc:"+content_desc+",resName:"+resName+",isClickable:"+isClickable+",point:"+point.x+point.y;

    }

    /////get///

    public String getEleClass() {
        return eleClass;
    }

    public String getContent_desc() {
        return content_desc;
    }

    public String getText() {
        return text;
    }

    public String getResName() {
        return resName;
    }

    public boolean isClickable() {
        return isClickable;
    }

    public String getXpath() {
        return Xpath;
    }

    public int getEleID() {
        return eleID;
    }

    public Point getPoint() {
        return point;
    }

    @Override
    public String toString() {
        return "element{" +
                "eleID=" + eleID +
                ", eleClass='" + eleClass + '\'' +
                ", content_desc='" + content_desc + '\'' +
                ", text='" + text + '\'' +
                ", resName='" + resName + '\'' +
                ", isClickable=" + isClickable +
                '}';
    }
}
