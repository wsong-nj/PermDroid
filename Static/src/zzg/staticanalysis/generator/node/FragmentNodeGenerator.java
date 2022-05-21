package zzg.staticanalysis.generator.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.SysexMessage;

import soot.AmbiguousMethodException;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.RecourseMissingException;
import zzg.staticanalysis.staticTepOne;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.analyzer.AdapterAnalyzer;
//import zzg.staticanalysis.analyzer.Dataflow;
//import zzg.staticanalysis.analyzer.UnitAnalyzer;
import zzg.staticanalysis.dataflow.TargetFragmentFinder;
import zzg.staticanalysis.dataflow.WidgetFinder;
import zzg.staticanalysis.generator.node.menu.ContextMenuNodeGenerator;
import zzg.staticanalysis.generator.node.menu.OptionsMenuNodeGenerator;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.model.FragNode;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.MethodService;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.EventType;
import zzg.staticguimodel.Widget;

public class FragmentNodeGenerator {
	private static final String TAG = "[Generator-FragmentNode]";
	
	public static void build(SootClass frag) {
		Logger.i(TAG, "¡ª¡ª¡ª¡ªStart to generate FragmentNode ["+frag.getName()+"]");
		new FragmentNodeGenerator(frag).generate();
	}
	
	private SootClass frag;
	private FragNode fragNode;
	
	private FragmentNodeGenerator(SootClass frag) {
		this.frag = frag;
		this.fragNode = new FragNode(IdProvider.v().nodeId(), frag.getName());
	}

	private Set<String> fragmentClasses = new HashSet<String>();
	private Set<SootMethod> records = new HashSet<SootMethod>();
	protected void generate() {
		if(!isDialogFragment(frag)) {
			//SootMethod onCtreateViewMethod = frag.getMethodUnsafe("android.view.View onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)");
			SootMethod onCtreateViewMethod = frag.getMethodByNameUnsafe("onCreateView");
			if(onCtreateViewMethod == null) {
				SootClass superCls = frag;  
				while(superCls.hasSuperclass()) {
					superCls = superCls.getSuperclass();
					if(AppParser.v().isAppClass(superCls.getName())) {
						//onCtreateViewMethod = superCls.getMethodUnsafe("android.view.View onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)");
						onCtreateViewMethod = frag.getMethodByNameUnsafe("onCreateView");
						if(onCtreateViewMethod != null) 
							break;
					}else 
						break;
				}
			}
			if(onCtreateViewMethod != null) {
				handleOnCreateView(onCtreateViewMethod);  
			}else {
				System.out.println("con't find fragment's nCreateView method");
			}
			
			try {
				SootMethod onCtreateMethod = frag.getMethodByNameUnsafe("onCreate");
				/*
				 * <b>here is important
				 * <b>
				 * <b>
				 * <b>
				 */
				if(onCtreateMethod != null) {
					records.add(onCtreateMethod);
					boolean hasOptionsMenu = hasOptionsMenu(onCtreateMethod);
					if(hasOptionsMenu) {
						SootMethod onCreateOptionsMenuMethod = frag.getMethodByNameUnsafe("onCreateOptionsMenu");
						if(onCreateOptionsMenuMethod != null) {
							records.add(onCreateOptionsMenuMethod);
							BaseNode optionsMenuNode = OptionsMenuNodeGenerator.build(onCreateOptionsMenuMethod);
							fragNode.setOptionsMenu(optionsMenuNode);
						}
					}
					
					//for PreferenceFragment
					//find setOnPreferenceClickListener
					if(isPreferenceFragment()) {
						Logger.i(TAG, "is PreferenceFragment");
						handleOnCreate(onCtreateMethod, null);
					}
				}
				/*
				 * for ListFragment
				 * <b>void onListItemClick(android.widget.ListView,android.view.View,int,long)
				 * <b>::setListAdapter(ListAdapter adapter) 
				 * <b>in ListFragment's layout.xml file£¬you must specify one android:id is ¡°@android:id/list¡± for ListView widget
				 * <b>To do this, your view hierarchy must contain a ListView object with the id "@android:id/list" 
				 * <b>(or R.id.list if it's in code) 
				 * <b>To call setlistadapter in oncreate and complete the binding of adapter
				 */
				if(isListFragment()) {
					Logger.i(TAG, "is ListFragment");
					Widget w1 = getListView(onCtreateMethod, null, new HashSet<SootMethod>());
					if(w1 != null) {
						//get the onListItemClick() method, set in widget
						try {
							SootMethod onListItemClick = frag.getMethodByNameUnsafe("onListItemClick");
							w1.setEventHandler(onListItemClick);
							w1.setEventType(EventType.CLICK);
							w1.setEventMethod("onListItemClick");
						} catch (RuntimeException e) {
							Logger.e(TAG, e);
						}
						fragNode.addWidget(w1);
					}
				}
			}catch (RuntimeException e) {
				Logger.e(TAG, e);
			}
			
			SootMethod onCreateContextMenuMethod = frag.getMethodByNameUnsafe("onCreateContextMenu");
			if(onCreateContextMenuMethod != null) {
				records.add(onCreateContextMenuMethod);
				BaseNode contextMenuNode = ContextMenuNodeGenerator.build(onCreateContextMenuMethod);
				fragNode.setContextMenu(contextMenuNode);
			}
		}else {
			Logger.i(TAG, "is DialogFragment");
			//analyse whether android.app.Dialog onCreateDialog(android.os.Bundle)rewrites Dialog
			//Only the custom dialog created by alterdialog. Builder is analyzed
			try {
				SootMethod onCreateDialog = frag.getMethodByNameUnsafe("onCreateDialog");
				records.add(onCreateDialog);
//				UnitAnalyzer.handleAlertDialog(onCreateDialog, fragNode);
				try {
					Body body = onCreateDialog.retrieveActiveBody();
					UnitGraph cfg = new BriefUnitGraph(body);
					DialogNodeGenerator.build(null, cfg, null, fragNode);
				}catch (ActiveBodyNotFoundException e) {
					Logger.e(TAG, new RuntimeException("Method onCreateDialog[DialogFragment] haven't ActiveBody!", e));
				}
			}catch (RuntimeException e) {
				Logger.e(TAG, e);
			}
		}
		
		
		for(SootMethod sm : frag.getMethods()) {
			if(sm.getName().equals("onCreateDialog") 
					|| sm.getName().equals("onCreateOptionsMenu")
					|| sm.getName().equals("onCreateContextMenu")
					|| sm.getName().equals("onCreate"))
				continue;
			if(records.contains(sm))
				continue;
			handleOtherMethod(sm, null);
		}
		
		
		//public void onPrepareOptionsMenu (Menu menu) dynamic modification OptionsMenu£¨add¡¢remove£©
		fragNode.setFragmentsName(fragmentClasses);
	}
	
