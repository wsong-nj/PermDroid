package zzg.staticanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import zzg.staticanalysis.model.ActNode;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.model.FragNode;
import zzg.staticanalysis.model.Transition;
import zzg.staticguimodel.Edge;
import zzg.staticguimodel.Graph;
import zzg.staticguimodel.MenuItem;
import zzg.staticguimodel.Node;
import zzg.staticguimodel.SubMenu;
import zzg.staticguimodel.Widget;

public class Manager {
	private static Manager instance = null;
	
	public static void reset() {
		instance = null;
	}
	
	private Manager() { }
	public static Manager v() {
		if(instance == null) {
			synchronized (Manager.class) {
				if(instance == null) {
					instance = new Manager();
				}
			}
		}
		return instance;
	}
	////////////////////////////manage nodes//////////////////
	private List<BaseNode> baseNodes = new ArrayList<BaseNode>();
	private Map<String, FragNode> fragNodes = new HashMap<String, FragNode>();
	private Map<String, ActNode> actNodes = new HashMap<String, ActNode>();
	
	public void add(BaseNode node) {
		this.baseNodes.add(node);
	}
	
	public void putFragNode(String name, FragNode fragNode) {
		this.fragNodes.put(name, fragNode);
	}
	public void putActNode(String name, ActNode actNode) {
		this.actNodes.put(name, actNode);
	}
	
	public List<BaseNode> getNodes() {
		return baseNodes;
	}
	
	public FragNode getFragNodeByName(String name) {
		return fragNodes.get(name);
	}
	
	public ActNode getActNodeByName(String name) {
		return actNodes.get(name);
	}
	 
	public Collection<FragNode> getFragNodes(){
		return fragNodes.values();
	}
	
	public Collection<ActNode> getActNodes() {
		return actNodes.values();
	}
	
	public BaseNode getNodeById(long id) {
		for(BaseNode bn : baseNodes) {
			if(bn.getId() == id)
				return bn;
		}
		for(FragNode fn : fragNodes.values()) {
			if(fn.getId() == id)
				return fn;
		}
		for(ActNode an : actNodes.values()) {
			if(an.getId() == id)
				return an;
		}
		return null;
	}
	
	////////////////////////////manage transitions//////////////////
	private Set<Transition> transitions = new HashSet<Transition>(); 
	
	public Set<Transition> getTransitions(){
		return transitions;
	}
	
	public void add(Transition t) {
		transitions.add(t);
	}
	
	public Set<Transition> getTransitionsBySourceId(long id) {
		Set<Transition> set = new HashSet<Transition>();
		for(Transition t : transitions) {
			if(t.getSrc() == id) {
				set.add(t);
			}
		}
		return set;
	}
	
	public Set<Transition> getTransitionsByTargetId(long id) {
		Set<Transition> set = new HashSet<Transition>();
		for(Transition t : transitions) {
			if(t.getTgt() == id) {
				set.add(t);
			}
		}
		return set;
	}
	
	public boolean hasTransition(long srcId, long tgtId) {
		for(Transition t : transitions) {
			if(t.getSrc() == srcId && t.getTgt() == tgtId) {
				return true;
			}
		}
		return false;
	}
	
	
	
	
	
