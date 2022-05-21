package zzg.staticanalysis.generator.node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.AmbiguousMethodException;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.Manager;
import zzg.staticanalysis.RecourseMissingException;
import zzg.staticanalysis.staticTepOne;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.analyzer.AdapterAnalyzer;
//import zzg.staticanalysis.analyzer.Dataflow;
//import zzg.staticanalysis.analyzer.UnitAnalyzer;
import zzg.staticanalysis.dataflow.TargetFragmentFinder;
import zzg.staticanalysis.dataflow.WidgetFinder;
import zzg.staticanalysis.generator.node.menu.ContextMenuNodeGenerator;
import zzg.staticanalysis.generator.node.menu.DrawerNodeGenerator;
import zzg.staticanalysis.generator.node.menu.OptionsMenuNodeGenerator;
import zzg.staticanalysis.model.ActNode;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.MethodService;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.EventType;
import zzg.staticguimodel.Widget;

public class ActivityNodeGenerator {
	private static final String TAG = "[Generator-ActivityNode]";
	
	public static ActNode build(SootClass act) {
		Logger.i(TAG, "！！！！Start to generate ActivityNode ["+act.getName()+"]");
		return new ActivityNodeGenerator(act).generate();
	}
	
	private SootClass act;
	private ActNode actNode;
	
	private ActivityNodeGenerator(SootClass act) {
		this.act = act;
		this.actNode = new ActNode(IdProvider.v().nodeId(), act.getName());
	}
	
	private Set<String> fragmentClasses = new HashSet<String>();
	private Set<SootMethod> records = new HashSet<SootMethod>();
	protected ActNode generate() {
		//Record the control that will trigger events, and include the triggered events in the control.
		//Finally, analyze the control and its events to determine whether to generate an edge or test point
		SootMethod onCtreateMethod = null;
		try {
			onCtreateMethod = act.getMethodByNameUnsafe("onCreate");
		}catch (AmbiguousMethodException e) {
			onCtreateMethod = act.getMethodUnsafe("void onCreate(android.os.Bundle)");
		}
		if(onCtreateMethod == null) {
			SootClass superCls = act;
			while(superCls.hasSuperclass()) {
				superCls = superCls.getSuperclass();
				if(AppParser.v().isAppClass(superCls.getName())) {
					try {
						onCtreateMethod = superCls.getMethodByNameUnsafe("onCreate");
					}catch (AmbiguousMethodException e) {
						onCtreateMethod = superCls.getMethodUnsafe("void onCreate(android.os.Bundle)");
					}
					if(onCtreateMethod != null)
						break;
				}else
					break;
			}
		}
		if(onCtreateMethod != null) {
			handleOnCtreate(onCtreateMethod);  
			}
		System.out.println("TAG �� Number of analysis methods:"+act.getMethods().size());
		for(SootMethod sm : act.getMethods()) {
			String methodName = sm.getName();
			switch (methodName) {
				case "onCreateOptionsMenu"://OptionsMenu
					BaseNode optionsMenuNode = OptionsMenuNodeGenerator.build(sm);//An edge should be added here, but the control itself has no node. You can add items through dynamic code or XML
					actNode.setOptionsMenu(optionsMenuNode);
					continue;
				case "onCreateContextMenu"://ContextMenu
					BaseNode contextMenuNode = ContextMenuNodeGenerator.build(sm);
					actNode.setContextMenu(contextMenuNode);
					continue;
				default:
					handleOtherMethod(sm, null);
					continue;
			}
		}
		actNode.setFragmentsName(fragmentClasses);  
		Logger.i(TAG, "！！！！end to generate ActivityNode ["+act.getName()+"]");
		return actNode;
	}
	