	private void handleOnCreate(SootMethod onCtreateMethod, SootMethod caller) {
		//Find the method addpreferencesfromresource (int preferencesresid), find the resource ID of the preferences file, and then find the XML resource
		Map<String, String> key_title = new HashMap<String, String>();
		String preferenceFileName = getPreferenceRes(onCtreateMethod);   //Get the XML file name,   addPreferencesFromResource(R.xml.pref_general);
		if(preferenceFileName != null) {
			//Parse the preferences file, and android:key and android:title Correspondingly, the former is in the code, and the latter is the text displayed by the control, which can be found according to the text (the latter may be ID, and string can be found through ID)
			key_title = AppParser.v().parsePreferenceFile(preferenceFileName);
		}
		
		Body body = null;
		try {
			body = onCtreateMethod.retrieveActiveBody();
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
					
					//OnPreferenceClickListener
					if(invokeeName.equals("setOnPreferenceClickListener")) {
						if(invokeExpr instanceof VirtualInvokeExpr) {
							Logger.i(TAG, "Find a PreferenceClick Register");
							EventType eventType = EventType.CLICK;
							String eventMethodName = "onPreferenceClick";
							Value preferenceValue = ((VirtualInvokeExpr) invokeExpr).getBase();
							String preferenceKey = getPreferenceKey(stmt, cfg, preferenceValue);
							if(preferenceKey != null) {
								String text = key_title.get(preferenceKey);
								if(text != null) {
									Widget widget = new Widget();
									widget.setId(IdProvider.v().widgetId());
									widget.setText(text);
									widget.setEventMethod(eventMethodName);
		
									Value listenerValue = invokeExpr.getArg(0);
									String listenerClassName = listenerValue.getType().toQuotedString();
									SootClass listenerClass = Scene.v().getSootClassUnsafe(listenerClassName);
									if(listenerClass != null) {
										SootMethod eventMethod = listenerClass.getMethodByNameUnsafe(eventMethodName);
										if(eventMethod != null) {
											widget.setEventHandler(eventMethod);
										}
									}
									widget.setEventType(eventType);
									Logger.i(TAG, "Successful to find a Preference(Widget)");
									fragNode.addWidget(widget);
								}
							}
						}
					}
					
					//Callback registration for android.view.View 
					if(MethodService.isViewCallbackRegister(invokee)) {
						if(invokeExpr instanceof VirtualInvokeExpr) {
							Logger.i(TAG, "Find a ViewCallback Register");
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
									SootMethod eventMethod = listenerClass.getMethodByNameUnsafe(eventMethodName);
									if(eventMethod != null) {
										widget.setEventHandler(eventMethod);
									}
								}
								widget.setEventType(eventType);
								Logger.i(TAG, "Successful to find a Widget");
								fragNode.addWidget(widget);
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
							fragNode.addWidget(widget);
							Logger.i(TAG, "Successful to find a Widget with ContextMenu");
						}
					}
					//Adapter
					if(invokee.getName().equals("setAdapter") || invokee.getName().equals("swapAdapter")) {
						AdapterAnalyzer.analysis(stmt, cfg, fragNode, caller);
//						UnitAnalyzer.handleAdapter(stmt, cfg, fragNode, null);
					}
					
					if(MethodService.isFragmentTransaction(invokee)) {
//						fragmentClasses.addAll(UnitAnalyzer.getFragments(cfg, stmt, null));
						fragmentClasses.addAll(TargetFragmentFinder.find(cfg, stmt, null));
					}
					
					if(MethodService.isDialogFragmentShow(invokee) && invokee instanceof InstanceInvokeExpr) {
						Value dialogFragmentValue = ((InstanceInvokeExpr)invokeExpr).getBase();
						String dialogFragmentClassName = dialogFragmentValue.getType().toQuotedString();
						SootClass dialogFragmentClass = Scene.v().getSootClassUnsafe(dialogFragmentClassName);
						if(dialogFragmentClass != null) {
							fragmentClasses.add(dialogFragmentClassName);
						}
					}
					
					String tagPermissionString=staticTepOne.v().isTestingPoint(stmt,body);  // 00:Not the target API , 01:Unable to parse   other £ºpermissions
					if (!tagPermissionString.equals("00")) {
						fragNode.setTest(true);
						if (!tagPermissionString.equals("01")) {
							fragNode.setPermissions(tagPermissionString);
						}
						//  class:  method:  API signature:
						Utils.buildTestPointMsg(onCtreateMethod, invokee);
					}
					
					//invoke an other method, if it's app method and in other class, parse it
//					if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName())   
//							&& !invokee.getDeclaringClass().getName().equals(onCtreateMethod.getDeclaringClass().getName())) {
//						handleOtherMethod(invokee, onCtreateMethod);
//					}
					if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName())) {
						handleOtherMethod(invokee, onCtreateMethod);
					}
				}
			}
		}
		
	}

	private void handleOnCreateView(SootMethod onCtreateViewMethod) {
		String layoutName = getLayout(onCtreateViewMethod);
		if(layoutName != null) {
			//get widgets that is found in  XML  by "onClick" tag.
			List<Widget> widgets = AppParser.v().getEventWidget(layoutName);
			
			
			fragNode.addAllWidgets(widgets);
		}
	}

	// setHasOptionsMenu() was called?
	//is ListFragment£¬setListAdapter()was called?
	private boolean hasOptionsMenu(SootMethod onCtreateMethod) {
		Body body = null;
		try {
			body = onCtreateMethod.retrieveActiveBody();
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
					if(invokee.getName().equals("setHasOptionsMenu")) {
						Value value = invokeExpr.getArg(0);
						if(value.toString().equals("0"))
							return false;
						else
							return true;
					}
				}
			}
		}
		return false;
	}

	private Widget getListView(SootMethod onCtreateMethod, SootMethod caller, Set<SootMethod> records) {
		records.add(onCtreateMethod);
		Body body = null;
		try {
			body = onCtreateMethod.retrieveActiveBody();
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
					if(invokee.getName().equals("setListAdapter")) {
						
						try {
							Value adapterValue = invokeExpr.getArg(0);
							String adapterClassName = adapterValue.getType().toQuotedString();
							SootClass adapterClass = Scene.v().getSootClass(adapterClassName);
							//android.view.View	getView(int,android.view.View,android.view.ViewGroup)
							SootMethod getView = adapterClass.getMethod("android.view.View getView(int,android.view.View,android.view.ViewGroup)");
							Widget listView = new Widget();
							listView.setId(IdProvider.v().widgetId());
							listView.setResName("list");
							listView.setResId(16908298);
							listView.setType("android.widget.ListView");
//							UnitAnalyzer.analysisAdapter(getView, listView, fragNode, null);
							AdapterAnalyzer.analysisEvent(getView, listView, fragNode, null);
							return listView;
						}catch (RuntimeException e) {
							Logger.e(TAG, e);
						}
						return null;
					}
					if(AppParser.v().isAppClass(invokee.getName()) && !records.contains(invokee)) {
						Widget listView = getListView(invokee, onCtreateMethod, records);
						if(listView != null)
							return listView;
					}
				}
			}
		}
		return null;
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
					
					//Callback registration for android.view.View 
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
								fragNode.addWidget(widget);
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
							fragNode.addWidget(widget);
							Logger.i(TAG, "Successful to find a Widget with ContextMenu");
						}
					}
					//Adapter
					if(invokee.getName().equals("setAdapter") || invokee.getName().equals("swapAdapter")) {
						AdapterAnalyzer.analysis(stmt, cfg, fragNode, caller);
//						UnitAnalyzer.handleAdapter(stmt, cfg, fragNode, null);
					}
					
					if(MethodService.isFragmentTransaction(invokee)) {
						Logger.i(TAG, "Find a Fragment Transition");
//						fragmentClasses.addAll(UnitAnalyzer.getFragments(cfg, stmt, null));
						fragmentClasses.addAll(TargetFragmentFinder.find(cfg, stmt, null));
					}
					
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
						fragNode.setTest(true);
						if (!tagPermissionString.equals("01")) {
							fragNode.setPermissions(tagPermissionString);
						}
						//print & record
						Utils.buildTestPointMsg(sm, invokee);
					}
