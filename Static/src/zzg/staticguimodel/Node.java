package zzg.staticguimodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.model.BaseNode;

public class Node implements Serializable {
	private static final long serialVersionUID = 1L;

	private long id;
	private boolean test;
	//here
	private int isActivityTest;   //<三种状态“0”，只测纯widget“1”,混合的/纯activity的“2”>
	private String permissions; 
	private String fragIDString;
	//here
	private NodeType nType;
	private List<Widget> widgets = new ArrayList<Widget>();
	private String name;  //其实就是label
	
	//完全体ActNode，这样才能有更多信息  ，所以我添加了下面的东西
	private long contextMenuNodeID;
	private long optionsMenuNodeID;
	//该fragment可以达到的fragment（通过replace或add）
	private Set<String> fragmentsName = new HashSet<String>();
	private long leftDrawerNodeID;//The drawer slides out from the left
	private long rightDrawerNodeID;//The drawer slides out from the right
	
	
	public String getFragIDString() {
		return fragIDString;
	}
	public void setFragIDString(String fragIDString) {
		this.fragIDString = fragIDString;
	}
	public String getPermissions() {
		return permissions;
	}
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
	public int getIsActivityTest() {
		return isActivityTest;
	}
	public void setIsActivityTest(int isTest) {
		this.isActivityTest = isTest;
	}
	public long getContextMenu() {
		return contextMenuNodeID;
	}
	public void setContextMenu(long contextMenuid) {
		this.contextMenuNodeID = contextMenuid;
	}
	public long getOptionsMenu() {
		return optionsMenuNodeID;
	}
	public void setOptionsMenu(long optionsMenuid) {
		this.optionsMenuNodeID = optionsMenuid;
	}
	public Set<String> getFragmentsName() {
		return fragmentsName;
	}
	public void setFragmentsName(Set<String> fragmentsName) {
		this.fragmentsName = fragmentsName;
	}
	public long getLeftDrawer() {
		return leftDrawerNodeID;
	}
	public void setLeftDrawer(long leftDrawerid) {
		this.leftDrawerNodeID = leftDrawerid;
	}
	public long getRightDrawer() {
		return rightDrawerNodeID;
	}
	public void setRightDrawer(long rightDrawerid) {
		this.rightDrawerNodeID = rightDrawerid;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public boolean isTest() {
		return test;
	}
	public void setTest(boolean test) {
		this.test = test;
	}
	public NodeType getnType() {
		return nType;
	}
	public void setnType(NodeType nType) {
		this.nType = nType;
	}
	public List<Widget> getWidgets() {
		return widgets;
	}
	public void setWidgets(List<Widget> widgets) {
		this.widgets = widgets;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" { id=\'").append(id)
			.append("\'\ttest=\'").append(test)
			.append("\'\tnode-type=\'").append(nType.toString())
			.append("\'\tname=\'");
		if(name != null) {
			sb.append(name);
		}
		if(widgets.size() > 0) {
			sb.append("\'\twidgets=[");
			for(Widget w : widgets) {
				sb.append(w.toString());
			}
			sb.append("]");
		}
		sb.append("};");
		return sb.toString();
	}
	//bw.write("id,name,test,type,fragmentsNameSize,contextMenuID,optionsMenuID,leftDrawerID,rightDrawerID,isStatic,Widgets\n");   
	//sb.append(text != null ? text : "无").append(",");//
	public String toCsv() {
		StringBuilder sb = new StringBuilder();
		sb.append(id).append(",");  //id
		if(name != null)  //name
			sb.append(name.replace(AppParser.v().getPkg()+".", "")).append(",");
		else
			sb.append(nType.toString()).append(id).append(",");
		sb.append(test).append(",");  //test
		sb.append(nType.toString()).append(","); //type
		
		sb.append(fragmentsName != null ?  fragmentsName.size(): "0").append(",");//
		sb.append(contextMenuNodeID).append(",");
		sb.append(optionsMenuNodeID).append(",");
		sb.append(leftDrawerNodeID).append(",");
		sb.append(rightDrawerNodeID).append(",");
		sb.append("<sizes(").append(widgets.size()).append(")");
		for(Widget w : widgets) {
			if(w == null)
				continue;
			sb.append(" [").append(w.toCsv()).append("] ");
		}
		sb.append(">\n");
		return sb.toString();
	}
}
