package zzg.staticanalysis.analyzer;

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
import soot.jimple.ClassConstant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.Utils;

public class IntentParser {
	private static final String TAG = "[IntentParser]";
	//Intent init method Signatrue
	public static final String INIT0 = "<android.content.Intent: void <init>()>";
	public static final String INIT1 = "<android.content.Intent: void <init>(android.content.Intent)>";
	public static final String INIT2 = "<android.content.Intent: void <init>(java.lang.String)>";
	public static final String INIT3 = "<android.content.Intent: void <init>(java.lang.String,android.net.Uri)>";
	public static final String INIT4 = "<android.content.Intent: void <init>(android.content.Context,java.lang.Class)>";
	public static final String INIT5 = "<android.content.Intent: void <init>(java.lang.String,android.net.Uri,android.content.Context,java.lang.Class)>";
	
	//action
	public static final String SETACTION = "<android.content.Intent: android.content.Intent setAction(java.lang.String)>";
	//category
	public static final String ADDCATEGORY = "<android.content.Intent: android.content.Intent addCategory(java.lang.String)>";
	//type
	public static final String SETDATAANDTYPE = "<android.content.Intent: android.content.Intent setDataAndType(android.net.Uri,java.lang.String)>";
	public static final String SETDATAANDTYPEANDNORMALIZE = "<android.content.Intent: android.content.Intent setDataAndTypeAndNormalize(android.net.Uri,java.lang.String)>";
	public static final String SETTYPE = "<android.content.Intent: android.content.Intent setType(java.lang.String)>";
	public static final String SETTYPEANDNORMALIZE = "<android.content.Intent: android.content.Intent setTypeAndNormalize(java.lang.String)>";
	
	//target class
	public static final String SETCLASS = "<android.content.Intent: android.content.Intent setClass(android.content.Context,java.lang.Class)>";
	public static final String SETCLASSNAME1 = "<android.content.Intent: android.content.Intent setClassName(java.lang.String,java.lang.String)>";
	public static final String SETCLASSNAME2 = "<android.content.Intent: android.content.Intent setClassName(android.content.Context,java.lang.String)>";
	
	public static final String SETCOMPONENT = "<android.content.Intent: android.content.Intent setComponent(android.content.ComponentName)>";
	
	//android.content.ComponentName
	private static final String COMPONENTNAME_INIT1 = "<android.content.ComponentName: void <init>(java.lang.String,java.lang.String)>";
	private static final String COMPONENTNAME_INIT2 = "<android.content.ComponentName: void <init>(android.content.Context,java.lang.String)>";
	private static final String COMPONENTNAME_INIT3 = "<android.content.ComponentName: void <init>(android.content.Context,java.lang.Class)>";
	//static method
	private static final String CREATERELATIVE1 = "<android.content.ComponentName: android.content.ComponentName createRelative(java.lang.String,java.lang.String)>";
	private static final String CREATERELATIVE2 = "<android.content.ComponentName: android.content.ComponentName createRelative(android.content.Context,java.lang.String)>";
	