//					if(MethodService.isImageAPI(invokee)) {
//						fragNode.setTest(true);
//						//print & record
//						Utils.buildTestPointMsg(sm, invokee);
//					}
					
//					if(ClassService.isAlertDialogBuilder(invokee.getDeclaringClass()) 
//							&& (invokee.getName().equals("show") || invokee.getName().equals("create"))) {
//						UnitAnalyzer.handleAlertDialog(sm, fragNode, app);
//					}
					
					//invoke an other method, if it's app method and in other class, parse it
					if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
							//&& !invokee.getDeclaringClass().getName().equals(sm.getDeclaringClass().getName())  ÎÒ¼ÓµÄ
							&& !records.contains(invokee)) {
						handleOtherMethod(invokee, sm);
					}
				}
			}
		}
	}
	
	private String getLayout(SootMethod onCtreateViewMethod) {
		String layout = null;
		Body body = null;
		try {
			body = onCtreateViewMethod.retrieveActiveBody();
		}catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body == null)
			return null;
		UnitGraph cfg = new BriefUnitGraph(body);  //sootmethod->body->unitGraph->unit->stmt->invokeExpr
		Iterator<Unit> units = cfg.iterator();
		while(units.hasNext()) {
			Stmt stmt = (Stmt)units.next();
			if(stmt.containsInvokeExpr()) {
				InvokeExpr invokeExpr = stmt.getInvokeExpr();
				String invokeeName = invokeExpr.getMethod().getName();
				String invokeeClassName = invokeExpr.getMethod().getDeclaringClass().getName();  
				//inflater.inflate(R.layout.afragment, container, false);   inflater.inflate(R.layout.fragment_first2, null);  But some call the parent class:super.onCreateView
				if(invokeeName.equals("inflate") && invokeeClassName.equals("android.view.LayoutInflater")) {
					Value layoutValue = invokeExpr.getArg(0);
					if(layoutValue instanceof IntConstant) {
						try {
							layout = AppParser.v().getLayoutNameById(((IntConstant)layoutValue).value);
							System.out.println("TAG Find the fragmented layout file:"+layout);
						} catch (RecourseMissingException e) {
							Logger.e(TAG, e);
						}
						break;
					}
					
				}
				// I added, dealing with exceptions     But some call the parent class :super.onCreateView()   "android.view.View onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)"
				/**SootClass superCls = act;
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
			}*/
				
				/**
				 *sootclass sc= onCreateView.getClass();
				 *if(sc.hasSuperClass()){
				 *sc=sc.getsuperclass;
				 *sootmethod sm=
				 *}
				 * */
				if(invokeeName.equals("onCreateView") ) {
					System.out.println("TAG  super.onCreateView() ");
					System.out.println("TAG: method£º"+onCtreateViewMethod.getName());
					SootClass sClass=onCtreateViewMethod.getDeclaringClass();
					System.out.println("TAG: class£º"+sClass.getName());
					if(sClass.hasSuperclass()) {
						sClass=sClass.getSuperclass();
						System.out.println("TAG: superClass£º"+sClass.getName());
					}
					if(AppParser.v().getAllClasses().contains(sClass)) {
						System.out.println("TAG  super class fragment  ");
						SootMethod s = sClass.getMethodByNameUnsafe("onCreateView");
						if (s != null) {
							System.out.println("super class has onCreateView ");
							getLayout(s);
						} else {
							System.out.println("TAG There is no oncreateview method in the fragment parent class");

						}
					}
							
				}
				
			}
		}
		return layout;
	}
	
	private String getPreferenceRes(SootMethod onCreate) {
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
				if(invokeExpr.getMethod().getName().equals("addPreferencesFromResource")) {
					Value layoutValue = invokeExpr.getArg(0);
					if(layoutValue instanceof IntConstant) {
						try {
							return AppParser.v().getXmlNameById(((IntConstant)layoutValue).value);
						} catch (RecourseMissingException e) {
							Logger.e(TAG, e);
						}
						break;
					}
				}
			}
		}
		return null;
	}
	
	
	private String getPreferenceKey(Stmt stmt, UnitGraph cfg, Value preferenceValue) {
		SootMethod sm = cfg.getBody().getMethod();
		Stmt curStmt = stmt, dataflowStmt = stmt;
		Value curValue = preferenceValue;
		while(!cfg.getPredsOf(curStmt).isEmpty()) {
			List<Unit> preStmts = cfg.getPredsOf(curStmt);
			curStmt = (Stmt)preStmts.get(0);
			if(curStmt instanceof AssignStmt) {
				AssignStmt curAssignStmt = (AssignStmt)curStmt;
				Value left = curAssignStmt.getLeftOp();
				if(left.equivTo(curValue)) {
					dataflowStmt = curAssignStmt;
					Value right = curAssignStmt.getRightOp();
					if(right instanceof CastExpr) {
						CastExpr castExpr = (CastExpr)right;
						curValue = castExpr.getOp();
					}else {
						curValue = right;
					}
					if(right instanceof VirtualInvokeExpr) {
						VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr)right;
						SootMethod rMethod = virtualInvokeExpr.getMethod();
						if(rMethod.getName().equals("findPreference")) {
							Value arg0 = virtualInvokeExpr.getArg(0);
							if(arg0 instanceof StringConstant) {
								return ((StringConstant) arg0).value;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	
	private boolean isListFragment() {
		SootClass subClass = frag;
        while(subClass.hasSuperclass()) {
        	subClass = subClass.getSuperclass();
        	String subClassName = subClass.getName();
        	if(subClassName.equals("android.app.ListFragment")
        			|| subClassName.equals("androidx.fragment.app.ListFragment")
        			|| subClassName.equals("android.support.v4.app.ListFragment")) {
        		return true;
        	}
        }
        return false;
	}
	
	private boolean isPreferenceFragment() {
		SootClass subClass = frag;
        while(subClass.hasSuperclass()) {
        	subClass = subClass.getSuperclass();
        	String subClassName = subClass.getName();
        	if(subClassName.equals("androidx.preference.PreferenceFragment")
        			|| subClassName.equals("android.preference.PreferenceFragment")
        			|| subClassName.equals("android.support.v14.preference.PreferenceFragment")
        			|| subClassName.equals("androidx.preference.PreferenceFragmentCompat")) {
        		return true;
        	}
        }
        return false;
	}
	
	private boolean isDialogFragment(SootClass sc) {
		SootClass subClass = sc;
        while(subClass.hasSuperclass()) {
        	subClass = subClass.getSuperclass();
        	String subClassName = subClass.getName();
        	if(subClassName.equals("android.app.DialogFragment")
        			|| subClassName.equals("androidx.fragment.app.DialogFragment")
        			|| subClassName.equals("android.support.v4.app.DialogFragment")) {
        		return true;
        	}
        }
        return false;
	}
}
