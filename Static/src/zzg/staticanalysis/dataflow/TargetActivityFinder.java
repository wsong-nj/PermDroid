package zzg.staticanalysis.dataflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.analyzer.IntentParser;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.Utils;

/**
 * This is a standard dataflow analysis procedure.
 * There are four cases:
 * Case 1: fromReturn
 * 		targetValue = $rx.<className: returnType methodName(param...)>(arg...)
 * 			need to find ReturnStmt in methodName, and forward dataflow from ReturnStmt.
 * Case 2: fromParam
 * 		targetValue := @paramterX: parameterType
 * 			need to find the caller of this method, and forward dataflow from invokeStmt.
 * Case 3: fromField
 * 		targetValue = $rx.<className: fieldType fieldName>
 * 			need to find the AssignStmt of field. backward dataflow all methods.
 * Case 4: fromTransferParam
 * 		$rx.<className: returnType methodName(param...)>(arg...) && targetValue ∈ arg
 * 			backward dataflow methodName.
 * @author dell
 *
 */
public class TargetActivityFinder {
	private static final String TAG = "[TargetActivityFinder]";
	
	public static String find(Stmt startActivityInvokeStmt, UnitGraph cfg, Value intentValue, SootMethod caller) {
		Logger.i(TAG, "----Strat to collect intent info and fing target activity for:");
		Logger.i(TAG, "--------Stmt: " + startActivityInvokeStmt.toString());
		Logger.i(TAG, "--------Method: " + cfg.getBody().getMethod().getSignature());
		Logger.i(TAG, "--------Caller: " + (caller == null ? "null" : caller.getSignature()));
		String result = new TargetActivityFinder().start(startActivityInvokeStmt, cfg, intentValue, caller);
		Logger.i(TAG, "----End");
		return result;
	}
	
	private String targetActivity;
	private boolean finish = false;
	private List<String> actions = new ArrayList<String>();
	private List<String> types = new ArrayList<String>();
	private Set<String> categories = new HashSet<String>();
	
	private Set<SootMethod> records = new HashSet<SootMethod>();
	
	private String start(Stmt stmt, UnitGraph cfg, Value intentValue, SootMethod caller) {
		forward(stmt, cfg, intentValue, caller);
		if(targetActivity != null)
			return targetActivity;
		if(!actions.isEmpty()) {
			String type = types.isEmpty() ? null : types.get(0);
			String action = actions.get(0);
			targetActivity = AppParser.v().getActivityByImplicitIntent(action, type, categories);
		}
		return targetActivity;
	}
	
