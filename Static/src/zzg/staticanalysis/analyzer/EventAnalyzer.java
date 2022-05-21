package zzg.staticanalysis.analyzer;

import java.util.ArrayList;
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
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.Manager;
import zzg.staticanalysis.staticTepOne;
import zzg.staticanalysis.dataflow.TargetActivityFinder;
import zzg.staticanalysis.dataflow.TargetFragmentFinder;
import zzg.staticanalysis.dataflow.WidgetFinder;
import zzg.staticanalysis.generator.node.DialogNodeGenerator;
import zzg.staticanalysis.generator.node.menu.PopupMenuNodeGenerator;
import zzg.staticanalysis.model.ActNode;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.model.Transition;
import zzg.staticanalysis.model.FragNode;
import zzg.staticanalysis.utils.ClassService;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.MethodService;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.NodeType;
import zzg.staticguimodel.Widget;

public class EventAnalyzer {
	private static final String TAG = "[EventAnalyzer]";
	
	public static void analysis(SootMethod sm, BaseNode srcNode, Widget current) {
		Logger.i(TAG, "Start to analysis Widgets Event");
		Logger.i(TAG, "In node: "+srcNode.getNodeType() + "[" + (srcNode instanceof FragNode ? ((FragNode)srcNode).getName() : "") +"]");
		Logger.i(TAG, "Widget: "+current.toString());
		Logger.i(TAG, "EventHandler: "+sm.getSubSignature());
		new EventAnalyzer().analysisEventMethod(sm, srcNode, current, null);
		Logger.i(TAG, "end");
	}
	
	private EventAnalyzer() { }
	