	private static List<SootMethod> records = new ArrayList<SootMethod>();
	
	
	public static String getAction(Stmt stmt, UnitGraph cfg) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		Value actionValue = invokeExpr.getArg(0);
		if(actionValue instanceof StringConstant) {
			return ((StringConstant)actionValue).value;
		}else if(actionValue instanceof Local) {
			String action = findCommonStringValue(stmt, actionValue, cfg);
			Logger.i(TAG, "Action:\t>>>>"+action+"<<<<");
			return action;
		}
		return null;
	}

	public static String getTargetClassName(Stmt stmt, UnitGraph cfg) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		String mSignature = invokeExpr.getMethod().getSignature();
		Value classValue = null, stringClassValue = null;
		if(mSignature.equals(INIT4) || mSignature.equals(SETCLASS)) {
			classValue = invokeExpr.getArg(1);
		}else if(mSignature.equals(INIT5)) {
			classValue = invokeExpr.getArg(3);
		}else if(mSignature.equals(SETCLASSNAME1) || mSignature.equals(SETCLASSNAME2)) {
			stringClassValue = invokeExpr.getArg(1);
		}
		if(classValue != null) {
			if(classValue instanceof ClassConstant) {
				String classValueStr = ((ClassConstant) classValue).getValue();
				return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
			}else if(classValue instanceof Local) {
				String target = findClassValue(stmt, classValue, cfg);
				Logger.i(TAG, "TargetClassName(class):\t>>>>"+target+"<<<<");
				return target;
			}
		}
		if(stringClassValue != null) {
			if(stringClassValue instanceof StringConstant) {
				return ((StringConstant)stringClassValue).value;
			}else if(stringClassValue instanceof Local) {
				String target = findStringClassValue(stmt, stringClassValue, cfg);
				Logger.i(TAG, "TargetClassName(String):\t>>>>"+target+"<<<<");
				return target;
			}
		}
		return null;
	}

	public static String getCategory(Stmt stmt, UnitGraph cfg) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		Value categoryValue = invokeExpr.getArg(0);
		if(categoryValue instanceof StringConstant) {
			return ((StringConstant)categoryValue).value;
		}else if(categoryValue instanceof Local) {
			String category = findCommonStringValue(stmt, categoryValue, cfg);
			Logger.i(TAG, "Category:\t>>>>"+category+"<<<<");
			return category;
		}
		return null;
	}

	public static String getDataType(Stmt stmt, UnitGraph cfg) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		String mSignature = invokeExpr.getMethod().getSignature();
		Value dataTypeValue = null;
		if(mSignature.equals(SETDATAANDTYPE) || mSignature.equals(SETDATAANDTYPEANDNORMALIZE)) {
			dataTypeValue = invokeExpr.getArg(1);
		}else {
			dataTypeValue = invokeExpr.getArg(0);
		}
		if(dataTypeValue != null) {
			if(dataTypeValue instanceof StringConstant) {
				return ((StringConstant)dataTypeValue).value;
			}else if(dataTypeValue instanceof Local) {
				String dataType = findCommonStringValue(stmt, dataTypeValue, cfg);
				Logger.i(TAG, "DataType:\t>>>>"+dataType+"<<<<");
				return dataType;
			}
		}
		return null;
	}

	public static String getComponentName(Stmt stmt, UnitGraph cfg, Value componentValue) {
		SmartLocalDefs slds = new SmartLocalDefs(cfg, new SimpleLiveLocals(cfg));
		List<Unit> defs = slds.getDefsOfAt((Local)componentValue, stmt);
		for(Unit u : defs) {
			if(u instanceof DefinitionStmt) {
				DefinitionStmt def = (DefinitionStmt) u;
				System.out.println("def:\t"+def.toString());
				Value rightOp = def.getRightOp();
				if(def instanceof IdentityStmt && rightOp instanceof ParameterRef) {
					//componentValue := @paramterX: android.content.ComponentName
					int position = ((ParameterRef)rightOp).getIndex();
					return findComponentNameFromParam(cfg.getBody().getMethod(), position);
				}else if(def instanceof AssignStmt) {
					if(def.containsInvokeExpr()) {
						InvokeExpr invokeExpr = def.getInvokeExpr();
						SootMethod invokee = invokeExpr.getMethod();
						String signature = invokee.getSignature();
						Value curValue = invokeExpr.getArg(1);
						if(signature.equals(COMPONENTNAME_INIT1) || signature.equals(COMPONENTNAME_INIT2)) {
							Value stringClassValue = curValue;
							if(stringClassValue instanceof StringConstant) {
								return ((StringConstant)stringClassValue).value;
							}else if(stringClassValue instanceof Local) {
								String target = findStringClassValue(def, stringClassValue, cfg);
								Logger.i(TAG, "ComponentName:\t>>>>"+target+"<<<<");
								return target;
							}
						}else if(signature.equals(COMPONENTNAME_INIT3)) {
							Value classValue = curValue;
							if(classValue instanceof ClassConstant) {
								String classValueStr = ((ClassConstant) classValue).getValue();
								return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
							}else if(classValue instanceof Local) {
								String target = findClassValue(def, classValue, cfg);
								Logger.i(TAG, "ComponentName:\t>>>>"+target+"<<<<");
								return target;
							}
						}else if(signature.equals(CREATERELATIVE1) || signature.equals(CREATERELATIVE2)) {
							Value stringClassValue = curValue;
							if(stringClassValue instanceof StringConstant) {
								return ((StringConstant)stringClassValue).value;
							}else if(stringClassValue instanceof Local) {
								String target = findStringClassValue(def, stringClassValue, cfg);
								Logger.i(TAG, "ComponentName:\t>>>>"+target+"<<<<");
								return target;
							}
						}else {
							if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
									&& !invokee.getName().equals(cfg.getBody().getMethod().getName())) {
								//componentValue = $r1.<className: android.content.ComponentName methodName(params)>(args)
								Body body = null;
								try {
									body = invokee.retrieveActiveBody();
								} catch (RuntimeException e) {
									Logger.e(TAG, new ActiveBodyNotFoundException(e));
								}
								if(body != null) {
									UnitGraph invokeeCfg = new BriefUnitGraph(body);
									for(Unit tail : invokeeCfg.getTails()) {
										if(tail instanceof ReturnStmt) {
											Value newValue = ((ReturnStmt) tail).getOp();
											if(newValue instanceof Local) {
												return getComponentName((Stmt)tail, invokeeCfg, newValue);
											}else if(newValue instanceof StringConstant) {
												return ((StringConstant) newValue).value;
											}
										}
									}
								}
							}
						}
					}else if(def.containsFieldRef()) {
						//componentValue = $r1.<className: android.content.ComponentName componentField>
						FieldRef fieldRef = (FieldRef)rightOp;
						return findComponentNameFromField(cfg.getBody().getMethod(), fieldRef);
					}else {
						Logger.i("TEST", "findStringClassValue: other situations: ["+def.toString()+"]");
					}
				}
			}
		}
		return null;
	}
	
	private static String findComponentNameFromParam(SootMethod sm, int position) {
		Iterator<soot.jimple.toolkits.callgraph.Edge> edges = AppParser.v().getCg().edgesInto(sm);
		if(!edges.hasNext()) {
			StringBuffer sb = new StringBuffer();
			sb.append("Method[").append(sm.getSignature()).append("] haven't caller. Find target fail.");
			Logger.e(TAG, new RuntimeException(sb.toString()));
			return null;
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
							Value curValue = invokeExpr.getArg(position);
							return getComponentName(stmt, cfg, curValue);
						}
					}
				}
			}
		}
		return null;
	}
	
	private static String findComponentNameFromField(SootMethod sm, FieldRef fieldRef) {
		for(SootMethod method : sm.getDeclaringClass().getMethods()) {
			if(!method.equals(sm) && !records.contains(method)) {
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
									if(curValue instanceof Local)
										return getComponentName(assignStmt, cfg, curValue);
									else if(curValue instanceof StringConstant)
										return ((StringConstant) curValue).value;
	                            }
							}
						}
					}
				}
			}
		}
		SootField field = fieldRef.getField();
		SootClass owner = field.getDeclaringClass();
		for(SootMethod method : owner.getMethods()) {
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
								if(curValue instanceof Local)
									return getComponentName(assignStmt, extraCfg, curValue);
								else if(curValue instanceof StringConstant)
									return ((StringConstant) curValue).value;
                            }
						}
					}
				}
			}
		}


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
									if(curValue instanceof Local)
										return getComponentName(assignStmt, extraCfg, curValue);
									else if(curValue instanceof StringConstant)
										return ((StringConstant) curValue).value;
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
										if(curValue instanceof Local)
											return getComponentName(assignStmt, extraCfg, curValue);
										else if(curValue instanceof StringConstant)
											return ((StringConstant) curValue).value;
		                            }
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	public static String findCommonStringValue(Stmt stmt, Value stringValue, UnitGraph cfg) {
		SmartLocalDefs slds = new SmartLocalDefs(cfg, new SimpleLiveLocals(cfg));
		List<Unit> defs = slds.getDefsOfAt((Local)stringValue, stmt);
		for(Unit u : defs) {
			if(u instanceof DefinitionStmt) {
				DefinitionStmt def = (DefinitionStmt) u;
				Value rightOp = def.getRightOp();
				if(def instanceof IdentityStmt && rightOp instanceof ParameterRef) {
					//stringValue := @paramterX: java.lang.String
					int position = ((ParameterRef)rightOp).getIndex();
					return getCommonStringValueFromParam(cfg.getBody().getMethod(), position);
				}else if(def instanceof AssignStmt) {
					if(def.containsInvokeExpr()) {
						InvokeExpr invokeExpr = def.getInvokeExpr();
						SootMethod invokee = invokeExpr.getMethod();
						if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
								&& !invokee.getName().equals(cfg.getBody().getMethod().getName())) { 
							//stringValue = $r1.<className: java.lang.String methodName(params)>(args)
							Body body = null;
							try {
								body = invokee.retrieveActiveBody();
							} catch (RuntimeException e) {
								Logger.e(TAG, new ActiveBodyNotFoundException(e));
							}
							if(body != null) {
								UnitGraph invokeeCfg = new BriefUnitGraph(body);
								for(Unit tail : invokeeCfg.getTails()) {
									if(tail instanceof ReturnStmt) {
										Value curValue = ((ReturnStmt) tail).getOp();
										if(curValue.getType().toQuotedString().equals("java.lang.String")) {
											if(curValue instanceof Local)
												return findCommonStringValue((Stmt)tail, curValue, invokeeCfg);
											else if(curValue instanceof StringConstant) {
												return ((StringConstant) curValue).value;
											}
										}
									}
								}
							}
						}
					}else if(def.containsFieldRef()) {
						//stringValue = $r1.<className: java.lang.String stringField>
						FieldRef fieldRef = (FieldRef)rightOp;
						return getCommonStringValueFromField(cfg.getBody().getMethod(), fieldRef);
					}else {
						Logger.i("TEST", "findStringClassValue: other situations: ["+def.toString()+"]");
					}
				}
			}
		}
		return null;
	}
	
	private static String getCommonStringValueFromParam(SootMethod sm, int position) {
		Iterator<soot.jimple.toolkits.callgraph.Edge> edges = AppParser.v().getCg().edgesInto(sm);
		if(!edges.hasNext()) {
			StringBuffer sb = new StringBuffer();
			sb.append("Method[").append(sm.getSignature()).append("] haven't caller. Find target fail.");
			Logger.e("", new RuntimeException(sb.toString()));
			return null;
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
							Value classValue = invokeExpr.getArg(position);
							return findCommonStringValue(stmt, classValue, cfg);
						}
					}
				}
			}
		}
		return null;
	}
	
	private static String getCommonStringValueFromField(SootMethod sm, FieldRef fieldRef) {
		for(SootMethod method : sm.getDeclaringClass().getMethods()) {
			if(!method.equals(sm) && !records.contains(method)) {
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
									if(curValue instanceof Local)
										return findCommonStringValue(assignStmt, curValue, cfg);
									else if(curValue instanceof StringConstant)
										return ((StringConstant) curValue).value;
	                            }
							}
						}
					}
				}
			}
		}
		SootField field = fieldRef.getField();
		SootClass owner = field.getDeclaringClass();
		for(SootMethod method : owner.getMethods()) {
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
								if(curValue instanceof Local)
									return findCommonStringValue(assignStmt, curValue, extraCfg);
								else if(curValue instanceof StringConstant)
									return ((StringConstant) curValue).value;
                            }
						}
					}
				}
			}
		}


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
									if(curValue instanceof Local)
										return findCommonStringValue(assignStmt, curValue, extraCfg);
									else if(curValue instanceof StringConstant)
										return ((StringConstant) curValue).value;
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
										if(curValue instanceof Local)
											return findCommonStringValue(assignStmt, curValue, extraCfg);
										else if(curValue instanceof StringConstant)
											return ((StringConstant) curValue).value;
		                            }
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public static String findClassValue(Stmt stmt, Value classValue, UnitGraph cfg) {
		SmartLocalDefs slds = new SmartLocalDefs(cfg, new SimpleLiveLocals(cfg));
		List<Unit> defs = slds.getDefsOfAt((Local)classValue, stmt);
		for(Unit u : defs) {
			if(u instanceof DefinitionStmt) {
				DefinitionStmt def = (DefinitionStmt) u;
				Value rightOp = def.getRightOp();
				if(def instanceof IdentityStmt && rightOp instanceof ParameterRef) {
					//classValue := @paramterX: java.lang.Class
					int position = ((ParameterRef)rightOp).getIndex();
					return getClassValueFromParam(cfg.getBody().getMethod(), position);
				}else if(def instanceof AssignStmt) {
					if(def.containsInvokeExpr()) {
						InvokeExpr invokeExpr = def.getInvokeExpr();
						SootMethod invokee = invokeExpr.getMethod();
						if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
								&& !invokee.getName().equals(cfg.getBody().getMethod().getName())) {
							//classValue = $r1.<className: java.lang.Class methodName(params)>(args)
							Body body = null;
							try {
								body = invokee.retrieveActiveBody();
							} catch (RuntimeException e) {
								Logger.e(TAG, new ActiveBodyNotFoundException(e));
							}
							if(body != null) {
								UnitGraph invokeeCfg = new BriefUnitGraph(body);
								for(Unit tail : invokeeCfg.getTails()) {
									if(tail instanceof ReturnStmt) {
										Value curValue = ((ReturnStmt) tail).getOp();
										if(curValue.getType().toQuotedString().equals("java.lang.Class")) {
											if(curValue instanceof Local) {
												return findClassValue((Stmt)tail, curValue, invokeeCfg);
											}else if(curValue instanceof ClassConstant) {
												String classValueStr = ((ClassConstant) curValue).getValue();
												return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
											}
										}
									}
								}
							}
						}
					}else if(def.containsFieldRef()) {
						//classValue = $r1.<className: java.lang.Class classField>
						FieldRef fieldRef = (FieldRef)rightOp;
						return getClassValueFromField(cfg.getBody().getMethod(), fieldRef);
						
					}else {
						if(rightOp instanceof ClassConstant) {
							String classValueStr = ((ClassConstant) rightOp).getValue();
							return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
						}
					}
				}
			}
		}
		return null;
	}
	
	private static String getClassValueFromParam(SootMethod sm, int position) {
		Iterator<soot.jimple.toolkits.callgraph.Edge> edges = AppParser.v().getCg().edgesInto(sm);
		if(!edges.hasNext()) {
			StringBuffer sb = new StringBuffer();
			sb.append("Method[").append(sm.getSignature()).append("] haven't caller. Find target fail.");
			Logger.e("", new RuntimeException(sb.toString()));
			return null;
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
							Value classValue = invokeExpr.getArg(position);
							if(classValue instanceof ClassConstant) {
								String classValueStr = ((ClassConstant) classValue).getValue();
								return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
							}
							else
								return findClassValue(stmt, classValue, cfg);
						}
					}
				}
			}
		}
		return null;
	}
	
	private static String getClassValueFromField(SootMethod sm, FieldRef fieldRef) {
		for(SootMethod method : sm.getDeclaringClass().getMethods()) {
			if(!method.equals(sm) && !records.contains(method)) {
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
									if(curValue instanceof Local)
										return findClassValue(assignStmt, curValue, cfg);
									else if(curValue instanceof ClassConstant) {
										String classValueStr = ((ClassConstant) curValue).getValue();
										return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
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
		for(SootMethod method : owner.getMethods()) {
			if(records.contains(method))
				continue;
			records.add(method);
			Body body = null;
			try {
				body = method.retrieveActiveBody();
			}catch (RuntimeException e) {
				Logger.e("", e);
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
								if(curValue instanceof Local)
									return findClassValue(assignStmt, curValue, extraCfg);
								else if(curValue instanceof ClassConstant) {
									String classValueStr = ((ClassConstant) curValue).getValue();
									return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
								}
                            }
						}
					}
				}
			}
		}

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
					Logger.e("", e);
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
									if(curValue instanceof Local)
										return findClassValue(assignStmt, curValue, extraCfg);
									else if(curValue instanceof ClassConstant) {
										String classValueStr = ((ClassConstant) curValue).getValue();
										return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
									}
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
										if(curValue instanceof Local)
											return findClassValue(assignStmt, curValue, extraCfg);
										else if(curValue instanceof ClassConstant) {
											String classValueStr = ((ClassConstant) curValue).getValue();
											return classValueStr.substring(1, classValueStr.length()-1).replace("/", ".");
										}
		                            }
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public static String findStringClassValue(Stmt stmt, Value stringClassValue, UnitGraph cfg) {
		SmartLocalDefs slds = new SmartLocalDefs(cfg, new SimpleLiveLocals(cfg));
		List<Unit> defs = slds.getDefsOfAt((Local)stringClassValue, stmt);
		for(Unit u : defs) {
			if(u instanceof DefinitionStmt) {
				DefinitionStmt def = (DefinitionStmt) u;
				Value rightOp = def.getRightOp();
				if(def instanceof IdentityStmt && rightOp instanceof ParameterRef) {
					//stringClassValue := @paramterX: java.lang.String
					int position = ((ParameterRef)rightOp).getIndex();
					return getStringClassValueFromParam(cfg.getBody().getMethod(), position);
				}else if(def instanceof AssignStmt) {
					if(def.containsInvokeExpr()) {
						InvokeExpr invokeExpr = def.getInvokeExpr();
						SootMethod invokee = invokeExpr.getMethod();
						if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
								&& !invokee.getName().equals(cfg.getBody().getMethod().getName())) {
							//stringClassValue = $r1.<className: java.lang.String methodName(params)>(args)
							Body body = null;
							try {
								body = invokee.retrieveActiveBody();
							} catch (RuntimeException e) {
								Logger.e(TAG, new ActiveBodyNotFoundException(e));
							}
							if(body != null) {
								UnitGraph invokeeCfg = new BriefUnitGraph(body);
								for(Unit tail : invokeeCfg.getTails()) {
									if(tail instanceof ReturnStmt) {
										Value curValue = ((ReturnStmt) tail).getOp();
										if(curValue instanceof Local) {
											return findStringClassValue((Stmt)tail, curValue, invokeeCfg);
										}else if(curValue instanceof StringConstant) {
											return ((StringConstant) curValue).value;
										}
									}
								}
							}
						}else if(invokee.getSignature().equals("<java.lang.Class: java.lang.String getName()>")) {
							Value classValue = ((InstanceInvokeExpr)invokeExpr).getBase();
							if(classValue instanceof Local) {
								return findClassValue(def, classValue, cfg);
							}
						}
					}else if(def.containsFieldRef()) {
						//stringClassValue = $r1.<className: java.lang.String stringClassField>
						FieldRef fieldRef = (FieldRef)rightOp;
						return getStringClassValueFromField(cfg.getBody().getMethod(), fieldRef);
						
					}else {
						Logger.i("TEST", "findStringClassValue: other situations: ["+def.toString()+"]");
					}
				}
			}
		}
		return null;
	}
	
	private static String getStringClassValueFromParam(SootMethod sm, int position) {
		Iterator<soot.jimple.toolkits.callgraph.Edge> edges = AppParser.v().getCg().edgesInto(sm);
		if(!edges.hasNext()) {
			StringBuffer sb = new StringBuffer();
			sb.append("Method[").append(sm.getSignature()).append("] haven't caller. Find target fail.");
			Logger.e("", new RuntimeException(sb.toString()));
			return null;
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
							Value stringClassValue = invokeExpr.getArg(position);
							return findStringClassValue(stmt, stringClassValue, cfg);
						}
					}
				}
			}
		}
		return null;
	}
	
	private static String getStringClassValueFromField(SootMethod sm, FieldRef fieldRef) {
		for(SootMethod method : sm.getDeclaringClass().getMethods()) {
			if(!method.equals(sm) && !records.contains(method)) {
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
									if(curValue instanceof Local)
										return findStringClassValue(assignStmt, curValue, cfg);
									else if(curValue instanceof StringConstant)
										return ((StringConstant) curValue).value;
	                            }
							}
						}
					}
				}
			}
		}
		SootField field = fieldRef.getField();
		SootClass owner = field.getDeclaringClass();
		for(SootMethod method : owner.getMethods()) {
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
								if(curValue instanceof Local)
									return findStringClassValue(assignStmt, curValue, extraCfg);
								else if(curValue instanceof StringConstant)
									return ((StringConstant) curValue).value;
                            }
						}
					}
				}
			}
		}
		
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
									if(curValue instanceof Local)
										return findStringClassValue(assignStmt, curValue, extraCfg);
									else if(curValue instanceof StringConstant)
										return ((StringConstant) curValue).value;
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
										if(curValue instanceof Local)
											return findStringClassValue(assignStmt, curValue, extraCfg);
										else if(curValue instanceof StringConstant)
											return ((StringConstant) curValue).value;
		                            }
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
}
