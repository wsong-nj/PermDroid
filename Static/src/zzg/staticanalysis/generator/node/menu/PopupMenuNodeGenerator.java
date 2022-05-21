package zzg.staticanalysis.generator.node.menu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.Scene;
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
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.Manager;
import zzg.staticanalysis.RecourseMissingException;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.NodeType;

public class PopupMenuNodeGenerator extends AMenuNodeGenerator {
	private static final String TAG = "[Generator-PopupMenu]";
	
	public static BaseNode build(SootMethod sm, Stmt stmt) {
		Logger.i(TAG, "Start to generate PopupMenu ["+sm.getSignature()+"]");
		return new PopupMenuNodeGenerator(sm, stmt).generate();
	}
	
	private PopupMenuNodeGenerator(SootMethod sm) {
		super(sm);
	}
	private PopupMenuNodeGenerator(SootMethod sm, Stmt stmt) {
		super();
		this.sm = sm;
		this.stmt = stmt;
		popupMenuNode = new BaseNode(IdProvider.v().nodeId(), NodeType.POPUPMENU);
	}
	private SootMethod sm;
	private Stmt stmt;//virtualinvoke mPopupMenu.<....PopupMenu: void show()>();
	private BaseNode popupMenuNode;

	@Override
	protected BaseNode generate() {
		buildPopupMenuNode();
		if(eventHandler != null) {
			super.analysisItemsEventMethod(eventHandler, popupMenuNode);
		}
		return popupMenuNode;
	}

	private static final String SETONMENUITEMCLICKLISTENER1 = "<android.widget.PopupMenu: void setOnMenuItemClickLstener(android.widget.PopupMenu$OnMenuItemClickListener)>";
	private static final String SETONMENUITEMCLICKLISTENER2 = "<android.support.v7.widget.PopupMenu: void setOnMenuItemClickLstener(android.support.v7.widget.PopupMenu$OnMenuItemClickListener)>";
	private static final String SETONMENUITEMCLICKLISTENER3 = "<androidx.appcompat.widget.PopupMenu: void setOnMenuItemClickLstener(androidx.appcompat.widget.PopupMenu$OnMenuItemClickListener)>";
	
	private static final String INFLATE1 = "<android.widget.PopupMenu: void inflate(int)>";
	private static final String INFLATE2 = "<android.support.v7.widget.PopupMenu: void inflate(int)>";
	private static final String INFLATE3 = "<androidx.appcompat.widget.PopupMenu: void inflate(int)>";
	private static final String MENUINFLATE = "<android.view.MenuInflater: void inflate(int,android.view.Menu)>";
	
	private SootMethod eventHandler;
	private int menuId = -1;
	private boolean finish = false;
	private Set<SootMethod> records = new HashSet<SootMethod>();
	
	private void buildPopupMenuNode() {
		records.add(sm);
		Value popupMenuValue = ((InstanceInvokeExpr) stmt.getInvokeExpr()).getBase();
		UnitGraph cfg = new BriefUnitGraph(sm.retrieveActiveBody());
		forwardBuildPopupMenu(stmt, cfg, popupMenuValue);
		buildMenuItem();
	}
	
