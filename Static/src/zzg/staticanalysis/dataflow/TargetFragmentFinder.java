package zzg.staticanalysis.dataflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.utils.ClassService;
import zzg.staticanalysis.utils.Logger;

public class TargetFragmentFinder {
	private static final String TAG = "[TargetFragmentFinder]";
	
	public static Set<String> find(UnitGraph cfg, Stmt stmt, SootMethod caller){
		Logger.i(TAG, "----Strat to fing target fragments for:");
		Logger.i(TAG, "--------Stmt: " + stmt.toString());
		Logger.i(TAG, "--------Method: " + cfg.getBody().getMethod().getSignature());
		Logger.i(TAG, "--------Caller: " + (caller == null ? "null" : caller.getSignature()));
		Set<String> result = new TargetFragmentFinder().start(cfg, stmt, caller);
		Logger.i(TAG, "----End");
		return result;
	}

	private Set<String> fragments = new HashSet<String>();
	
	/**
	 * add(int, Fragment, String)
	 * add(Fragment, String)
	 * add(int, Fragment)
	 * add(Class<? extends Fragment>, Bundle, String)
	 * add(int, Class<? extends Fragment>, Bundle)
	 * add(int, Class<? extends Fragment>, Bundle, String)
	 * 
	 * replace(int, Class<? extends Fragment>, Bundle, String)
	 * replace(int, Class<? extends Fragment>, Bundle)
	 * replace(int, Fragment, String)
	 * replace(int, Fragment)
	 */
	
	private Set<String> start(UnitGraph cfg, Stmt stmt, SootMethod caller) {
		if(!stmt.containsInvokeExpr())
			return fragments;
		
		String fragmentClassName = null;
		Value fragmentValue = null;
		
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		SootMethod invokee = invokeExpr.getMethod();
		List<Type> paramTypes = invokee.getParameterTypes();
		for(int index = 0; index < paramTypes.size(); index++) {
			String paramType = paramTypes.get(index).toQuotedString();
			if(paramType.equals("java.lang.Class")) {
				fragmentValue = invokeExpr.getArg(index);
				Pattern pattern = Pattern.compile("class \"L(.*?);");
				Matcher m = pattern.matcher(fragmentValue.toString().toString());
				if(m.find()){
					fragmentClassName = m.group(1).trim().replace("/", ".");
				}
				break;
			}else if(paramType.equals("androidx.fragment.app.Fragment")
					|| paramType.equals("android.app.Fragment")
					|| paramType.equals("android.support.v4.app.Fragment")) {
				fragmentValue = invokeExpr.getArg(index);
				fragmentClassName = fragmentValue.getType().toQuotedString();
				break;
			}
		}
		if(fragmentClassName != null) {
			if(!fragmentClassName.equals("androidx.fragment.app.Fragment") 
					&& !fragmentClassName.equals("android.app.Fragment") 
					&& !fragmentClassName.equals("android.support.v4.app.Fragment")) {
				fragments.add(fragmentClassName);
			}else {
				if(fragmentValue != null && fragmentValue instanceof Local) {
					
				}
			}
		}
		return fragments;
	}
	
	private void findFragments(Local fragLocal, Stmt stmt, UnitGraph cfg, SootMethod caller) {
		SmartLocalDefs localDefs = new SmartLocalDefs(cfg, new SimpleLiveLocals(cfg));
		List<Unit> defs = localDefs.getDefsOfAt(fragLocal, stmt);
		for(Unit def : defs) {
			if(def instanceof DefinitionStmt) {
				Value rightOp = ((DefinitionStmt) def).getRightOp();
				if(rightOp instanceof ParameterRef) {//来自参数
					int position = ((ParameterRef)rightOp).getIndex();
					fromParam(cfg.getBody().getMethod(), position, caller, new ArrayList<SootMethod>());
				}else if(rightOp instanceof FieldRef) {//来自域
					FieldRef fieldRef = (FieldRef)rightOp;
					fromField(cfg.getBody().getMethod(), fieldRef, caller);
				}else if(rightOp instanceof Local) {//来自其他Value的赋值，例如：$r7 = $r3
					String fragmentClassName = rightOp.getType().toQuotedString();
					if(!fragmentClassName.equals("androidx.fragment.app.Fragment") 
							&& !fragmentClassName.equals("android.app.Fragment") 
							&& !fragmentClassName.equals("android.support.v4.app.Fragment")) {
						fragments.add(fragmentClassName);
					}else {
						findFragments((Local)rightOp, (DefinitionStmt) def, cfg, caller);
					}
				}else if(rightOp instanceof JStaticInvokeExpr){
					SootClass sClass = ((JStaticInvokeExpr) rightOp).getMethod().getDeclaringClass();
					if(ClassService.isFragment(sClass)) {
						fragments.add(sClass.getName());
					}
				}else {
					System.out.println("DDD : "+def.toString()+" "+rightOp.getClass().getName());
				}
			}
		}
	}
	