	private void handleOnCtreate(SootMethod sm) {
		//handle layout widget
		String layoutName = getLayout(sm);
		if(layoutName != null) {
			Logger.i(TAG, "\tLayout: "+layoutName);
			//Gets the control that statically registers click events in the layout file
			List<Widget> widgets = AppParser.v().getEventWidget(layoutName);
			actNode.addAllWidgets(widgets);
			Logger.i(TAG, "Activity Statically registered widgets size: "+widgets.size());
			
			//Get and analyze the drawer. If there is a drawer, generate and add the drawernode to actnode
			List<Widget> leftDrawerItems = new ArrayList<Widget>();
			Widget leftDrawer = AppParser.v().getLeftDrawer(layoutName, leftDrawerItems);  //Find the drawer directly in the active XML (this is only one case), return the navigation control in the Darwin, and get the items contained in the navigation to the
			BaseNode leftDrawerNode = handleDrawer(leftDrawer, leftDrawerItems, layoutName);  //The left drawer node is obtained, and the edges of fragments reachable by its items are constructed
			actNode.setLeftDrawer(leftDrawerNode);
			//Find a way to get the fragments, RES / navigation that draweritems can reach
			//app:navGraph="@navigation/mobile_navigation"
			List<Widget> rightDrawerItems = new ArrayList<Widget>();
			Widget rightDrawer = AppParser.v().getRightDrawer(layoutName, rightDrawerItems);
			BaseNode rightDrawerNode = handleDrawer(rightDrawer, rightDrawerItems, layoutName);
			actNode.setRightDrawer(rightDrawerNode);
			if(leftDrawerNode != null || rightDrawerNode != null)
				Logger.i(TAG, "\tHas drawer: "+true);
			
			//Get the statically registered fragment
			Set<String> fragmentClassNames = AppParser.v().getStaticFragmentClassName(layoutName);  
			System.out.println("TAG: class"+act.getName()+"has "+fragmentClassNames.size()+" static <fragment/>");
			for(String fragClassName : fragmentClassNames) {
				SootClass cls = Scene.v().getSootClassUnsafe(fragClassName);
				if(cls != null) {
					fragmentClasses.add(cls.getName());
					System.out.println("TAG: fragmentName��"+cls.getName());
				}
			}
		}
	}
	
	private BaseNode handleDrawer(Widget drawer, List<Widget> drawerItems, String layoutName) {
		BaseNode drawerNode = null;
		if(drawer == null)
			return null;
		
		if(drawerItems.isEmpty())
			drawerNode = DrawerNodeGenerator.build(act, drawer, layoutName);
		else
			drawerNode = DrawerNodeGenerator.build(drawerItems, layoutName);  //Some edges are constructed from the navigation node to the edge of a fragment
		return drawerNode;
	}
	