	private Set<SootMethod> recordes = new HashSet<SootMethod>();
	private void analysisEventMethod(SootMethod sm, BaseNode srcNode, Widget current, SootMethod caller) {
		recordes.add(sm);  //
		Body body = null;
		try {
			if(sm.hasActiveBody()) {
				System.out.println("______TAG________");
				System.out.println("SootMethod's subSignature:"+sm.getSubSignature().toString());
				System.out.println("SootMethod's class"+sm.getDeclaringClass().getName());
				SootClass sootClass=sm.getDeclaringClass();
				SootClass sootClass2=sm.getDeclaringClass();
				while (sootClass2.hasOuterClass()) {
					sootClass2=sootClass2.getOuterClassUnsafe();
					System.out.println("outer class："+sootClass2.getName());
				}
				while (sootClass.hasSuperclass()) {
					sootClass=sootClass.getSuperclass();
					System.out.println("super class："+sootClass.getName());
				}
				System.out.println("______TAG________");
				body = sm.retrieveActiveBody();
			}else {
				System.out.println("haven't ActiveBody");
				body = sm.retrieveActiveBody();
			}
			
			
			//Or, in this class, find the sootmethod by the method name, and then get the body
		}catch (RuntimeException e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body != null) {
			UnitGraph cfg = new BriefUnitGraph(body);
			Iterator<Unit> units = cfg.iterator();
			while(units.hasNext()) {
				Stmt stmt = (Stmt)units.next();
				if(stmt.containsInvokeExpr()) {
					handleEvent(stmt, cfg, srcNode, current, caller);
					
					////// app method //////
					InvokeExpr invokeExpr = stmt.getInvokeExpr();
					SootMethod invokee = invokeExpr.getMethod();
					if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName())
							&& !recordes.contains(invokee)) {
						Logger.i(TAG, "Find a app-method: "+invokee.getSubSignature());
						analysisEventMethod(invokee, srcNode, current, sm);
					}
				}
			}
		}
	}
	
	public static void handleEvent(Stmt stmt, UnitGraph cfg, BaseNode node, Widget curWidget, SootMethod caller) {
		SootMethod sm = cfg.getBody().getMethod();
		Body body=cfg.getBody();
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		SootMethod invokee = invokeExpr.getMethod();
		////// generate edge //////
		if(MethodService.isStartActivity(invokee)) {
			Logger.i(TAG, "Find a Activity Transition!");
			//startActivity
			////build edge to activity, special handling startActivityForResult
			Value intentValue = invokeExpr.getArg(0);
			//旧版
//			List<String> actions = new ArrayList<String>();
//			List<String> types = new ArrayList<String>();
//			Set<String> categories = new HashSet<String>();
//			String targetClassName = Dataflow.getTargetActivity(stmt, cfg, intentValue, actions, types, categories, caller);
//			if(targetClassName == null && !actions.isEmpty()) {
//				String type = null;
//				if(!types.isEmpty()) {
//					type = types.get(0);
//				}
//				String action = actions.get(0);
//				targetClassName = AppParser.v().getActivityByImplicitIntent(action, type, categories);
//			}
			//新版
			String targetClassName = TargetActivityFinder.find(stmt, cfg, intentValue, caller);
			
			if(targetClassName != null) {
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setWidget(curWidget);
				t.setSrc(node.getId());
				ActNode tgtNode = Manager.v().getActNodeByName(targetClassName);  
				if(tgtNode != null) {
					t.setTgt(tgtNode.getId());
				}else {
					t.setLabel("MTN["+targetClassName+"]");
				}
				Logger.i(TAG, "Build A T in "+sm.getSignature());
				//我加的
				Manager.v().add(t);
			}
			Logger.i(TAG, "\t-> "+(targetClassName == null ? "null" : targetClassName));
		}
		if(MethodService.isFragmentTransaction(invokee)) {  
			Logger.i(TAG, "Find a Fragment Transition!");
			//Fragment replace/add
			////build edge to fragment
			Set<String> fragmentSet = TargetFragmentFinder.find(cfg, stmt, caller);//UnitAnalyzer.getFragments(cfg, stmt, caller);
			for(String fragName : fragmentSet) {
				FragNode tgtNode = Manager.v().getFragNodeByName(fragName);
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setWidget(curWidget);
				t.setSrc(node.getId());
				if(tgtNode != null) {
					t.setTgt(tgtNode.getId());
				}else {
					t.setLabel("MTN["+fragName+"]");  //表示丢失目标
				}
				Logger.i(TAG, "\t-> "+fragName);
				Logger.i(TAG, "Build A T in "+sm.getSignature());
				//我加的
				Manager.v().add(t);
			}
		}
		if(MethodService.isAlertDialogShow(invokee)) {
			Logger.i(TAG, "Find a AlertDialog!");
			//AlertDialog.Builder show || AlertDialog show
			/////build dialog node & build edge to it
//			BaseNode dialogNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DIALOG);
//			UnitAnalyzer.handleAlertDialog(invokee, dialogNode);
			Value invoker = ((VirtualInvokeExpr)invokeExpr).getBase();
			BaseNode dialogNode = DialogNodeGenerator.build(stmt, cfg, invoker, null);
			if(dialogNode != null) {
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setWidget(curWidget);
				t.setSrc(node.getId());
				t.setTgt(dialogNode.getId());
				Logger.i(TAG, "Build A T in "+sm.getSignature());
				//我加的
				Manager.v().add(t);
			}
		}
		if(MethodService.isDialogShow(invokee) && invokeExpr instanceof VirtualInvokeExpr) {
			Logger.i(TAG, "Find a Dialog!");
			//Dialog show
			VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr)invokeExpr;
			Value invoker = virtualInvokeExpr.getBase();
			String invokerType = invoker.getType().toQuotedString();
			SootClass invokerClass = Scene.v().getSootClassUnsafe(invokerType);
			if(invokerClass != null && ClassService.isAlertDialog(invokerClass)) {
				// && invoker is AlertDialog
				//equals to AlertDialog show
				/////build dialog node & build edge to it
//				BaseNode dialogNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DIALOG);
//				UnitAnalyzer.handleAlertDialog(invokee, dialogNode);
				BaseNode dialogNode = DialogNodeGenerator.build(stmt, cfg, invoker, null);
				if(dialogNode != null) {
					Transition t = new Transition();
					t.setId(IdProvider.v().edgeId());
					t.setWidget(curWidget);
					t.setSrc(node.getId());
					t.setTgt(dialogNode.getId());
					Logger.i(TAG, "Build A T in "+sm.getSignature());
					//我加的
					Manager.v().add(t);
				}
			}
		}
		if(MethodService.isPopupMenuShow(invokee) && invokeExpr instanceof VirtualInvokeExpr) {
			Logger.i(TAG, "Find a PopupMenu!");
			//PopupMenu show
			BaseNode popupMenuNode = PopupMenuNodeGenerator.build(sm, stmt);
			Transition t = new Transition();
			t.setId(IdProvider.v().edgeId());
			t.setWidget(curWidget);
			t.setSrc(node.getId());
			t.setTgt(popupMenuNode.getId());
			Logger.i(TAG, "Build A T in "+sm.getSignature());
			//我加的
			Manager.v().add(t);
		}
		
		
		////// test point //////
		String tagPermissionString=staticTepOne.v().isTestingPoint(stmt,body);
		if (!tagPermissionString.equals("00")) {
			
			curWidget.setTest(true);
			node.setTest(true);
			if (!tagPermissionString.equals("01")) {
				curWidget.setPermissions(tagPermissionString);
				node.setPermissions(tagPermissionString);
			}
			//print & record
			Utils.buildTestPointMsg(sm, invokee);
		}
		