	/**
	 * 向前
	 * @param stmt
	 * @param cfg
	 * @param popupMenuValue
	 */
	private void forwardBuildPopupMenu(Stmt stmt, UnitGraph cfg, Value popupMenuValue) {
		SootMethod sm = cfg.getBody().getMethod();
		Stmt curStmt = stmt, dataflowStmt = stmt;
		Value curValue = popupMenuValue;
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
					}else if(right instanceof Local) {
						curValue = right;
					}
					if((dataflowStmt instanceof IdentityStmt) 
							|| (dataflowStmt instanceof AssignStmt && dataflowStmt.containsFieldRef())) {
						break;
					}
				}
			}
			if(curStmt.containsInvokeExpr()) {
				analysis(curStmt, curValue, cfg);
				if(finish) 
					return;
			}
		}
		//popupMenu := @paramterX: parameterType
		if(dataflowStmt instanceof IdentityStmt) {
			IdentityStmt identityStmt = (IdentityStmt)dataflowStmt;
			Value leftOp = identityStmt.getLeftOp();
			if(leftOp.equivTo(curValue)) {
				Value rightOp = identityStmt.getRightOp();
				if(rightOp instanceof ParameterRef) {
					int position = ((ParameterRef)rightOp).getIndex();
					buildPopupMenuFromParam(sm, position);
				}
			}
		}
		//popupMenu = $r1.<class: type field>
		else if(dataflowStmt instanceof AssignStmt && dataflowStmt.containsFieldRef()) {
			AssignStmt assignStmt = (AssignStmt)dataflowStmt;
			Value leftOp = assignStmt.getLeftOp();
			if(leftOp.equivTo(curValue)) {
				Value rightOp = assignStmt.getRightOp();
				if(rightOp instanceof FieldRef) {
					FieldRef fieldRef = (FieldRef)rightOp;
					buildPopupMenuFromField(sm, fieldRef);
				}
			}
		}
	}

	/**
	 * 向后
	 * @param stmt
	 * @param cfg
	 * @param curValue
	 */
	private void backwardBuildPopupMenu(Stmt stmt, UnitGraph cfg, Value curValue) {
		Stmt curStmt = stmt;
		while(!cfg.getSuccsOf(curStmt).isEmpty()) {
			List<Unit> succStmts = cfg.getSuccsOf(curStmt);
			curStmt = (Stmt)succStmts.get(0);
			if(curStmt instanceof AssignStmt) {
				AssignStmt curAssignStmt = (AssignStmt)curStmt;
				Value left = curAssignStmt.getLeftOp();
				if(left.equivTo(curValue)) {
					Value right = curAssignStmt.getRightOp();
					if(right instanceof CastExpr) {
						CastExpr castExpr = (CastExpr)right;
						curValue = castExpr.getOp();
					}else if(right instanceof Local) {
						curValue = right;
					}
				}
			}
			if(curStmt.containsInvokeExpr()) {
				analysis(curStmt, curValue, cfg);
				if(finish)
					return;
			}
		}
	}
	
	private void buildPopupMenuFromParam(SootMethod sm, int position) {
		Iterator<soot.jimple.toolkits.callgraph.Edge> edges = AppParser.v().getCg().edgesInto(sm);
		if(!edges.hasNext()) {
			StringBuffer sb = new StringBuffer();
			sb.append("Method[").append(sm.getSignature()).append("] haven't caller. Find Widget fail.");
			Logger.e(TAG, new RuntimeException(sb.toString()));
			return;
		}
		while(edges.hasNext()) {
			SootMethod caller = edges.next().src();
			if(records.contains(caller))
				continue;
			records.add(caller);
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
							Value popupMenuValue = invokeExpr.getArg(position);
							forwardBuildPopupMenu(stmt, cfg, popupMenuValue);
							if(finish)
								return;
						}
					}
				}
			}
		}
	}
	
	private void buildPopupMenuFromField(SootMethod sm, FieldRef fieldRef) {
		Value popupMenuValue = null;
		UnitGraph cfg = null;
		Stmt stmt = null;
		
		//在popupMenu = $r1.<class: type field>所在的方法所在的类中找xx.<class: type field> = yy
		A: for(SootMethod method : sm.getDeclaringClass().getMethods()) {
			if (!method.equals(sm)/* && !records.contains(method) */) {
				records.add(method);
				Body body = null;
				try {
					body = method.retrieveActiveBody();
				}catch (RuntimeException e) {
					Logger.e(TAG, new ActiveBodyNotFoundException(e));
				}
				if(body != null) {
					UnitGraph extraCfg = new BriefUnitGraph(body);
					Iterator<Unit> units = extraCfg.iterator();
					while(units.hasNext()) {
						Stmt curStmt = (Stmt)units.next();
						if(curStmt instanceof AssignStmt && curStmt.containsFieldRef()) {
							AssignStmt assignStmt = (AssignStmt) curStmt;
                            Value leftOp = assignStmt.getLeftOp();
                            if(leftOp instanceof FieldRef) {
                            	FieldRef curFieldRef = (FieldRef)leftOp;
                            	if(curFieldRef.getField().equals(fieldRef.getField())) {
                            		popupMenuValue = assignStmt.getRightOp();
                            		cfg = extraCfg;
                            		stmt = curStmt;
                            		break A;
                            	}
                            }
						}
					}
				}
			}
		}
		if(popupMenuValue != null && cfg != null && stmt != null) {
			backwardBuildPopupMenu(stmt, cfg, popupMenuValue);
			finish = true;
			return;
		}
		SootField field = fieldRef.getField();
		SootClass owner = field.getDeclaringClass();
		B: for(SootMethod method : owner.getMethods()) {
			if(records.contains(method))
				continue;
			records.add(method);
			Body body = null;
			try {
				body = method.retrieveActiveBody();
			}catch (RuntimeException e) {
				Logger.e(TAG, new ActiveBodyNotFoundException(e));
			}
			if(body != null) {
				UnitGraph extraCfg = new BriefUnitGraph(body);
				Iterator<Unit> units = extraCfg.iterator();
				while(units.hasNext()) {
					Stmt curStmt = (Stmt)units.next();
					if(curStmt instanceof AssignStmt && curStmt.containsFieldRef()) {
						AssignStmt assignStmt = (AssignStmt)curStmt;
						Value leftOp = assignStmt.getLeftOp();
						if(leftOp instanceof FieldRef) {
							FieldRef curFieldRef = (FieldRef)leftOp;
							if(curFieldRef.getField().equals(fieldRef.getField())) {
                        		popupMenuValue = assignStmt.getRightOp();
                        		cfg = extraCfg;
                        		stmt = curStmt;
                        		break B;
							}
						}
					}
				}
			}
		}
		if(popupMenuValue != null && cfg != null && stmt != null) {
			backwardBuildPopupMenu(stmt, cfg, popupMenuValue);
			finish = true;
			return;
		}
		
		//当在popupMenu = $r1.<class: type field>所在的方法所在的类找不到时，
		String FLAG = "";
		if(field.isPrivate()) {//private 内部类
			FLAG = "private";
		}else if(!field.isPrivate() && !field.isProtected() && !field.isPublic()) {//default +包类
			FLAG = "default";
		}else if(field.isProtected()) {//protected +子类
			FLAG = "protected";
		}else{//public 太广了，在前面的找不到之后，再找其他
			FLAG = "public";
		}
		
		
		List<SootClass> innerClasses = new ArrayList<SootClass>();//owner的内部类
		List<SootClass> subClasses = new ArrayList<SootClass>();//owner的子类
		List<SootClass> pkgClasses = new ArrayList<SootClass>();//owner的同包类
		List<SootClass> others = new ArrayList<SootClass>();
		Utils.getRelatedClasses(owner, innerClasses, subClasses, pkgClasses, others, FLAG);
		
		Set<SootClass> relatedClasses = new HashSet<SootClass>();
		if(field.isPrivate()) {//private 内部类
			relatedClasses.addAll(innerClasses);
		}else if(!field.isPrivate() && !field.isProtected() && !field.isPublic()) {//default +包类
			relatedClasses.addAll(innerClasses);
			relatedClasses.addAll(pkgClasses);
		}else if(field.isProtected()) {//protected +子类
			relatedClasses.addAll(innerClasses);
			relatedClasses.addAll(pkgClasses);
			relatedClasses.addAll(subClasses);
		}else{//public 太广了，在前面的找不到之后，再找其他
		}
		
		C: for(SootClass sc : relatedClasses) {
			for(SootMethod method : sc.getMethods()) {
				if(records.contains(method))
					continue;
				records.add(method);
				Body body = null;
				try {
					body = method.retrieveActiveBody();
				}catch (RuntimeException e) {
					Logger.e(TAG, new ActiveBodyNotFoundException(e));
				}
				if(body != null) {
					UnitGraph extraCfg = new BriefUnitGraph(body);
					Iterator<Unit> units = extraCfg.iterator();
					while(units.hasNext()) {
						Stmt curStmt = (Stmt)units.next();
						if(curStmt instanceof AssignStmt && curStmt.containsFieldRef()) {
							AssignStmt assignStmt = (AssignStmt)curStmt;
							Value leftOp = assignStmt.getLeftOp();
							if(leftOp instanceof FieldRef) {
								FieldRef curFieldRef = (FieldRef)leftOp;
								if(curFieldRef.getField().equals(fieldRef.getField())) {
                            		popupMenuValue = assignStmt.getRightOp();
                            		cfg = extraCfg;
                            		stmt = curStmt;
                            		break C;
								}
							}
						}
					}
				}
			}
		}
		if(popupMenuValue != null && cfg != null && stmt != null) {
			backwardBuildPopupMenu(stmt, cfg, popupMenuValue);
			finish = true;
			return;
		}
		//public
		if(field.isPublic()) {
			D: for(SootClass sc : others) {
				for(SootMethod method : sc.getMethods()) {
					if(records.contains(method))
						continue;
					records.add(method);
					Body body = null;
					try {
						body = method.retrieveActiveBody();
					}catch (RuntimeException e) {
						Logger.e(TAG, new ActiveBodyNotFoundException(e));
					}
					if(body != null) {
						UnitGraph extraCfg = new BriefUnitGraph(body);
						Iterator<Unit> units = extraCfg.iterator();
						while(units.hasNext()) {
							Stmt curStmt = (Stmt)units.next();
							if(curStmt instanceof AssignStmt && curStmt.containsFieldRef()) {
								AssignStmt assignStmt = (AssignStmt)curStmt;
								Value leftOp = assignStmt.getLeftOp();
								if(leftOp instanceof FieldRef) {
									FieldRef curFieldRef = (FieldRef)leftOp;
									if(curFieldRef.getField().equals(fieldRef.getField())) {
	                            		popupMenuValue = assignStmt.getRightOp();
	                            		cfg = extraCfg;
	                            		stmt = curStmt;
	                            		break D;
									}
								}
							}
						}
					}
				}
			}
			if(popupMenuValue != null && cfg != null && stmt != null) {
				backwardBuildPopupMenu(stmt, cfg, popupMenuValue);
				finish = true;
				return;
			}
		}
	}
	
	private void analysis(Stmt curStmt, Value curValue, UnitGraph cfg) {
		InvokeExpr invokeExpr = curStmt.getInvokeExpr();
		SootMethod invokee = invokeExpr.getMethod();
		if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName())
				&& !records.contains(invokee)) {
			List<Value> args = invokeExpr.getArgs();
			int index = 0;
			for(Value arg : args) {
				if(arg.equivTo(curValue)) {
					break;
				}else {
					index++;
				}
			}
			if(index < args.size()) {
				// <className: returnType methodName(params)>(args, intent, args)
				buildPopupMenuFromOtherMethodParam(invokee, index);
				if(finish)
					return;
			}else if(curStmt instanceof AssignStmt) {
				//intent = $r1.<className: returnType methodName(params)>(args)
				buildPopupMenuFromOtherMethodReturn(invokee);
				if(finish)
					return;
			}
		}
		if(invokeExpr instanceof VirtualInvokeExpr) {
			VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;
			Value invoker = virtualInvokeExpr.getBase();
			if(invoker.equivTo(curValue)) {
				String invokeeSignature = invokee.getSignature();
				if(invokeeSignature.equals(SETONMENUITEMCLICKLISTENER1)
						|| invokeeSignature.equals(SETONMENUITEMCLICKLISTENER2)
						|| invokeeSignature.equals(SETONMENUITEMCLICKLISTENER3)) {
					Value listenerValue = virtualInvokeExpr.getArg(0);
					String listenerClassName = listenerValue.getType().toQuotedString();
					SootClass listenerClass = Scene.v().getSootClassUnsafe(listenerClassName);
					if(listenerClass != null) {
						try {
							eventHandler = listenerClass.getMethod("boolean onMenuItemClick(android.view.MenuItem)");
							if(menuId != -1) {
								finish = true;
								return;
							}
						}catch (RuntimeException e) {
							Logger.e(TAG, e);
						}
					}
				}else if(invokeeSignature.equals(INFLATE1)
						|| invokeeSignature.equals(INFLATE2)
						|| invokeeSignature.equals(INFLATE3)) {
					Value menuLayoutValue = virtualInvokeExpr.getArg(0);
					if(menuLayoutValue instanceof IntConstant) {
						menuId = ((IntConstant)menuLayoutValue).value;
						try {
							String menuLayoutName = AppParser.v().getLayoutNameById(menuId);
							popupMenuNode.setWidgets(AppParser.v().parseMenu(menuLayoutName));
						} catch (RecourseMissingException e) {
							Logger.e(TAG, e);
						}
						if(eventHandler != null) {
							finish = true;
							return;
						}
					}
				}
			}else if(invokee.getSignature().equals(MENUINFLATE)) {
				if(isMenuInflate(curStmt, cfg, invoker, curValue)) {
					Value menuLayoutValue = virtualInvokeExpr.getArg(0);
					if(menuLayoutValue instanceof IntConstant) {
						menuId = ((IntConstant)menuLayoutValue).value;
						try {
							String menuLayoutName = AppParser.v().getLayoutNameById(menuId);
							popupMenuNode.setWidgets(AppParser.v().parseMenu(menuLayoutName));
						} catch (RecourseMissingException e) {
							Logger.e(TAG, e);
						}
						if(eventHandler != null) {
							finish = true;
							return;
						}
					}
				}
			}
		}else if(invokeExpr instanceof SpecialInvokeExpr) {
			Value invoker = ((SpecialInvokeExpr) invokeExpr).getBase();
			if(invoker.equivTo(curValue) && invokee.getName().equals("<init>")) {
				finish = true;
				return;
			}
		}
	}
	

	private void buildPopupMenuFromOtherMethodParam(SootMethod sm, int position) {
		if(records.contains(sm))
			return;
		records.add(sm);
		Body body = null;
		try {
			body = sm.retrieveActiveBody();
		} catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body != null) {
			Value curValue = null;
			Stmt curStmt = null;
			for(Unit s : body.getUnits()) {
				if (s instanceof IdentityStmt && ((IdentityStmt) s).getRightOp() instanceof ParameterRef) {
			        IdentityStmt is = (IdentityStmt) s;
			        ParameterRef pr = (ParameterRef) is.getRightOp();
			        if (pr.getIndex() == position) {
			        	curValue = is.getLeftOp();
			        	curStmt = is;
			        }
				}
			}
			if(curStmt != null && curValue != null) {
				UnitGraph cfg = new BriefUnitGraph(body);
				backwardBuildPopupMenu(curStmt, cfg, curValue);
			}
		}
	}

	private void buildPopupMenuFromOtherMethodReturn(SootMethod invokee) {
		if(records.contains(sm))
			return;
		records.add(sm);
		Body body = null;
		try {
			body = sm.retrieveActiveBody();
		}catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body != null) {
			UnitGraph cfg = new BriefUnitGraph(body);
			ReturnStmt returnStmt = Utils.getReturnStmt(cfg);
			if(returnStmt != null) {
				Value returnValue = returnStmt.getOp();
				forwardBuildPopupMenu(returnStmt, cfg, returnValue);
			}
		}
	}

	private boolean isMenuInflate(Stmt stmt, UnitGraph cfg, Value inflateInvokerValue, Value popupMenuValue) {
		Stmt curStmt = stmt;
		while(!cfg.getPredsOf(curStmt).isEmpty()) {
			List<Unit> preStmts = cfg.getPredsOf(curStmt);
			curStmt = (Stmt)preStmts.get(0);
			if(curStmt instanceof AssignStmt) {
				AssignStmt curAssignStmt = (AssignStmt)curStmt;
				Value left = curAssignStmt.getLeftOp();
				if(left.equivTo(inflateInvokerValue)) {
					Value right = curAssignStmt.getRightOp();
					//inflateInvokerValue = virtualinvoke popupMenuValue.<android.widget.PopupMenu: android.view.MenuInflater getMenuInflater()>();
					if(right instanceof VirtualInvokeExpr) {
						VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr)right;
						SootMethod rMethod = virtualInvokeExpr.getMethod();
						if(rMethod.getName().equals("getMenuInflater")) {
							Value invoker = virtualInvokeExpr.getBase();
							if(invoker.equivTo(popupMenuValue)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	
	
	private void buildMenuItem() {
		if(menuId != -1) {
			try {
				String menuName = AppParser.v().getMenuNameById(menuId);
				popupMenuNode.addAllWidgets(AppParser.v().parseMenu(menuName));
			} catch (RecourseMissingException e) {
				Logger.e(TAG, e);
			}
		}
	}
}
