package zzg.staticanalysis.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import zzg.staticanalysis.Manager;
import zzg.staticguimodel.NodeType;
import zzg.staticguimodel.Widget;

public class BaseNode {

	private long id;
	private boolean test;
	private NodeType nType;
	private List<Widget> widgets = new ArrayList<Widget>();
	
	// here
	private int isActivityTest;
	private String permissions;

	// here
	public int getIsActivityTest() {
		return isActivityTest;
	}

	public void setIsActivityTest(int isActivityTest) {
		this.isActivityTest = isActivityTest;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
	protected BaseNode(long id) {
		this.id = id;
	}
	
	public BaseNode(long id, NodeType nType) {
		this.id = id;
		this.nType = nType;
		Manager.v().add(this);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public NodeType getNodeType() {
		return nType;
	}

	public void setNodeType(NodeType nType) {
		this.nType = nType;
	}

	public List<Widget> getWidgets() {
		return widgets;
	}

	public void setWidgets(List<Widget> widgets) {
		this.widgets = widgets;
	}

	public void addWidget(Widget w) {
		this.widgets.add(w);
	}

	public void addAllWidgets(List<Widget> widgets) {
		this.widgets.addAll(widgets);
	}

//	public Set<Transition> getOutEdges() {
//		return outEdges;
//	}
//
//	public void setOutEdges(Set<Transition> edges) {
//		this.outEdges = edges;
//	}
//
//	public void addOutEdge(Transition edge) {
//		this.outEdges.add(edge);
//	}
//
//	public void addAllOutEdges(Set<Transition> edges) {
//		this.outEdges.addAll(edges);
//	}

	public boolean isTest() {
		return test;
	}

	public void setTest(boolean test) {
		this.test = test;
	}
}
