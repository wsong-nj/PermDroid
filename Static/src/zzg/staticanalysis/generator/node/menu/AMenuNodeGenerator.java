package zzg.staticanalysis.generator.node.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.RuntimeErrorException;

import com.sun.xml.fastinfoset.util.ContiguousCharArrayArray;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.EqExpr;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.internal.AbstractSwitchStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JTableSwitchStmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.RecourseMissingException;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.analyzer.EventAnalyzer;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticguimodel.MenuItem;
import zzg.staticguimodel.NodeType;
import zzg.staticguimodel.SubMenu;
import zzg.staticguimodel.Widget;

public abstract class AMenuNodeGenerator {
	protected static final String TAG = "[Generator-Menu]";
	private SootMethod sm;
	private BaseNode menuNode;
	
	protected AMenuNodeGenerator() {}
	
	protected AMenuNodeGenerator(SootMethod sm) {
		this.sm = sm;
		this.menuNode = new BaseNode(IdProvider.v().nodeId(), NodeType.OPTIONSMENU);
	}

	protected BaseNode generate() {
		Value menuValue = getMenuValue(sm, 0);
		
		buildMenuNode(sm, false, menuValue);
		
		SootClass sc = sm.getDeclaringClass();
		SootMethod onItemSelected = null;
		if(sm.getName().equals("onCreateOptionsMenu")) {
			onItemSelected = sc.getMethodByNameUnsafe("onOptionsItemSelected");
		}else if(sm.getName().equals("onCreateContextMenu")) {
			onItemSelected = sc.getMethodByNameUnsafe("onContextItemSelected");
		}
		if(onItemSelected != null) {
			analysisItemsEventMethod(onItemSelected, menuNode);
		}
		
		return menuNode;
	}