	private void forward_(Stmt stmt, UnitGraph cfg, Value curValue, SootMethod caller, Set<Stmt> stmtRecords, Stmt dataflowStmt) {
		List<Unit> preds = cfg.getPredsOf(stmt);
		for(Unit pred : preds) {
			Stmt curStmt = (Stmt)pred;
			if(stmtRecords.add(curStmt)) {
				if(curStmt.containsInvokeExpr()) {
					InvokeExpr invokeExpr = curStmt.getInvokeExpr();
					SootMethod invokee = invokeExpr.getMethod();
					unitAnalysis(curStmt, curValue, cfg, caller);
					if(targetActivity != null || invokee.getSignature().equals(IntentParser.INIT2) 
							|| invokee.getSignature().equals(IntentParser.INIT3)
							|| invokee.getSignature().equals(IntentParser.INIT0)) {
						//前向分析，分析到Intent的初始化方法或者得到结果就结束了
						if(targetActivity == null)
							Logger.i(TAG, "--------end with init-method: " + curStmt.toString());
						else
							Logger.i(TAG, "--------end with target: " + targetActivity);
						finish = true;
						return;
					}
					if(invokeExpr instanceof InstanceInvokeExpr) {
						Value invokerValue = ((InstanceInvokeExpr)invokeExpr).getBase();
						if(invokerValue.equivTo(curValue)) {
							String signature = invokee.getSignature();
							if(signature.equals(IntentParser.INIT1)) {
								curValue = invokeExpr.getArg(0);
								forward_(curStmt, cfg, curValue, caller, stmtRecords, dataflowStmt);
								if(finish)
									return;
								//这个方法是用另一个Intent对当前Intent进行赋值，所以要继续追溯那个Intent
							}
						}
					}
				}
				if(curStmt instanceof DefinitionStmt) {
					Value left = ((DefinitionStmt) curStmt).getLeftOp();
					if(left.equivTo(curValue)) {
						dataflowStmt = curStmt;
						Value right = ((DefinitionStmt) curStmt).getRightOp();
						if(right instanceof Local) {
							curValue = right;
						}
					}
				}
				forward_(curStmt, cfg, curValue, caller, stmtRecords, dataflowStmt);
				if(finish)
					return;
			}
		}
		//intent := @paramterX: parameterType
		if(dataflowStmt instanceof IdentityStmt) {
			IdentityStmt identityStmt = (IdentityStmt)dataflowStmt;
			Value leftOp = identityStmt.getLeftOp();
			if(leftOp.equivTo(curValue)) {
				Value rightOp = identityStmt.getRightOp();
				if(rightOp instanceof ParameterRef) {
					int position = ((ParameterRef)rightOp).getIndex();
					fromParam(cfg.getBody().getMethod(), position, caller);
					return;
				}
			}
		}
		//intent = $r1.<className: fieldType fieldName>
		else if(dataflowStmt instanceof AssignStmt && dataflowStmt.containsFieldRef()) {
			AssignStmt assignStmt = (AssignStmt)dataflowStmt;
			Value leftOp = assignStmt.getLeftOp();
			if(leftOp.equivTo(curValue)) {
				Value rightOp = assignStmt.getRightOp();
				if(rightOp instanceof FieldRef) {
					FieldRef fieldRef = (FieldRef)rightOp;
					fromField(cfg.getBody().getMethod(), fieldRef, caller);
					return;
				}
			}
		}
	}
	
	private void forward(Stmt stmt, UnitGraph cfg, Value intentValue, SootMethod caller) {
		Logger.i(TAG, "------------Forward Dataflow");
		SootMethod sm = cfg.getBody().getMethod();
		records.add(sm);
		Stmt curStmt = stmt, dataflowStmt = stmt;
		Value curValue = intentValue;
		
		forward_(curStmt, cfg, curValue, caller, new HashSet<Stmt>(), dataflowStmt);
		
		//这里可以改，我只考虑一条控制流
//		while(!cfg.getPredsOf(curStmt).isEmpty()) {
//			curStmt = (Stmt)cfg.getPredsOf(curStmt).get(0);
//			if(curStmt instanceof DefinitionStmt) {
//				Value left = ((DefinitionStmt) curStmt).getLeftOp();
//				if(left.equivTo(curValue)) {
//					dataflowStmt = curStmt;
//					Value right = ((DefinitionStmt) curStmt).getRightOp();
//					if(right instanceof Local) {
//						curValue = right;
//					}
//				}
//			}
//			if(curStmt.containsInvokeExpr()) {
//				InvokeExpr invokeExpr = curStmt.getInvokeExpr();
//				SootMethod invokee = invokeExpr.getMethod();
//				if(invokeExpr instanceof InstanceInvokeExpr) {
//					Value invokerValue = ((InstanceInvokeExpr)invokeExpr).getBase();
//					if(invokerValue.equivTo(curValue)) {
//						String signature = invokee.getSignature();
//						if(signature.equals(IntentParser.INIT1)) {
//							curValue = invokeExpr.getArg(0);
//							continue;
//							//这个方法是用另一个Intent对当前Intent进行赋值，所以要继续追溯那个Intent
//						}
//					}
//				}
//				unitAnalysis(curStmt, curValue, cfg, caller);
//				if(targetActivity != null || invokee.getSignature().equals(IntentParser.INIT2) 
//						|| invokee.getSignature().equals(IntentParser.INIT3)
//						|| invokee.getSignature().equals(IntentParser.INIT0)) {
//					//前向分析，分析到Intent的初始化方法或者得到结果就结束了
//					if(targetActivity == null)
//						Logger.i(TAG, "--------end with init-method");
//					else
//						Logger.i(TAG, "--------end with target: "+targetActivity);
//					finish = true;
//					return;
//				}
//				
//			}
//		}
//		//intent := @paramterX: parameterType
//		if(dataflowStmt instanceof IdentityStmt) {
//			IdentityStmt identityStmt = (IdentityStmt)dataflowStmt;
//			Value leftOp = identityStmt.getLeftOp();
//			if(leftOp.equivTo(curValue)) {
//				Value rightOp = identityStmt.getRightOp();
//				if(rightOp instanceof ParameterRef) {
//					int position = ((ParameterRef)rightOp).getIndex();
//					fromParam(sm, position, caller);
//				}
//			}
//		}
//		//intent = $r1.<className: fieldType fieldName>
//		else if(dataflowStmt instanceof AssignStmt && dataflowStmt.containsFieldRef()) {
//			AssignStmt assignStmt = (AssignStmt)dataflowStmt;
//			Value leftOp = assignStmt.getLeftOp();
//			if(leftOp.equivTo(curValue)) {
//				Value rightOp = assignStmt.getRightOp();
//				if(rightOp instanceof FieldRef) {
//					FieldRef fieldRef = (FieldRef)rightOp;
//					fromField(sm, fieldRef, caller);
//				}
//			}
//		}
	}
	
