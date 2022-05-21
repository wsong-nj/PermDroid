package zzg.staticanalysis.model;

import zzg.staticanalysis.Manager;
import zzg.staticguimodel.Widget;

public class Transition {

	private long id;
	
	private String label;
	private Widget widget;
	private long srcId;
	private long tgtId;
	
	public Transition() {
		Manager.v().add(this);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Widget getWidget() {
		return widget;
	}

	public void setWidget(Widget widget) {
		this.widget = widget;
	}

	public long getSrc() {
		return srcId;
	}

	public void setSrc(long srcId) {
		this.srcId = srcId;
	}

	public long getTgt() {
		return tgtId;
	}

	public void setTgt(long tgtId) {
		this.tgtId = tgtId;
	}
	
}