	protected Value getMenuValue(SootMethod sm, int position) {
		Body body = null;
		try {
			body = sm.retrieveActiveBody();
		} catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body != null) {
			for(Unit s : body.getUnits()) {
				if (s instanceof IdentityStmt && ((IdentityStmt) s).getRightOp() instanceof ParameterRef) {
			        IdentityStmt is = (IdentityStmt) s;
			        ParameterRef pr = (ParameterRef) is.getRightOp();
			        if (pr.getIndex() == position) {
			        	return is.getLeftOp();
			        }
				}
			}
		}
		return null;
	}

	private Set<SootMethod> records = new HashSet<SootMethod>();
	private void buildMenuNode(SootMethod sm, boolean hasMenuWidget, Value menuValue) {
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
					SootMethod invokee = stmt.getInvokeExpr().getMethod();
					String signature = invokee.getSignature();
					switch (signature) {
						case "<android.view.MenuInflater: void inflate(int,android.view.Menu)>":
							Logger.i(TAG, "\tFind the MenuInflater...");
							Value arg0 = stmt.getInvokeExpr().getArg(0);
							if(arg0 instanceof IntConstant) {
								int layoutId = ((IntConstant)arg0).value;
								Logger.i(TAG, "\t\tLayout Id: " + layoutId);
								try {
									String menuLayoutName = AppParser.v().getMenuNameById(layoutId);
									Logger.i(TAG, "\t\tLayout: " + menuLayoutName);
									List<Widget> mItems = AppParser.v().parseMenu(menuLayoutName);
									Logger.i(TAG, "\t\tItems size: " + mItems.size());
									menuNode.addAllWidgets(mItems);
								} catch (RecourseMissingException e) {
									Logger.e(TAG, e);
								}
							}
							continue;
						case "<android.view.Menu: android.view.MenuItem add(int,int,int,int)>":
							Logger.i(TAG, "\tFind the MenuItem adder...");
							hasMenuWidget = true;
							MenuItem menuItem = new MenuItem();
							menuItem.setId(IdProvider.v().widgetId());
							Value id = stmt.getInvokeExpr().getArg(1);
							if(id instanceof IntConstant) {
								Logger.i(TAG, "\t\tItem id: " + ((IntConstant)id).value);
								menuItem.setItemId(((IntConstant)id).value);
							}
							Value textId = stmt.getInvokeExpr().getArg(3);
							if(textId instanceof IntConstant) {
								Logger.i(TAG, "\t\tItem textId: " + ((IntConstant)textId).value);
								try {
									String text = AppParser.v().getStringById(((IntConstant)textId).value);
									Logger.i(TAG, "\t\tItem text: " + (text == null? "null" : text));
									menuItem.setText(text);
								} catch (RecourseMissingException e) {
									Logger.e(TAG, e);
								}
							}
							menuNode.addWidget(menuItem);
							continue;
						case "<android.view.Menu: android.view.MenuItem add(int,int,int,java.lang.CharSequence)>":
							Logger.i(TAG, "\tFind the MenuItem adder...");
							hasMenuWidget = true;
							MenuItem menuItem1 = new MenuItem();
							menuItem1.setId(IdProvider.v().widgetId());
							Value id1 = stmt.getInvokeExpr().getArg(1);
							if(id1 instanceof IntConstant) {
								Logger.i(TAG, "\t\tItem id: " + ((IntConstant)id1).value);
								menuItem1.setItemId(((IntConstant)id1).value);
							}
							String text = stmt.getInvokeExpr().getArg(3).toString();
							Logger.i(TAG, "\t\tItem text: " + (text == null? "null" : text));
							menuItem1.setText(text);
							menuNode.addWidget(menuItem1);
							continue;
						case "<android.view.Menu: android.view.SubMenu addSubMenu(int,int,int,int)>":
							Logger.i(TAG, "\tFind the SubMenu adder...");
							hasMenuWidget = true;
							SubMenu subMenu = new SubMenu();
                            subMenu.setId(IdProvider.v().widgetId());
                            Value id2 = stmt.getInvokeExpr().getArg(1);
							if(id2 instanceof IntConstant) {
								Logger.i(TAG, "\t\tItem id: " + ((IntConstant)id2).value);
								subMenu.setSubMenuId(((IntConstant)id2).value);
							}
							Value textId1 = stmt.getInvokeExpr().getArg(3);
							if(textId1 instanceof IntConstant) {
								Logger.i(TAG, "\t\tItem textId: " + ((IntConstant)textId1).value);
								try {
									String text1 = AppParser.v().getStringById(((IntConstant)textId1).value);
									Logger.i(TAG, "\t\tItem text: " + (text1 == null? "null" : text1));
									subMenu.setText(text1);
								} catch (RecourseMissingException e) {
									Logger.e(TAG, e);
								}
							}
                            List<MenuItem> items = getSubItems(stmt, cfg);
                            subMenu.setItems(items);
                            menuNode.addWidget(subMenu);
							continue;
						case "<android.view.Menu: android.view.SubMenu addSubMenu(int,int,int,java.lang.CharSequence)>":
							Logger.i(TAG, "\tFind the SubMenu adder...");
							hasMenuWidget = true;
							SubMenu subMenu1 = new SubMenu();
                            subMenu1.setId(IdProvider.v().widgetId());
                            Value id3 = stmt.getInvokeExpr().getArg(1);
							if(id3 instanceof IntConstant) {
								Logger.i(TAG, "\t\tItem id: " + ((IntConstant)id3).value);
								subMenu1.setSubMenuId(((IntConstant)id3).value);
							}
							String text1 = stmt.getInvokeExpr().getArg(3).toString();
							Logger.i(TAG, "\t\tItem text: " + (text1 == null? "null" : text1));
							subMenu1.setText(text1);
							List<MenuItem> items1 = getSubItems(stmt, cfg);
                            subMenu1.setItems(items1);
                            menuNode.addWidget(subMenu1);
							continue;
						default:
							break;
					}
					if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName())
							&& !records.contains(invokee)) {
						for(int i = 0; i < stmt.getInvokeExpr().getArgCount(); i++) {
							Value arg = stmt.getInvokeExpr().getArg(i);
							if(arg.equivTo(menuValue)) {
								Value curMenuValue = getMenuValue(invokee, i);
								buildMenuNode(invokee, hasMenuWidget, curMenuValue);
								break;
							}
						}
					}
				}
			}
