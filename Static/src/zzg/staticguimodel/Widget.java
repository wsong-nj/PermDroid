package zzg.staticguimodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jf.smali.smaliParser.integer_literal_return;

import soot.SootMethod;
import zzg.staticanalysis.AppParser;

public class Widget implements Serializable {
	private static final long serialVersionUID = 1L;

	private long id;
	
	//here
	private long winID;
	private String winName;
	private String itemIDString;
	private String permissions;
	private int ItemID;
	private int SubMenuID;
	//here
	private String type;
	/* in R.class, resName=0xX, and X is converted from hexadecimal to decimal to give resId */
	private int resId;
	/* android:id="@+id/resName" */
	private String resName;
	private String text;
	private String eventMethod;//if equals "openContextMenu", is edge to context menu
	/* Event response method */
	private transient SootMethod eventHandler;
	/* click, longclick, slide... */
	private EventType eventType;
	
	private List<Widget> dependencies = new ArrayList<Widget>();
	
	private boolean test;
	
	public  boolean isStatic=false;   //用于记录是否静态注册了onClick
	
	public Widget() {}
	
	
	public String getItemIDString() {
		return itemIDString;
	}


	public void setItemIDString(String itemIDString) {
		this.itemIDString = itemIDString;
	}
	

	public int getItemID() {
		return ItemID;
	}


	public void setItemID(int itemID) {
		ItemID = itemID;
	}


	public int getSubMenuID() {
		return SubMenuID;
	}


	public void setSubMenuID(int subMenuID) {
		SubMenuID = subMenuID;
	}


	//================Setter======================
	public void setWinID(long id) {
		this.winID = id;
	}
	public String getWinName() {
		return winName;
	}

	public void setWinName(String winName) {
		this.winName = winName;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	public long getWinID() {
		return winID;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setResName(String resName) {
		this.resName = resName;
	}

	public void setResId(int resId) {
		this.resId = resId;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setEventHandler(SootMethod eventHandler) {
		this.eventHandler = eventHandler;
	}

	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	public void setDependencies(List<Widget> dependencies) {
		this.dependencies = dependencies;
	}

	public void setEventMethod(String eventMethod) {
		this.eventMethod = eventMethod;
	}

	public void setTest(boolean test) {
		this.test = test;
	}
	//================Getter======================

	public long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getResName() {
		return resName;
	}

	public int getResId() {
		return resId;
	}

	public String getText() {
		return text;
	}

	public SootMethod getEventHandler() {
		return eventHandler;
	}

	public EventType getEventType() {
		return eventType;
	}

	public List<Widget> getDependencies() {
		return dependencies;
	}
	
	public String getEventMethod() {
		return eventMethod;
	}

	public boolean isTest() {
		return test;
	}
	//================Add======================
	
	public void addDependentWidget(Widget dw) {
		dependencies.add(dw);
	}
	



	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<(id:").append(id).append(") type:").append(type).append(", ");
		if(resName != null)
			{sb.append("resName:").append(resName).append(", ");}
		else {
			sb.append("resName:空").append(", ");
			}
		sb.append(", resID:").append(resId).append(", ");
		if(text != null)
			{sb.append("text:").append(text).append(", ");}
		else {
			sb.append("text:空").append(", ");
		}
		sb.append(", eventType").append(eventType).append(", ");
		//eventmethod ,test ,dependencies这三个还没输出；我加上了
		if (eventMethod != null) {
			sb.append("eventmethod:").append(eventMethod).append(", ");
		} else {
			sb.append("eventmethod:空").append(", ");
		}
		sb.append("isTest:").append(test).append(", ");
		sb.append("hasDependWidgets:").append(dependencies.size()).append(">;");
		return sb.toString();
	}
	
	public String toCsv() {
		StringBuilder sb = new StringBuilder();
		sb.append("id:").append(id).append("|");
		sb.append("type:").append(type).append("|");
		sb.append("resName:").append(resName != null ? resName : "...").append("|");//
		sb.append("resID:").append(resId).append(",");
		sb.append("text:").append(text != null ? text : "...").append("|");//
		sb.append("eventMethod:").append(eventMethod != null ? eventMethod : "...").append("|");//
		//sb.append("eventHandler:").append(eventHandler != null ? eventHandler.getName() : "...").append("|");//
		sb.append("eventType:").append(eventType).append(",|");
		sb.append("isTest:").append(test).append("|");
		sb.append("depSizes:").append(dependencies.size());
		return sb.toString();
	}
	
//	public String toCsv() {
//		StringBuilder sb = new StringBuilder();
//		sb.append(id).append(",");
//		if(name != null)
//			sb.append(name.replace(AppParser.v().getPkg()+".", "")).append(",");
//		else
//			sb.append(nType.toString()).append(id).append(",");
//		sb.append(test).append(",");
//		sb.append(nType.toString()).append(",");
//		for(Widget w : widgets) {
//			if(w == null)
//				continue;
//			sb.append(w.toCsv());
//		}
//		sb.append("\n");
//		return sb.toString();
	
//	sb.append(src != null ? src.getId() : "0").append(",");//Source
//	sb.append(tgt != null ? tgt.getId() : "0").append(",");//Target
//	}
	
	public String newtoCSV() {
		StringBuffer sb = new StringBuffer();
		// id,type,resName,resID,text,eventMethod,eventHandler,eventType,test,dependSizes,isStatic
		sb.append(id).append(",");
		sb.append(type).append(",");
		sb.append(resName != null ? resName : "...").append(",");//
		sb.append(resId).append(",");
		sb.append(text != null ? text : "...").append(",");//
		sb.append(eventMethod != null ? eventMethod : "...").append(",");//
		sb.append(eventHandler != null ? eventHandler.getName() : "...").append(",");//
		sb.append(eventType).append(",");
		sb.append(test).append(",");
		sb.append(dependencies.size()).append(",");
		sb.append(isStatic).append(",");
		sb.append("\n");
		return sb.toString();
	}
}
