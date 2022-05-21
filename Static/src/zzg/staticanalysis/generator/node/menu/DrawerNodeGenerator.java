package zzg.staticanalysis.generator.node.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.Manager;
import zzg.staticanalysis.RecourseMissingException;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.analyzer.ButterKnifeHandler;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.model.Transition;
import zzg.staticanalysis.model.FragNode;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticguimodel.NodeType;
import zzg.staticguimodel.Widget;

public class DrawerNodeGenerator extends AMenuNodeGenerator {
	private static final String TAG = "[Generator-DrawerNode]";

	public static BaseNode build(SootClass sc, Widget drawer, String layoutName) {//不是静态注册的菜单，需要在代码中找到
		Logger.i(TAG, "Start to generate DrawerNode ["+sc.getName()+"]");
		return new DrawerNodeGenerator(sc, drawer, layoutName).getMenuItems().generate();
	}

	public static BaseNode build(List<Widget> menuItems, String layoutName) {
		Logger.i(TAG, "Start to generate DrawerNode By Layout ["+layoutName+"]");
		return new DrawerNodeGenerator(menuItems, layoutName).generate();
	}
	
	private List<Widget> menuItems;
	private SootClass sc;
	private BaseNode drawerNode;
	private Widget drawer;
	private String layoutName;
	
