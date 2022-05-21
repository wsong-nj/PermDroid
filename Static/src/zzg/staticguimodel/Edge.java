package zzg.staticguimodel;

import java.io.Serializable;

public class Edge implements Serializable {
	private static final long serialVersionUID = 1L;

	private long id;
	private Widget widget;
	private Node src;
	private Node tgt;
	
	private String note;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Widget getWidget() {
		return widget;
	}
	public void setWidget(Widget widget) {
		this.widget = widget;
	}
	public Node getSrc() {
		return src;
	}
	public void setSrc(Node src) {
		this.src = src;
	}
	public Node getTgt() {
		return tgt;
	}
	public void setTgt(Node tgt) {
		this.tgt = tgt;
	}
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" { id=\'").append(id).append("\' ");
		if(src != null)
			sb.append(src.getId());
		sb.append(" -> ");
		if(tgt != null)
			sb.append(tgt.getId());
		sb.append("\twidget-id=\'");
		if(widget != null)
			sb.append(widget.toString());
		sb.append("\'");
		if(note != null && note.length() > 0) {
			sb.append("\tnote=\'").append(note).append("\'");
		}
		sb.append("}");
		return sb.toString();
	}
	//bw.write("Source,Target,Type,Kind,Id,Label,Weight\n");
	//bw.write("id,label,srcID,tgtID,widget\n");
	public String toCsv() {
		StringBuilder sb = new StringBuilder();
		sb.append(id).append(",");//Id
		sb.append(note == null ? "" : note).append(",");//label
		sb.append(src != null ? src.getId() : "0").append(",");//Source
		sb.append(tgt != null ? tgt.getId() : "0").append(",");//Target
		sb.append(widget == null ? 0 : widget.toCsv()).append(",");//Weight
		//sb.append("Directed,");//Type
		//sb.append("1.5").append("\n");//Label  ШЈжи
		sb.append("\n");
		return sb.toString();
	}
}