//		if(MethodService.isImageAPI(invokee)) {
//			//ImageAPI
//			curWidget.setTest(true);
//			node.setTest(true);
//			//print & record
//			Utils.buildTestPointMsg(sm, invokee);
//		}
		//Thread & AsyncTask
		if(invokeExpr instanceof InstanceInvokeExpr) {
			String mSubSignature = invokee.getSubSignature();
			String mSignature = invokee.getSignature();
			switch (mSubSignature) {
				//Thread.start(), View.post(Runnable), View.postDelayed(Runnable, long), Activity.runOnUiThread(Runnable)
				case ThreadAnalyzer.VIEWPOST:
				case ThreadAnalyzer.VIEWPOSTDELAYED:
					ThreadAnalyzer.handleViewPost(stmt, curWidget);
					break;
				case ThreadAnalyzer.ACTIVITYRUNONUITHREAD:
					ThreadAnalyzer.handleActivityRunOnUiThread(stmt, curWidget);
					break;
				case ThreadAnalyzer.START:
					if(mSignature.equals(ThreadAnalyzer.THREADSTART)) {
						ThreadAnalyzer.handleThreadStart(stmt, cfg, curWidget);
					}else {
						ThreadAnalyzer.handleCustomThreadStart(stmt, curWidget);
					}
					break;
				//AsyncTask
				case ThreadAnalyzer.EXECUTE:
				case ThreadAnalyzer.EXECUTEONEXECUTER:
					ThreadAnalyzer.handleAsyncTask(stmt, curWidget);
					break;
				default:
					break;
			}
		}
		
		////// Dependency //////
		if(isDepStmt(stmt)) {
			AssignStmt assignStmt = (AssignStmt) stmt;
            Value rightValue = assignStmt.getRightOp();
            Value invokeObj = ((VirtualInvokeExpr) rightValue).getBase();
//            Widget dWidget = Dataflow.getWidget(assignStmt, cfg, invokeObj);
            Widget dWidget = WidgetFinder.find(assignStmt, cfg, invokeObj, caller);
            if(dWidget != null) {
            	curWidget.addDependentWidget(dWidget);
            }
		}
	}
	
	private static boolean isDepStmt(Stmt stmt) {
        if (stmt instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) stmt;
            Value right = assignStmt.getRightOp();
            if (right instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) right;
                SootMethod invokeMethod = virtualInvokeExpr.getMethod();
                String name = invokeMethod.getName();
                if (name.equals("isChecked")) {
                    return true;
                }
            }
        }
        return false;
    }
	
	
}