	private void handleOtherMethod(SootMethod sm, SootMethod caller) {
		records.add(sm);
		Body body = null;
		try {
			body = sm.retrieveActiveBody();
		}catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body != null) {
			UnitGraph cfg = new BriefUnitGraph(body);
			Iterator<Unit> units = cfg.iterator();
			while(units.hasNext()) {
				Stmt stmt = (Stmt)units.next();
				if(stmt.containsInvokeExpr()) {
					InvokeExpr invokeExpr = stmt.getInvokeExpr();
					SootMethod invokee = invokeExpr.getMethod();
					String invokeeName = invokee.getName();
					
					//Callback registration for android.view.view
					if(MethodService.isViewCallbackRegister(invokee)) {
						if(invokeExpr instanceof VirtualInvokeExpr) {
							Logger.i(TAG, "Find a ViewCallbackRegister");
							EventType eventType = EventType.getEventType(invokeeName);
							String eventMethodName = Utils.getViewCallbackMethod(invokeeName);
							Value widgetValue = ((VirtualInvokeExpr) invokeExpr).getBase();//widget
//							Widget widget = Dataflow.getWidget(stmt, cfg, widgetValue);
							Widget widget = WidgetFinder.find(stmt, cfg, widgetValue, caller);
							if(widget != null) {
								widget.setEventMethod(eventMethodName);
								Value listenerValue = invokeExpr.getArg(0);
								String listenerClassName = listenerValue.getType().toQuotedString();
								SootClass listenerClass = Scene.v().getSootClassUnsafe(listenerClassName);
								if(listenerClass != null) {
									SootMethod eventMethod = null;
									try {
										eventMethod = listenerClass.getMethodByNameUnsafe(eventMethodName);
									}catch (AmbiguousMethodException e) {
										if(eventMethodName.equals("onClick")) {
											//eventMethod = listenerClass.getMethodUnsafe("void onClick(android.view.View)");
											eventMethod=listenerClass.getMethodByNameUnsafe("onClick");
										}
									}
									if(eventMethod != null) {
										widget.setEventHandler(eventMethod);
									}
								}
								widget.setEventType(eventType);
								Logger.i(TAG, "Successful to find a Widget");
								actNode.addWidget(widget);
							}
						}
					}
					
					//registerForContextMenu(android.view.View), long click open context-menu
					if(invokee.getName().equals("registerForContextMenu")) {
						Logger.i(TAG, "Find a ContextMenu Register");
						Value widgetValue = invokeExpr.getArg(0);
						//Here is an edge. Click this control to pop up the ContextMenu and find the ID and other information of the control
//						Widget widget = Dataflow.getWidget(stmt, cfg, widgetValue);
						Widget widget = WidgetFinder.find(stmt, cfg, widgetValue, caller);
						if(widget != null) {
							widget.setEventType(EventType.LONGCLICK);
							widget.setEventMethod(Utils.OPENCONTEXTMENU);
							actNode.addWidget(widget);
							Logger.i(TAG, "Successful to find a Widget with ContextMenu");
						}
					}
					
					//com.google.android.material.navigation.NavigationView::inflateMenu(int menuId)
					if(invokee.getDeclaringClass().getName().equals("com.google.android.material.navigation.NavigationView")
							&& invokee.getName().equals("inflateMenu")) {
						Value navigationViewValue = ((InstanceInvokeExpr)invokeExpr).getBase();
//						Widget navigationView = Dataflow.getWidget(stmt, cfg, navigationViewValue);
						Widget navigationView = WidgetFinder.find(stmt, cfg, navigationViewValue, caller);
						//In fact, the navigationview should be included in actnode.drawer.widget. Find the corresponding widget, and then find the corresponding drawernode according to the widget
						//find the Drawer Node
						BaseNode drawerNode = getDrawerNode(navigationView);
						new ArrayList<Widget>();
						Value menuIdValue = invokeExpr.getArg(0);
						if(menuIdValue instanceof IntConstant) {
							try {
								String menuLayoutName = AppParser.v().getMenuNameById(((IntConstant)menuIdValue).value);
								List<Widget> drawerMenuItems = AppParser.v().parseMenu(menuLayoutName);
								if(drawerNode != null) {
									drawerNode.addAllWidgets(drawerMenuItems);
								}
							}catch (Exception e) {
								Logger.e(TAG, e);
							}
						}
					}
					
					//Adapter
					if(invokee.getName().equals("setAdapter") || invokee.getName().equals("swapAdapter")) {
						AdapterAnalyzer.analysis(stmt, cfg, actNode, caller);
	//					UnitAnalyzer.handleAdapter(stmt, cfg, actNode, caller);
					}
					
					//fragment  
					if(MethodService.isFragmentTransaction(invokee)) {
						Logger.i(TAG, "Find a Fragment Transition");
//						fragmentClasses.addAll(UnitAnalyzer.getFragments(cfg, stmt, null));
						fragmentClasses.addAll(TargetFragmentFinder.find(cfg, stmt, caller));
					}
					
					//dialog Fragment
					if(MethodService.isDialogFragmentShow(invokee) && invokee instanceof InstanceInvokeExpr) {
						Value dialogFragmentValue = ((InstanceInvokeExpr)invokeExpr).getBase();
						String dialogFragmentClassName = dialogFragmentValue.getType().toQuotedString();
						SootClass dialogFragmentClass = Scene.v().getSootClassUnsafe(dialogFragmentClassName);
						if(dialogFragmentClass != null) {
							fragmentClasses.add(dialogFragmentClassName);
						}
					}
					
					String tagPermissionString=staticTepOne.v().isTestingPoint(stmt,body);
					if (!tagPermissionString.equals("00")) {
						actNode.setTest(true);
						//print & record
						if (!tagPermissionString.equals("01")) {
							actNode.setPermissions(tagPermissionString);
						}
						Utils.buildTestPointMsg(sm, invokee);
					}					
					//invoke an other method, if it's app method and in other class, parse it
					if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName())
							&& !records.contains(invokee)) {
						handleOtherMethod(invokee, sm);
					}
				}
			}
		}
	}

	private BaseNode getDrawerNode(Widget navigationView) {
		BaseNode drawer = actNode.getLeftDrawer();
		if(drawer != null) {
			List<Widget> drawerItems = drawer.getWidgets();
			for(Widget item : drawerItems) {
				if(item.getResId() == navigationView.getResId()) {
					return drawer;
				}
			}
		}
		drawer = actNode.getRightDrawer();
		if(drawer != null) {
			List<Widget> drawerItems = drawer.getWidgets();
			for(Widget item : drawerItems) {
				if(item.getResId() == navigationView.getResId()) {
					return drawer;
				}
			}
		}
		return null;
	}

	private String getLayout(SootMethod onCreate) {
		String layout = null;
		Body body = null;
		try {
			body = onCreate.retrieveActiveBody();
		}catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body == null)
			return null;
		UnitGraph cfg = new BriefUnitGraph(body);
		Iterator<Unit> units = cfg.iterator();
		while(units.hasNext()) {
			Stmt stmt = (Stmt)units.next();
			if(stmt.containsInvokeExpr()) {
				InvokeExpr invokeExpr = stmt.getInvokeExpr();
				if(invokeExpr.getMethod().getName().equals("setContentView")) {
					Value layoutValue = invokeExpr.getArg(0);
					if(layoutValue instanceof IntConstant) {
						try {
							layout = AppParser.v().getLayoutNameById(((IntConstant)layoutValue).value);
						} catch (RecourseMissingException e) {
							Logger.e(TAG, e);
						}
						break;
					}
				}
			}
		}
		return layout;
	}
	
	
}