//			if(!hasMenuWidget) {
//				Iterator<Unit> units1 = cfg.iterator();
//                while (units1.hasNext()) {
//                    Stmt stmt = (Stmt) units1.next();
//                    if (stmt.containsInvokeExpr()) {
//                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
//                        if (invokeExpr.getArgCount() == 1) {
//                            Value arg = invokeExpr.getArg(0);
//                            if (arg.getType().toString().equals("android.view.Menu")) {
//                                SootMethod m = invokeExpr.getMethod();
//                                buildMenuNode(m, hasMenuWidget);
//                            }
//                        }
//                    }
//                }
//			}
		}
	}

	private List<MenuItem> getSubItems(Stmt stmt, UnitGraph cfg) {
		List<MenuItem> items = new ArrayList<MenuItem>();
		if (stmt instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value left = assignStmt.getLeftOp();
            if (left.getType().toString().equals("android.view.SubMenu")) {
                Stmt curStmt = assignStmt;
                Set<Stmt> stmtRecords = new HashSet<Stmt>();
                while (!cfg.getSuccsOf(curStmt).isEmpty()) {
                    curStmt = (Stmt) cfg.getSuccsOf(curStmt).get(0);
                    if(stmtRecords.add(curStmt)) {
	                    if (curStmt.containsInvokeExpr()) {
	                        InvokeExpr curInvokeExpr = curStmt.getInvokeExpr();
	                        if (curInvokeExpr instanceof InterfaceInvokeExpr) {
	                            InterfaceInvokeExpr interfaceInvokeExpr = (InterfaceInvokeExpr) curInvokeExpr;
	                            Value invokeObj = interfaceInvokeExpr.getBase();
	                            SootMethod interfaceInvokeMethod = interfaceInvokeExpr.getMethod();
	                            String interfaceMethodSign = interfaceInvokeMethod.getSignature();
	                            if (left.equivTo(invokeObj)) {
	                            	switch (interfaceMethodSign) {
										case "<android.view.SubMenu: android.view.MenuItem add(int,int,int,java.lang.CharSequence)>":
											MenuItem item = new MenuItem();
											item.setId(IdProvider.v().widgetId());
											Value id = interfaceInvokeExpr.getArg(1);
											if(id instanceof IntConstant) {
												item.setResId(((IntConstant)id).value);
											}
											String text = interfaceInvokeExpr.getArg(3).toString();
											item.setText(text);
											items.add(item);
											continue;
										case "<android.view.SubMenu: android.view.MenuItem add(int,int,int,int)>":
											MenuItem item1 = new MenuItem();
											item1.setId(IdProvider.v().widgetId());
											Value id1 = interfaceInvokeExpr.getArg(1);
											if(id1 instanceof IntConstant) {
												item1.setResId(((IntConstant)id1).value);
											}
											Value textId = interfaceInvokeExpr.getArg(3);
											if(textId instanceof IntConstant) {
												try {
													String text1 = AppParser.v().getStringById(((IntConstant)textId).value);
													item1.setText(text1);
												} catch (RecourseMissingException e) {
													Logger.e(TAG, e);
												}
											}
											items.add(item1);
											continue;
										default:
											continue;
									}
	                            }
	                        }
	                    }
                    }
                    break;
                }
            }
		}
		return items;
	}
	
	
	
	//解析ItemSelected方法，分析出Item的事件
	protected void analysisItemsEventMethod(SootMethod onMenuItemSelected, BaseNode node) {
		Body body = null;
		try{
			body = onMenuItemSelected.retrieveActiveBody();
		}catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body != null) {
			UnitGraph cfg = new BriefUnitGraph(body);
			Iterator<Unit> units = cfg.iterator();
			while(units.hasNext()) {
				Stmt stmt = (Stmt) units.next();
				if(stmt instanceof AbstractSwitchStmt) {
					Value key = ((AbstractSwitchStmt) stmt).getKey();
					if(key instanceof Local && isItemId((Local)key, stmt, cfg)) {
						List<Unit> targetUnits = ((AbstractSwitchStmt) stmt).getTargets();
						Unit defaultTargetUnit = ((AbstractSwitchStmt) stmt).getDefaultTarget();
						Set<Integer> handledItems = new HashSet<Integer>();
						if(stmt instanceof JLookupSwitchStmt) {
							JLookupSwitchStmt jLookupSwitchStmt = (JLookupSwitchStmt) stmt;
							int counts = jLookupSwitchStmt.getTargetCount();
							List<IntConstant> items = jLookupSwitchStmt.getLookupValues();
							for(int index = 0; index < counts; index++) {
								int item = items.get(index).value;
								Unit targetUnit = targetUnits.get(index);
								Widget menuItem = getMenuItemById(item, node);
								handleItemEvent(onMenuItemSelected, menuItem, targetUnit, cfg, node, new HashSet<Stmt>());
								handledItems.add(item);
							}
							
						}else if(stmt instanceof JTableSwitchStmt) {
							JTableSwitchStmt jTableSwitchStmt = (JTableSwitchStmt) stmt;
							int lowIndex = jTableSwitchStmt.getLowIndex();
							int highIndex = jTableSwitchStmt.getHighIndex();
							for(int index = 0; index < highIndex - lowIndex + 1; index++) {
								int item = index + lowIndex;
								Unit targetUnit = targetUnits.get(index);
								Widget menuItem = getMenuItemById(item, node);
								handleItemEvent(onMenuItemSelected, menuItem, targetUnit, cfg, node, new HashSet<Stmt>());
								handledItems.add(item);
							}
						}
						Set<Widget> unHandledItems = getUnhandledItems(node, handledItems);
						if(!unHandledItems.isEmpty()) {
							handleDefaultEvent(onMenuItemSelected, unHandledItems, defaultTargetUnit, cfg, node);
						}
						break;
					}
				}else if(stmt instanceof JIfStmt) {
					Value conditionValue = ((JIfStmt) stmt).getCondition();
					if(conditionValue instanceof EqExpr) {
						Value key = ((EqExpr) conditionValue).getOp1();
						if(key instanceof Local && key.getType().toQuotedString().equals("int")
								 && isItemId((Local)key, stmt, cfg)) {
							Value itemValue = ((EqExpr) conditionValue).getOp2();
							if(itemValue instanceof IntConstant) {
								int item = ((IntConstant) itemValue).value;
								Stmt target = ((JIfStmt) stmt).getTarget();
								Widget menuItem = getMenuItemById(item, node);
								handleItemEvent(onMenuItemSelected, menuItem, target, cfg, node, new HashSet<Stmt>());
							}
						}
					}else if(conditionValue instanceof NeExpr) {
						//有时候，菜单只有一个菜单项，它会先进行一个不等判断，不等于唯一的菜单项时直接返回false，不符合时才继续后序的工作
						if(menuNode.getWidgets().size() == 1) {
							Value key = ((NeExpr) conditionValue).getOp1();
							if(key instanceof Local && key.getType().toQuotedString().equals("int")
									 && isItemId((Local)key, stmt, cfg)) {
								List<Unit> succsU = cfg.getSuccsOf(stmt);
								Stmt target = ((JIfStmt) stmt).getTarget();
								for(Unit succU : succsU) {
									Stmt succ = (Stmt) succU;
									if(!succ.equals(target)) {
										Value itemValue = ((NeExpr) conditionValue).getOp2();
										int item = ((IntConstant) itemValue).value;
										Widget menuItem = getMenuItemById(item, node);
										handleItemEvent(onMenuItemSelected, menuItem, succ, cfg, node, new HashSet<Stmt>());
										break;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void handleDefaultEvent(SootMethod onMenuItemSelected, Set<Widget> unHandledItems, 
			Unit defaultTargetUnit, UnitGraph cfg, BaseNode node) {
		List<Unit> succs = cfg.getSuccsOf(defaultTargetUnit);
		for(Unit succ : succs) {
			Stmt stmt = (Stmt) succ;
			if(stmt.containsInvokeExpr()) {
				
				for(Widget menuItem : unHandledItems) {
					EventAnalyzer.handleEvent(stmt, cfg, node, menuItem, null);
				}
				
				InvokeExpr invokeExpr = stmt.getInvokeExpr();
				SootMethod invokee = invokeExpr.getMethod();
				if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName())) {
					for(Widget menuItem : unHandledItems) {
						interHandleItemEvent(invokee, node, menuItem, new HashSet<SootMethod>());
					}
				}
			}
			handleDefaultEvent(onMenuItemSelected, unHandledItems, stmt, cfg, node);
		}
	}
	
	private void handleItemEvent(SootMethod onMenuItemSelected, Widget menuItem, Unit targetUnit, UnitGraph cfg, BaseNode node, Set<Stmt> stmtRecords) {
		List<Unit> succs = cfg.getSuccsOf(targetUnit);
		for(Unit succ : succs) {
			Stmt stmt = (Stmt) succ;
			if(stmtRecords.contains(stmt))
				continue;
			stmtRecords.add(stmt);
			if(stmt.containsInvokeExpr()) {
				EventAnalyzer.handleEvent(stmt, cfg, node, menuItem, null);
				
				InvokeExpr invokeExpr = stmt.getInvokeExpr();
				SootMethod invokee = invokeExpr.getMethod();
				if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName())) {
					interHandleItemEvent(invokee, node, menuItem, new HashSet<SootMethod>());
				}
			}
			handleItemEvent(onMenuItemSelected, menuItem, stmt, cfg, node, stmtRecords);
		}
	}
	
	private void interHandleItemEvent(SootMethod sm, BaseNode node, Widget menuItem, Set<SootMethod> recordes) {
		recordes.add(sm);
		Body body = null;
		try {
			body = sm.retrieveActiveBody();
		} catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body != null) {
			UnitGraph cfg = new BriefUnitGraph(body);
			Iterator<Unit> units = cfg.iterator();
			while(units.hasNext()) {
				Stmt stmt = (Stmt) units.next();
				if(stmt.containsInvokeExpr()) {
					EventAnalyzer.handleEvent(stmt, cfg, node, menuItem, null);
					
					InvokeExpr invokeExpr = stmt.getInvokeExpr();
					SootMethod invokee = invokeExpr.getMethod();
					if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
							&& !recordes.contains(invokee)) {
						interHandleItemEvent(invokee, node, menuItem, recordes);
					}
				}
			}
		}
	}
	
	private Set<Widget> getUnhandledItems(BaseNode node, Set<Integer> handledItems){
		Set<Widget> unHandledItems = new HashSet<Widget>();
		if(id_item.isEmpty()) {
			getMenuItems(node);
		}
		for(int id : id_item.keySet()) {
			if(!handledItems.contains(id)) {
				unHandledItems.add(id_item.get(id));
			}
		}
		return unHandledItems;
	}
	
	private Map<Integer, Widget> id_item = new HashMap<Integer, Widget>();
	private Widget getMenuItemById(int item, BaseNode node) {
		if(id_item.isEmpty()) {
			getMenuItems(node);
		}
		Widget menuItem = id_item.get(item);
		if(menuItem == null) {
			Logger.i(TAG, "The corresponding widget for item["+item+"] could not be found, generate one.");
			menuItem = new MenuItem();
			menuItem.setId(IdProvider.v().widgetId());
			menuItem.setResId(item);
		}
		return menuItem;
	}
	
	private void getMenuItems(BaseNode node) {
		for(Widget w : node.getWidgets()) {
			if(w instanceof MenuItem) {
				id_item.put(((MenuItem)w).getItemId(), w);
			}else if(w instanceof SubMenu) {
				SubMenu subMenu = (SubMenu) w;
				id_item.put(subMenu.getSubMenuId(), subMenu);
				for(MenuItem menuItem : subMenu.getItems()) {
					id_item.put(menuItem.getItemId(), menuItem);
				}
			}
		}
	}

	private boolean isItemId(Local key, Stmt stmt, UnitGraph cfg) {
		SmartLocalDefs smd = new SmartLocalDefs(cfg, new SimpleLiveLocals(cfg));
		List<Unit> defsOfKey = smd.getDefsOfAt((Local)key, stmt);
		for(Unit def : defsOfKey) {
			if(def instanceof AssignStmt) {
				AssignStmt assignStmt = (AssignStmt) def;
				if(assignStmt.containsInvokeExpr()) {
					InvokeExpr invokeExpr = assignStmt.getInvokeExpr();
					SootMethod invokee = invokeExpr.getMethod();
					if(invokee.getSignature().equals("<android.view.MenuItem: int getItemId()>")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
}
