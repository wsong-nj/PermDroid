package zzg.staticanalysis.analyzer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.staticTepOne;
import zzg.staticanalysis.utils.ClassService;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.MethodService;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.Widget;

public class ThreadAnalyzer {
	private static final String TAG = "[ThreadAnalyzer]";
	//sub-signature
	public static final String VIEWPOST = "boolean post(java.lang.Runnable)";
	public static final String VIEWPOSTDELAYED = "boolean postDelayed(java.lang.Runnable,long)";
	public static final String ACTIVITYRUNONUITHREAD = "void runOnUiThread(java.lang.Runnable)";
	
	public static final String START = "void start()";
	
	public static final String EXECUTE = "android.os.AsyncTask execute(java.lang.Object[])";
	public static final String EXECUTEONEXECUTER = "android.os.AsyncTask executeOnExecutor(java.util.concurrent.Executor,java.lang.Object[])";
	//signature
	public static final String THREADSTART = "<java.lang.Thread: void start()>";
	
	private static final String THREADINIT1 = "<java.lang.Thread: void <init>(java.lang.Runnable)>";
	private static final String THREADINIT2 = "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.Runnable)>";
	private static final String THREADINIT3 = "<java.lang.Thread: void <init>(java.lang.Runnable,java.lang.String)>";
	private static final String THREADINIT4 = "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.Runnable,java.lang.String)>";
	private static final String THREADINIT5 = "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.Runnable,java.lang.String,long)>";
	
	public static void handleThreadStart(Stmt stmt, UnitGraph cfg, Widget current) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
		Value invoker = instanceInvokeExpr.getBase();
		//data flow find task class
		Stmt curStmt_ = stmt;
		Value curValue_ = invoker;
		while(!cfg.getPredsOf(curStmt_).isEmpty()) {
			List<Unit> preStmts_ = cfg.getPredsOf(curStmt_);
			curStmt_ = (Stmt)preStmts_.get(0);
			if(curStmt_.containsInvokeExpr()) {
				InvokeExpr invokeExpr_ = curStmt_.getInvokeExpr();
				if(invokeExpr_ instanceof SpecialInvokeExpr) {
					SpecialInvokeExpr specialInvokeExpr_ = (SpecialInvokeExpr) invokeExpr_;
					Value invoker_ = specialInvokeExpr_.getBase();
					if(invoker_.equivTo(curValue_)) {
						String invokeeSignature_ = specialInvokeExpr_.getMethod().getSignature();
						Value taskValue_ = null;
						switch (invokeeSignature_) {
							case THREADINIT1:
							case THREADINIT3:
								taskValue_ = specialInvokeExpr_.getArg(0);
								break;
							case THREADINIT2:
							case THREADINIT4:
							case THREADINIT5:
								taskValue_ = specialInvokeExpr_.getArg(1);
								break;
							default:
								break;
						}
						if(taskValue_ != null) {
							SootClass taskClass_ = Scene.v().getSootClassUnsafe(taskValue_.getType().toQuotedString());
							if(taskClass_ != null) {
								SootMethod run = taskClass_.getMethodByNameUnsafe("run");
								if(run != null) {
									analysisTask(run, current, new HashSet<SootMethod>());
								}
								break;
							}
						}
					}
				}
			}
		}
	}
	
	public static void handleViewPost(Stmt stmt, Widget current) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
		Value invoker = instanceInvokeExpr.getBase();
		String invokerType = invoker.getType().toQuotedString();
		SootClass invokerClass = Scene.v().getSootClassUnsafe(invokerType);
		if(invokerClass != null) {
			if(!ClassService.isView(invokerClass)) {
				return;
			}
		}
		Value taskValue = instanceInvokeExpr.getArg(0);
		SootClass taskClass = Scene.v().getSootClassUnsafe(taskValue.getType().toQuotedString());
		if(taskClass != null) {
			SootMethod run = taskClass.getMethodByNameUnsafe("run");
			if(run != null) {
				analysisTask(run, current, new HashSet<SootMethod>());
			}
		}
	}
	
	public static void handleActivityRunOnUiThread(Stmt stmt, Widget current) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
		Value invoker = instanceInvokeExpr.getBase();
		String invokerType = invoker.getType().toQuotedString();
		SootClass invokerClass = Scene.v().getSootClassUnsafe(invokerType);
		if(invokerClass != null) {
			if(!ClassService.isActivity(invokerClass)) {
				return;
			}
		}
		Value taskValue = instanceInvokeExpr.getArg(0);
		SootClass taskClass = Scene.v().getSootClassUnsafe(taskValue.getType().toQuotedString());
		if(taskClass != null) {
			SootMethod run = taskClass.getMethodByNameUnsafe("run");
			if(run != null) {
				analysisTask(run, current, new HashSet<SootMethod>());
			}
		}
	}
	
	public static void handleCustomThreadStart(Stmt stmt, Widget current) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
		Value invoker = instanceInvokeExpr.getBase();
		String invokerType = invoker.getType().toQuotedString();
		SootClass invokerClass = Scene.v().getSootClassUnsafe(invokerType);
		if(invokerClass != null) {
			if(ClassService.isCustomThread(invokerClass)) {
				SootMethod run = invokerClass.getMethodByNameUnsafe("run");
				if(run != null) {
					analysisTask(run, current, new HashSet<SootMethod>());
				}
			}
		}
	}
	
	public static void handleAsyncTask(Stmt stmt, Widget current) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
		Value invoker = instanceInvokeExpr.getBase();
		String invokerType = invoker.getType().toQuotedString();
		SootClass invokerClass = Scene.v().getSootClassUnsafe(invokerType);
		if(invokerClass != null) {
			if(ClassService.isAsyncTask(invokerClass)) {
				for(SootMethod sm : invokerClass.getMethods()) {
					if(sm.getName().equals("doInBackground"))
						analysisTask(sm, current, new HashSet<SootMethod>());
					else if(sm.getName().equals("onPostExecute"))
						analysisTask(sm, current, new HashSet<SootMethod>());
				}
			}
		}
	}
	
	private static void analysisTask(SootMethod taskMethod, Widget current, Set<SootMethod> recordes) {
		recordes.add(taskMethod);
		Body body = null;
		try {
			body = taskMethod.retrieveActiveBody();
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
					String tagPermissionString=staticTepOne.v().isTestingPoint(stmt,body);
					if (!tagPermissionString.equals("00")) {
						
						current.setTest(true);
						if (!tagPermissionString.equals("01")) {
							current.setPermissions(tagPermissionString);
						}
						//print & record
						Utils.buildTestPointMsg(taskMethod, invokee);
					}
					
//					if(MethodService.isImageAPI(invokee)) {
//						//ImageAPI
//						current.setTest(true);
//						//print & record
//						Utils.buildTestPointMsg(taskMethod, invokee);
//					}
					if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
							&& !recordes.contains(invokee)) {
						analysisTask(invokee, current, recordes);
					}
				}
			}
		}
	}
}