	private void backward(Stmt stmt, UnitGraph cfg, Value curValue, SootMethod caller) {
		Logger.i(TAG, "------------Backward Dataflow");
		records.add(cfg.getBody().getMethod());
		Set<Stmt> stmtRecords = new HashSet<Stmt>();
		
		backward_(stmt, cfg, curValue, caller, stmtRecords, null, null);
		
		for(Stmt s : stmtRecords)
			System.out.println(s.toString());
	}
	
	private void backward_(Stmt stmt, UnitGraph cfg, Value curValue, SootMethod caller, Set<Stmt> stmtRecords, Stmt init1Stmt, Value init1Value) {
		List<Unit> succs = cfg.getSuccsOf(stmt);
		for(Unit succ : succs) {
			Stmt curStmt = (Stmt) succ;
			if(stmtRecords.add(curStmt)) {
				if(curStmt.containsInvokeExpr()) {
					InvokeExpr invokeExpr = curStmt.getInvokeExpr();
					SootMethod invokee = invokeExpr.getMethod();
					if(invokeExpr instanceof SpecialInvokeExpr) {
						SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr)invokeExpr;
						Value invokerValue = specialInvokeExpr.getBase();
						if(invokerValue.equivTo(curValue)) {
							String signature = invokee.getSignature();
							if(signature.equals(IntentParser.INIT1)) {
								init1Stmt = curStmt;
								init1Value = specialInvokeExpr.getArg(0);
							}
						}
					}
					unitAnalysis(curStmt, curValue, cfg, caller);
					if(finish) {
						if(targetActivity == null && init1Stmt != null) {
							forward(init1Stmt, cfg, init1Value, caller);
						}
						return;
					}
				}
				backward_(curStmt, cfg, curValue, caller, stmtRecords, init1Stmt, init1Value);
			}
		}
	}
	
	//情况1： Intent是其他方法的返回值（向前）
	private void fromReturn(SootMethod sm, SootMethod caller) {
		Logger.i(TAG, "------------Find target Activity from other method return: "+sm.getSignature());
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
				forward(returnStmt, cfg, returnValue, caller);
				return;
			}
		}
	}
	
	/**
	 * 情况2： Intent来自参数（向前）
	 * 这个方法目的是要找到调用sm的方法，如果caller已知，则不用在callgraph找了
	 * @param sm
	 * @param position
	 * @param caller
	 */
	private void fromParam(SootMethod sm, int position, SootMethod caller) {
		Logger.i(TAG, "------------Find target Activity from param: "+sm.getSignature());
		if(caller != null) {
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
							Value intentValue = invokeExpr.getArg(position);
							forward(stmt, cfg, intentValue, null);
							return;
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
									Value intentValue = invokeExpr.getArg(position);
									forward(stmt, cfg, intentValue, null);
									if(finish)
										return;
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 情况3： Intent是某个对象的域（向后）
	 * 这个方法是找到，谁给域赋了什么值
	 * @param sm
	 * @param fieldRef
	 * @param caller
	 */
	private void fromField(SootMethod sm, FieldRef fieldRef, SootMethod caller) {
		Logger.i(TAG, "------------Find target Activity from field: "+fieldRef.toString());
		//首先在方法sm所在的类的其他方法找
		for(SootMethod method : sm.getDeclaringClass().getMethods()) {
//			if(!method.equals(sm) && !records.contains(method)) {
//			}
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
						AssignStmt assignStmt = (AssignStmt) curStmt;
						Value leftOp = assignStmt.getLeftOp();
						if(leftOp instanceof FieldRef) {
							if(((FieldRef)leftOp).getField().equals(fieldRef.getField())) {
								Value curValue = assignStmt.getRightOp();
								backward(curStmt, cfg, curValue, method.getSignature().equals(sm.getSignature()) ? caller : null);
								if(finish)
									return;
							}
						}
					}
				}
			}
		}
		//然后，如果那个域的类不是方法sm所在的类，则找那个域所在的类
		SootField field = fieldRef.getField();
		SootClass owner = field.getDeclaringClass();
		if(!owner.getName().equals(sm.getDeclaringClass().getName())) {
			for(SootMethod method : owner.getMethods()) {
//				if(records.contains(method))
//					continue;
//				records.add(method);
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
								if(fieldRef.getField().equals(((FieldRef)leftOp).getField())) {
									Value curValue = assignStmt.getRightOp();
									backward(curStmt, extraCfg, curValue, null);
									if(finish)
										return;
	                            }
							}
						}
					}
				}
			}
		}
		
		boolean isProtected = field.isProtected();
		List<SootClass> innerClasses = new ArrayList<SootClass>();//owner的内部类
		List<SootClass> subClasses = new ArrayList<SootClass>();//owner的子类
		List<SootClass> pkgClasses = new ArrayList<SootClass>();//owner的同包类
		List<SootClass> others = new ArrayList<SootClass>();
		
		Utils.getRelatedClasses(owner, innerClasses, subClasses, pkgClasses, others, isProtected);
		
		Set<SootClass> relatedClasses = new HashSet<SootClass>();
		if(field.isPrivate()) {//private 内部类
			relatedClasses.addAll(innerClasses);
		}else if(!field.isPrivate() && !field.isProtected() && !field.isPublic()) {//default +包类
			relatedClasses.addAll(innerClasses);
			relatedClasses.addAll(pkgClasses);
		}else if(field.isProtected()) {//protected +子类
			relatedClasses.addAll(innerClasses);
			relatedClasses.addAll(pkgClasses);
			relatedClasses.addAll(innerClasses);
		}else{//public 太广了，在前面的找不到之后，再找其他
		}
		for(SootClass sc : relatedClasses) {
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
							AssignStmt assignStmt = (AssignStmt) curStmt;
							Value leftOp = assignStmt.getLeftOp();
							if(leftOp instanceof FieldRef) {
								if(fieldRef.getField().equals(((FieldRef)leftOp).getField())) {
									Value curValue = assignStmt.getRightOp();
									backward(curStmt, extraCfg, curValue, null);
									if(finish)
										return;
	                            }
							}
						}
					}
				}
			}
		}
		if(field.isPublic()) {
			for(SootClass sc : others) {
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
								AssignStmt assignStmt = (AssignStmt) curStmt;
								Value leftOp = assignStmt.getLeftOp();
								if(leftOp instanceof FieldRef) {
									if(fieldRef.getField().equals(((FieldRef)leftOp).getField())) {
										Value curValue = assignStmt.getRightOp();
										backward(curStmt, extraCfg, curValue, null);
										if(finish)
											return;
		                            }
								}
							}
						}
					}
				}
			}
		}
	}
	
	//情况4： Intent被传进某个方法里，在该方法中进行填充（向后）
	private void fromTransferParam(SootMethod sm, int position, SootMethod caller) {
		Logger.i(TAG, "------------Find target Activity from Transfer Param: "+sm.getSignature());
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
				backward(curStmt, cfg, curValue, caller);
				if(finish)
					return;
			}
		}
	}
	
	
	private void unitAnalysis(Stmt curStmt, Value curValue, UnitGraph cfg, SootMethod caller) {
		InvokeExpr invokeExpr = curStmt.getInvokeExpr();
		SootMethod invokee = invokeExpr.getMethod();
		
		//如果这条语句调用的方法是应用的方法，且之前没分析过
		//那么会有两种可能：1.把Intent作为参数传进该方法，在该方法中对Intent进行一系列赋值； 2.Intent是该方法的返回值，在该方法中被赋值
		if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
				&& !records.contains(invokee)) {
			//判断这个方法是否把Intent作为参数传进去
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
				//情况1： <className: returnType methodName(params)>(args, intent, args)
				fromTransferParam(invokee, index, cfg.getBody().getMethod());
				if(finish)
					return;
			}else if(curStmt instanceof AssignStmt) {
				//情况2： intent = $r1.<className: returnType methodName(params)>(args)
				Value left = ((AssignStmt) curStmt).getLeftOp();
				if(left.equivTo(curValue)) {
					fromReturn(invokee, cfg.getBody().getMethod());
					if(finish)
						return;
				}
			}
		}
		
		if(invokeExpr instanceof InstanceInvokeExpr) {
			Value invokerValue = ((InstanceInvokeExpr)invokeExpr).getBase();
			if(invokerValue.equivTo(curValue)) {
				String signature = invokee.getSignature();
				switch (signature) {
					//target class
					case IntentParser.INIT4:
					case IntentParser.INIT5:
					case IntentParser.SETCLASS:
					case IntentParser.SETCLASSNAME1:
					case IntentParser.SETCLASSNAME2:
						finish = true;
						targetActivity = IntentParser.getTargetClassName(curStmt, cfg);
						return;
					//action
					case IntentParser.INIT2:
					case IntentParser.INIT3:
						finish = true;
					case IntentParser.SETACTION:
						String action_ = IntentParser.getAction(curStmt, cfg);
						if(action_ != null)
							actions.add(action_);
						return;
					//category
					case IntentParser.ADDCATEGORY:
						String category_ = IntentParser.getCategory(curStmt, cfg);
						if(category_ != null)
							categories.add(category_);
						return;
					//data type
					case IntentParser.SETDATAANDTYPE:
					case IntentParser.SETDATAANDTYPEANDNORMALIZE:
					case IntentParser.SETTYPE:
					case IntentParser.SETTYPEANDNORMALIZE:
						String dataType_ = IntentParser.getDataType(curStmt, cfg);
						if(dataType_ != null)
							types.add(dataType_);
						return;
					//ComponentName
					case IntentParser.SETCOMPONENT:
						Value componentValue = curStmt.getInvokeExpr().getArg(0);
						targetActivity = IntentParser.getComponentName(curStmt, cfg, componentValue);
						finish = true;
						return;
					default:
						break;
				}
			}
		}
	}
}