	private void fromParam(SootMethod sm, int position, SootMethod caller, List<SootMethod> records) {
		if(caller != null) {
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
							Value fragmentValue = invokeExpr.getArg(position);
							String fragmentClassName = fragmentValue.getType().toQuotedString();
							if(!fragmentClassName.equals("androidx.fragment.app.Fragment") 
									&& !fragmentClassName.equals("android.app.Fragment") 
									&& !fragmentClassName.equals("android.support.v4.app.Fragment")) {
								fragments.add(fragmentClassName);
							}else {
								if(fragmentValue instanceof Local) {
									findFragments((Local)fragmentValue, stmt, cfg, null);
								}else {
									System.out.println("DDD "+fragmentValue.getClass().getName());
								}
							}
						}
					}
				}
			}
		}else {
			Iterator<Edge> intos = AppParser.v().getCg().edgesInto(sm);
			if(!intos.hasNext()) {
				Logger.e(TAG, new RuntimeException("Method[" + sm.getSignature() + "] haven't caller. Find target activity fail."));
				return;
			}
			while(intos.hasNext()) {
				caller = intos.next().src();
				if(records.add(caller)) {
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
									Value fragmentValue = invokeExpr.getArg(position);
									String fragmentClassName = fragmentValue.getType().toQuotedString();
									if(!fragmentClassName.equals("androidx.fragment.app.Fragment") 
											&& !fragmentClassName.equals("android.app.Fragment") 
											&& !fragmentClassName.equals("android.support.v4.app.Fragment")) {
										fragments.add(fragmentClassName);
									}else {
										if(fragmentValue instanceof Local) {
											findFragments((Local)fragmentValue, stmt, cfg, null);
										}else {
											System.out.println("DDD "+fragmentValue.getClass().getName());
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void fromField(SootMethod sm, FieldRef fieldRef, SootMethod caller) {
		for(SootMethod method : sm.getDeclaringClass().getMethods()) {
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
					Stmt stmt = (Stmt)units.next();
					if(stmt instanceof AssignStmt && stmt.containsFieldRef()) {
						AssignStmt assignStmt = (AssignStmt) stmt;
						Value leftOp = assignStmt.getLeftOp();
						if(leftOp instanceof FieldRef) {
							if(((FieldRef)leftOp).getField().equals(fieldRef.getField())) {
								Value fragmentValue = assignStmt.getRightOp();
								String fragmentClassName = fragmentValue.getType().toQuotedString();
								if(!fragmentClassName.equals("androidx.fragment.app.Fragment") 
										&& !fragmentClassName.equals("android.app.Fragment") 
										&& !fragmentClassName.equals("android.support.v4.app.Fragment")) {
									fragments.add(fragmentClassName);
								}else {
									if(fragmentValue instanceof Local) {
										findFragments((Local)fragmentValue, stmt, cfg, method.getSignature().equals(sm.getSignature()) ? caller : null);
									}else {
										System.out.println("DDD");
									}
								}
                            }
						}
					}
				}
			}
		}
		
		SootField field = fieldRef.getField();
		SootClass owner = field.getDeclaringClass();
		if(!owner.getName().equals(sm.getDeclaringClass().getName())) {
			for(SootMethod method : owner.getMethods()) {
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
						Stmt stmt = (Stmt)units.next();
						if(stmt instanceof AssignStmt && stmt.containsFieldRef()) {
							AssignStmt assignStmt = (AssignStmt) stmt;
							Value leftOp = assignStmt.getLeftOp();
							if(leftOp instanceof FieldRef) {
								if(((FieldRef)leftOp).getField().equals(fieldRef.getField())) {
									Value fragmentValue = assignStmt.getRightOp();
									String fragmentClassName = fragmentValue.getType().toQuotedString();
									if(!fragmentClassName.equals("androidx.fragment.app.Fragment") 
											&& !fragmentClassName.equals("android.app.Fragment") 
											&& !fragmentClassName.equals("android.support.v4.app.Fragment")) {
										fragments.add(fragmentClassName);
									}else {
										if(fragmentValue instanceof Local) {
											findFragments((Local)fragmentValue, stmt, cfg, method.getSignature().equals(sm.getSignature()) ? caller : null);
										}else {
											System.out.println("DDD");
										}
									}
	                            }
							}
						}
					}
				}
			}
		}
	}
}