	public Graph buildGraph() {
		Graph graph = new Graph();
		List<Node> nodes = new ArrayList<Node>();
		for(BaseNode bn : baseNodes) {
			Node node = new Node();
			node.setId(bn.getId());
			node.setnType(bn.getNodeType());
			
			if(bn.isTest()) {
				node.setTest(true);
				node.setIsActivityTest(1);   //本身就有API
				String perString=bn.getPermissions();
				for(Widget w : bn.getWidgets()) {  //控件isTest，则节点也是True
					if(w.isTest()) {
						perString+=w.getPermissions()+" ";
					}
					w.setWinID(bn.getId());
					w.setWinName(bn.getNodeType().toString()+bn.getId());
					if (w instanceof SubMenu) {
						List<MenuItem> tItems=((SubMenu) w).getItems();
						String itemIDString="";
						Iterator<MenuItem> itIterator=tItems.iterator();
						while (itIterator.hasNext()) {
							MenuItem menuItem = (MenuItem) itIterator.next();
							itemIDString+=menuItem.getItemId()+" ";
						}
						w.setItemIDString(itemIDString);
						w.setSubMenuID(((SubMenu) w).getSubMenuId());
					}
					if (w instanceof MenuItem) {
						w.setItemID(((MenuItem) w).getItemId());
					}
					graph.addWidget(w);
				}
				node.setPermissions(perString);
			}
			else {
				String perString="";
				for(Widget w : bn.getWidgets()) {  //控件isTest，则节点也是True ,但是 2模式
					if(w.isTest()) {
						node.setTest(true);
						node.setIsActivityTest(2);
						perString+=w.getPermissions()+" ";
					}
					w.setWinID(bn.getId());
					w.setWinName(bn.getNodeType().toString()+bn.getId());
					if (w instanceof SubMenu) {
						List<MenuItem> tItems=((SubMenu) w).getItems();
						String itemIDString="";
						Iterator<MenuItem> itIterator=tItems.iterator();
						while (itIterator.hasNext()) {
							MenuItem menuItem = (MenuItem) itIterator.next();
							itemIDString+=menuItem.getItemId()+" ";
						}
						w.setItemIDString(itemIDString);
						w.setSubMenuID(((SubMenu) w).getSubMenuId());
					}
					if (w instanceof MenuItem) {
						w.setItemID(((MenuItem) w).getItemId());
					}
					graph.addWidget(w);
					
				}
				node.setPermissions(perString);
			}
			node.setWidgets(bn.getWidgets());
			nodes.add(node);
			//graph.addAllWidgets(bn.getWidgets());  //添加所有的基础节点控件
		}
		// 碎片 或者 活动， 其实应该考虑 基本节点的，因为基本节点也属于 活动或者碎片，但是先省略,不考虑之间的关联
		for(FragNode fn : fragNodes.values()) {
			Node node = new Node();
			node.setId(fn.getId());
			node.setnType(fn.getNodeType());
			
			if(fn.isTest()) {
				node.setTest(true);
				node.setIsActivityTest(1);
				String perString=fn.getPermissions();
				for(Widget w : fn.getWidgets()) {  //控件isTest，则节点也是True
					if(w.isTest()) {
						perString+=w.getPermissions()+" ";
					}
					w.setWinID(fn.getId());
					w.setWinName(fn.getName());
					if (w instanceof SubMenu) {
						List<MenuItem> tItems=((SubMenu) w).getItems();
						String itemIDString="";
						Iterator<MenuItem> itIterator=tItems.iterator();
						while (itIterator.hasNext()) {
							MenuItem menuItem = (MenuItem) itIterator.next();
							itemIDString+=menuItem.getItemId()+" ";
						}
						w.setItemIDString(itemIDString);
						w.setSubMenuID(((SubMenu) w).getSubMenuId());
					}
					if (w instanceof MenuItem) {
						w.setItemID(((MenuItem) w).getItemId());
					}
					graph.addWidget(w);
				}
				node.setPermissions(perString);
			}
			else {
				String perString="";
				for(Widget w : fn.getWidgets()) {
					if(w.isTest()) {
						node.setTest(true);
						node.setIsActivityTest(2);
						perString+=w.getPermissions()+" ";
					}
					w.setWinID(fn.getId());
					w.setWinName(fn.getName());
					if (w instanceof SubMenu) {
						List<MenuItem> tItems=((SubMenu) w).getItems();
						String itemIDString="";
						Iterator<MenuItem> itIterator=tItems.iterator();
						while (itIterator.hasNext()) {
							MenuItem menuItem = (MenuItem) itIterator.next();
							itemIDString+=menuItem.getItemId()+" ";
						}
						w.setItemIDString(itemIDString);
						w.setSubMenuID(((SubMenu) w).getSubMenuId());
					}
					if (w instanceof MenuItem) {
						w.setItemID(((MenuItem) w).getItemId());
					}
					graph.addWidget(w);
				}
				node.setPermissions(perString);
			}
			node.setWidgets(fn.getWidgets());
			node.setName(fn.getName());
			if(fn.getContextMenu()!=null) {
				node.setContextMenu(fn.getContextMenu().getId());
			}
			if(fn.getOptionsMenu()!=null) {
				node.setOptionsMenu(fn.getOptionsMenu().getId());
			}
			node.setFragmentsName(fn.getFragmentsName());
			Iterator<String> fraNames=fn.getFragmentsName().iterator();
			String frgIDString="";
			while (fraNames.hasNext()) {
				String fraName = (String) fraNames.next();
				FragNode temp=getFragNodeByName(fraName);
				frgIDString+=temp.getId()+" ";
			}
			node.setFragIDString(frgIDString);
			nodes.add(node);
			//graph.addAllWidgets(fn.getWidgets());
		}
		for(ActNode an : actNodes.values()) {
			Node node = new Node();
			node.setId(an.getId());
			node.setnType(an.getNodeType());
			
			if(an.isTest()) {
				node.setTest(true);
				node.setIsActivityTest(1);
				String perString=an.getPermissions();
				for(Widget w : an.getWidgets()) {  //控件isTest，则节点也是True
					if(w.isTest()) {
						perString+=w.getPermissions()+" ";
					}
					w.setWinID(an.getId());
					w.setWinName(an.getName());
					if (w instanceof SubMenu) {
						List<MenuItem> tItems=((SubMenu) w).getItems();
						String itemIDString="";
						Iterator<MenuItem> itIterator=tItems.iterator();
						while (itIterator.hasNext()) {
							MenuItem menuItem = (MenuItem) itIterator.next();
							itemIDString+=menuItem.getItemId()+" ";
						}
						w.setItemIDString(itemIDString);
						w.setSubMenuID(((SubMenu) w).getSubMenuId());
					}
					if (w instanceof MenuItem) {
						w.setItemID(((MenuItem) w).getItemId());
					}
					graph.addWidget(w);
				}
				node.setPermissions(perString);
			}
			else {
				String perString="";
				for(Widget w : an.getWidgets()) {
					if(w.isTest()) {
						node.setTest(true);
						node.setIsActivityTest(2);
						perString+=w.getPermissions()+" ";
					}
					w.setWinID(an.getId());
					w.setWinName(an.getName());
					if (w instanceof SubMenu) {
						List<MenuItem> tItems=((SubMenu) w).getItems();
						String itemIDString="";
						Iterator<MenuItem> itIterator=tItems.iterator();
						while (itIterator.hasNext()) {
							MenuItem menuItem = (MenuItem) itIterator.next();
							itemIDString+=menuItem.getItemId()+" ";
						}
						w.setItemIDString(itemIDString);
						w.setSubMenuID(((SubMenu) w).getSubMenuId());
					}
					if (w instanceof MenuItem) {
						w.setItemID(((MenuItem) w).getItemId());
					}
					graph.addWidget(w);
					
				}
				node.setPermissions(perString);
			}
			node.setWidgets(an.getWidgets());
			node.setName(an.getName());
			if (an.getContextMenu()!=null) {
				node.setContextMenu(an.getContextMenu().getId());
			}
			if (an.getOptionsMenu()!=null) {
				node.setOptionsMenu(an.getOptionsMenu().getId());
			}
			if (an.getLeftDrawer()!=null) {
				node.setLeftDrawer(an.getLeftDrawer().getId());
			}
			if (an.getRightDrawer()!=null) {
				node.setRightDrawer(an.getRightDrawer().getId());
			}
			node.setFragmentsName(an.getFragmentsName());
			Iterator<String> fraNames=an.getFragmentsName().iterator();
			String frgIDString="";
			while (fraNames.hasNext()) {
				String fraName = (String) fraNames.next();
				FragNode temp=getFragNodeByName(fraName);
				frgIDString+=temp.getId()+" ";
			}
			node.setFragIDString(frgIDString);
			nodes.add(node);
			//graph.addAllWidgets(an.getWidgets());
		}
		graph.setNodes(nodes);  
		
		List<Edge> edges = new ArrayList<Edge>();
		for(Transition t : transitions) {
			Edge e = new Edge();
			e.setId(t.getId());
			e.setWidget(t.getWidget());
			e.setNote(t.getLabel());
			e.setSrc(getNodeById(nodes, t.getSrc()));
			if(t.getLabel() != null && t.getLabel().startsWith("MTN")) {
				String target = t.getLabel();
				int startIndex = target.indexOf("[");
				int endIndex = target.indexOf("]");
				target = target.substring(startIndex + 1, endIndex);
				Node tatN = graph.getNodeByName(target);
				if(tatN != null) {
					e.setTgt(tatN);
					e.setNote("");
				}
			}else {
				e.setTgt(getNodeById(nodes, t.getTgt()));
			}
			edges.add(e);
		}
		graph.setEdges(edges);
		return graph;
	}
	
	private Node getNodeById(List<Node> nodes, long id) {
		for(Node n : nodes) {
			if(n.getId() == id)
				return n;
		}
		return null;
	}
	
	
	
}
