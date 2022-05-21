package zzg.staticguimodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Graph implements Serializable {
	private static final long serialVersionUID = 1L;

	private List<Node> nodes = new ArrayList<Node>();
	private List<Edge> edges = new ArrayList<Edge>();
	private Set<Widget> widgets=new HashSet<Widget>();
	public Graph() {}

	public Set<Widget> getWidgets() {
		return widgets;
	}

	public void setWidgets(Set<Widget> widgets) {
		this.widgets = widgets;
	}
	public void addWidget(Widget widget) {
		this.widgets.add(widget);
	}
	public void addAllWidgets(List<Widget> Ls) {
		this.widgets.addAll(Ls);
	}
	public List<Node> getNodes() {
		return nodes;
	}
	
	
	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}
	
	public void add(Node node) {
		nodes.add(node);
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public void setEdges(List<Edge> edges) {
		this.edges = edges;
	}
	
	public void add(Edge edge) {
		edges.add(edge);
	}
	
	public String getNodeSize() {
		int an = 0, fn = 0;
		for(Node n : nodes) {
			if(n.getnType().equals(NodeType.ACTIVITY))
				an++;
			else if(n.getnType().equals(NodeType.FRAGMENT))
				fn++;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(" [ACTIVITY ").append(an)
			.append("; Fragment ").append(fn)
			.append("; All ").append(nodes.size())
			.append("]\n");
		return sb.toString();
	}
	
	public int getRealEdgeSize() {
		int i = 0;
		for(Edge e : edges) {
			if(e.getWidget() != null)
				i++;
		}
		return i;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("nodes={\n");
		for(Node n : nodes) {
			sb.append(n.toString()).append("\n");
		}
		sb.append("}\nedges={\n");
		for(Edge e : edges) {
			sb.append(e.toString()).append("\n");
		}
		sb.append("}");
		return sb.toString();
	}
	
	public Node getNodeByName(String name) {
		for(Node node : nodes){
			if(node.getName() != null && node.getName().equals(name)){
				return node;
			}
		}
		return null;
	}
}