	private DrawerNodeGenerator(List<Widget> menuItems, String layoutName) {
		this.menuItems = menuItems;
		this.layoutName = layoutName;
		this.drawerNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DRAWER);
	}
	
	private DrawerNodeGenerator(SootClass sc, Widget drawer, String layoutName) {
		this.sc = sc;
		this.drawer = drawer;
		this.layoutName = layoutName;
		this.menuItems = new ArrayList<Widget>();
		this.drawerNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DRAWER);
	}
	
	private DrawerNodeGenerator getMenuItems() {
		//<com.google.android.material.navigation.NavigationView void inflateMenu(int)>
		for(SootMethod sm : sc.getMethods()) {
			if(sm.isConcrete()) {
				Body body = null;
				try {
					body = sm.retrieveActiveBody();
				}catch (RuntimeException e) {
					Logger.e(TAG, new ActiveBodyNotFoundException(e));
				}
				if(body != null) {
					BriefUnitGraph cfg = new BriefUnitGraph(body);
					Iterator<Unit> units = cfg.iterator();
					while(units.hasNext()) {
						Stmt stmt = (Stmt)units.next();
						if(stmt.containsInvokeExpr()) {
							InvokeExpr invokeExpr = stmt.getInvokeExpr();
							SootMethod invokee = invokeExpr.getMethod();
							if(invokee.getSignature().equals("<com.google.android.material.navigation.NavigationView void inflateMenu(int)>")) {
								InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
								Value navigationViewValue = instanceInvokeExpr.getBase();
								//data flow find the invokerView's id, check the id is equals to the drawer's id
								int id = getNavigationViewId(stmt, cfg, navigationViewValue);
								if(drawer.getResId() == id) {
									Value menuIdValue = invokeExpr.getArg(0);
									if(menuIdValue instanceof IntConstant) {
										int menuId = ((IntConstant)menuIdValue).value;
										try {
											String menuName = AppParser.v().getMenuNameById(menuId);
											menuItems.addAll(AppParser.v().parseMenu(menuName));
										} catch (RecourseMissingException e) {
											Logger.e(TAG, e);
										}
										return this;
									}
								}
							}
						}
					}
				}
			}
		}
		return this;
	}

	@Override
	protected BaseNode generate() {
		drawerNode.setWidgets(menuItems);
		Map<Integer, String> itemId_targetFragName = AppParser.v().getNavHostFragment(layoutName);
		for(Widget item : menuItems) {
			int itemId = item.getResId();
			String target = itemId_targetFragName.get(itemId);
			if(target != null) {
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setWidget(item);
				t.setSrc(drawerNode.getId());//应该用活动ID吧， 使用新建的drawerNodeID
				FragNode tgt = Manager.v().getFragNodeByName(target);
				if(tgt != null)
					t.setTgt(tgt.getId());
				else {
					t.setLabel("MTN["+target+"]");//Missing Target Node
				}
				Logger.i(TAG, "Build A T in class : "+sc.getName());
			}
		}
		return drawerNode;
	}
	
	private int getNavigationViewId(Stmt stmt, UnitGraph cfg, Value navigationViewValue) {
		SootMethod sm = cfg.getBody().getMethod();
		Stmt curStmt = stmt, dataflowStmt = stmt;
		Value curValue = navigationViewValue;
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
						if(rMethod.getName().equals("findViewById")) {
							Value arg0 = virtualInvokeExpr.getArg(0);
							if(arg0 instanceof IntConstant) {
								int invokerViewId = ((IntConstant)arg0).value;
								return invokerViewId;
							}
						}
					}
					if(right instanceof NewExpr) {
						NewExpr newExpr = (NewExpr) right;
						if(curValue.getType().equals(newExpr.getType())) {
							return -1;
						}
					}
				}
			}
		}
		//navigationView = $r1.<className: fieldType fieldName>
		if(dataflowStmt instanceof AssignStmt && dataflowStmt.containsFieldRef()) {
			AssignStmt assignStmt = (AssignStmt)dataflowStmt;
			Value leftOp = assignStmt.getLeftOp();
			if(leftOp.equivTo(curValue)) {
				Value rightOp = assignStmt.getRightOp();
				if(rightOp instanceof FieldRef) {
					FieldRef fieldRef = (FieldRef)rightOp;
					return getNavigationViewIdFromField(sm, fieldRef);
				}
			}
		}
		//navigationView := @paramterX: parameterType
		if(dataflowStmt instanceof IdentityStmt) {
			IdentityStmt identityStmt = (IdentityStmt)dataflowStmt;
			Value leftOp = identityStmt.getLeftOp();
			if(leftOp.equivTo(curValue)) {
				Value rightOp = identityStmt.getRightOp();
				if(rightOp instanceof ParameterRef) {
					int position = ((ParameterRef)rightOp).getIndex();
					return getNavigationViewIdFromParam(sm, position);
				}
			}
		}
		return -1;
	}

	private int getNavigationViewIdFromField(SootMethod sm, FieldRef fieldRef) {
		SootField field = fieldRef.getField();
		SootClass fieldClass = field.getDeclaringClass();
		int id = ButterKnifeHandler.v().getResId(fieldClass, field);
		if(id != -1) {
			return id;
		}else {
			for(SootMethod method : sm.getDeclaringClass().getMethods()) {
				if(!method.equals(sm)) {
					Body body = null;
					try {
						body = method.retrieveActiveBody();
					}catch (RuntimeException e) {
						Logger.e(TAG, new ActiveBodyNotFoundException(e));
					}
					if(body != null) {
						UnitGraph cfg = new BriefUnitGraph(body);
						Iterator<Unit> units = cfg.iterator();
						while(units.hasNext()) {
							Stmt curStmt = (Stmt)units.next();
							if(curStmt instanceof AssignStmt && curStmt.containsFieldRef()) {
								Value leftOp = ((AssignStmt) curStmt).getLeftOp();
								if(leftOp instanceof FieldRef) {
									if(fieldRef.equivTo((FieldRef)leftOp)) {
										Value curValue = ((AssignStmt) curStmt).getRightOp();
										if (curValue instanceof CastExpr) {
	                                        CastExpr extraExpr = (CastExpr) curValue;
	                                        curValue = extraExpr.getOp();
	                                    }
										return getNavigationViewId(curStmt, cfg, curValue);
									}
								}
                            }
						}
					}
				}
			}
		}
		return -1;
	}

	private int getNavigationViewIdFromParam(SootMethod sm, int position) {
		Iterator<soot.jimple.toolkits.callgraph.Edge> edges = AppParser.v().getCg().edgesInto(sm);
		if(!edges.hasNext()) {
			StringBuffer sb = new StringBuffer();
			sb.append("Method[").append(sm.getSignature()).append("] haven't caller. Find Widget fail.");
			Logger.e(TAG, new RuntimeException(sb.toString()));
			return -1;
		}
		while(edges.hasNext()) {
			SootMethod caller = edges.next().src();
			Body body = null;
			try {
				body = caller.retrieveActiveBody();
			}catch (RuntimeException e) {
				Logger.e(TAG, new ActiveBodyNotFoundException(e));
			}
			if(body != null) {
				UnitGraph cfg = new BriefUnitGraph(body);
				Iterator<Unit> units = cfg.iterator();
				while(units.hasNext()) {
					Stmt stmt = (Stmt) units.next();
					if(stmt.containsInvokeExpr()) {
						InvokeExpr invokeExpr = stmt.getInvokeExpr();
						if(invokeExpr.getMethod().getSignature().equals(sm.getSignature())) {
							Value navigationViewValue = invokeExpr.getArg(position);
							int id = getNavigationViewId(stmt, cfg, navigationViewValue);
							if(id != -1)
								return id;
						}
					}
				}
			}
		}
		return -1;
	}
}
