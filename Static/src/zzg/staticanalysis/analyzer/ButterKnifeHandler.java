package zzg.staticanalysis.analyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.Body;
import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.EventType;
import zzg.staticguimodel.Widget;

public class ButterKnifeHandler {
	private static final String TAG = "[ButterKnifeHandler]";
	
	public static void reset() {
		instance = null;
	}
	
	private static ButterKnifeHandler instance = null;
	private ButterKnifeHandler() { }
	public static ButterKnifeHandler v() {
		if(instance == null) {
			synchronized (ButterKnifeHandler.class) {
				if(instance == null) {
					instance = new ButterKnifeHandler();
				}
			}
		}
		return instance;
	}
	
	private List<SootClass> usingButterKnifeClass = new ArrayList<SootClass>();
	private Map<SootClass, Map<SootField, Integer>> map = new HashMap<SootClass, Map<SootField,Integer>>();
	private Map<SootClass, Set<Widget>> map2 = new HashMap<>();
	
	public void handleButterKnife(List<SootClass> viewBindings) {
		Logger.i(TAG, "Start to handle ButterKnife...");
		for(SootClass sc : viewBindings) {
			try {
				SootField target = sc.getFieldByName("target");
				SootClass targetClass = target.getDeclaringClass();
				usingButterKnifeClass.add(targetClass);
				SootMethod init = null;
				for(SootMethod sm : sc.getMethods()) {
					if(sm.getName().contentEquals("<init>") && sm.getParameterCount() > 1) {
						init = sm;
						break;
					}
				}
				if(init != null) {
					Body body = null;
					try{
						body = init.retrieveActiveBody();
					}catch (Exception e) {
						Logger.e(TAG, new ActiveBodyNotFoundException(e));
					}
					if(body == null)
						continue;
					for(Unit unit : body.getUnits()) {
						Stmt stmt = (Stmt)unit;
						if(stmt.containsInvokeExpr()) {
							SootMethod invokee = stmt.getInvokeExpr().getMethod();
							String invokeMethodName = invokee.getName();
							switch (invokeMethodName) {
								case "findRequiredViewAsType":
								case "findOptionalViewAsType":
								case "castView":
									handle01(stmt, targetClass);
									break;
								case "findRequiredView":
									handle02(stmt, targetClass, body);
								case "findViewById":
									handle03(stmt, targetClass, body);
								default:
									break;
							}
						}
					}
				}
			}catch (RuntimeException e) {
				Logger.e(TAG, e);
			}
		}
		Logger.i(TAG, "Handle ButterKnife end.");
	}

	private Pattern pForField = Pattern.compile("field \'(.*?)\'");//正则表达式中的单条反斜杠竟然要用4条表示，因为要经过两次转义
	
	private void handle01(Stmt stmt, SootClass target) {
		Value arg1 = stmt.getInvokeExpr().getArg(1);
		if(arg1 instanceof IntConstant) {
			int id = ((IntConstant)arg1).value;
			Value arg2 = stmt.getInvokeExpr().getArg(2);
			Matcher m = pForField.matcher(arg2.toString());
			if(m.find()) {
				String fieldName = m.group(1);
				SootField field = target.getFieldByName(fieldName);
				if(!map.containsKey(target)) {
					map.put(target, new HashMap<SootField, Integer>());
				}
				map.get(target).put(field, id);
			}
		}
	}
	
	private void handle02(Stmt stmt, SootClass target, Body body) {
		Widget widget = null;
		Value arg1 = stmt.getInvokeExpr().getArg(1);
		if(arg1 instanceof IntConstant) {
			int id = ((IntConstant)arg1).value;
			Value arg2 = stmt.getInvokeExpr().getArg(2);
			if(arg2 instanceof StringConstant) {
				String arg2Str = ((StringConstant)arg2).value;//field 'deleteBtn' and method 'deleteCategory'
				Pattern pForMethod = Pattern.compile("method \'(.*?)\'");
				Matcher mForMethod = pForMethod.matcher(arg2Str);
				if(mForMethod.find()) {
					String responseMethod = mForMethod.group(1);
					widget = new Widget();
					widget.setEventMethod(responseMethod);
					SootMethod eventHandle = target.getMethodByNameUnsafe(responseMethod);
					if(eventHandle != null) {
						widget.setEventHandler(eventHandle);
					}
					handleListenerType(stmt, body, widget);
					if(!map2.containsKey(target))
						map2.put(target, new HashSet<Widget>());
					map2.get(target).add(widget);
				}
				Matcher mForField = pForField.matcher(arg2Str);
				if(mForField.find()) {
					String fieldName = mForField.group(1);
					SootField field = target.getFieldByName(fieldName);
					if(!map.containsKey(target)) {
						map.put(target, new HashMap<SootField, Integer>());
					}
					map.get(target).put(field, id);
				}
			}
			if(widget != null) {
				widget.setResId(id);
			}
		}
	}
	
	private void handle03(Stmt stmt, SootClass target, Body body) {
		Value arg0 = stmt.getInvokeExpr().getArg(0);
		if(arg0 instanceof IntConstant) {
			int id = ((IntConstant)arg0).value;
			BriefUnitGraph graph = new BriefUnitGraph(body);
			Unit u = graph.getSuccsOf(stmt).get(0);
			if(((Stmt)u) instanceof AssignStmt && ((Stmt)u).containsFieldRef()) {
				AssignStmt ass = (AssignStmt)u;
				FieldRef leftOfField = ass.getFieldRef();
				SootField field = leftOfField.getField();
				if(!map.containsKey(target)) {
					map.put(target, new HashMap<SootField, Integer>());
				}
				map.get(target).put(field, id);
			}
		}
	}
	
	
	/**
	 * targetType = "android.widget.AdapterView<?>",setter = "setOnItemClickListener",type = "android.widget.AdapterView.OnItemClickListener"
	 * targetType = "android.widget.AdapterView<?>",setter = "setOnItemLongClickListener",type = "android.widget.AdapterView.OnItemLongClickListener"
	 * targetType = "android.widget.AdapterView<?>",setter = "setOnItemSelectedListener",type = "android.widget.AdapterView.OnItemSelectedListener",callbacks = OnItemSelected.Callback.class
	 * targetType = "android.view.View",setter = "setOnClickListener",type = "butterknife.internal.DebouncingOnClickListener"
	 * targetType = "android.view.View",setter = "setOnLongClickListener",type = "android.view.View.OnLongClickListener"
	 * targetType = "android.view.View",setter = "setOnTouchListener",type = "android.view.View.OnTouchListener"
	 * targetType = "android.widget.TextView",setter = "setOnEditorActionListener",type = "android.widget.TextView.OnEditorActionListener"
	 * @param widget 
	 * @param responseMethod 
	 */
	private void handleListenerType(Stmt stmt, Body body, Widget widget) {
		if(stmt instanceof AssignStmt) {
			Value leftOp = ((AssignStmt)stmt).getLeftOp();
			if(leftOp instanceof Local) {
				BriefUnitGraph graph = new BriefUnitGraph(body);
				List<Unit> succs = graph.getSuccsOf(stmt);
				for(Unit succ : succs) {
					Stmt suc = (Stmt)succ;
					if(suc.containsInvokeExpr()) {
						InvokeExpr invokeExpr = suc.getInvokeExpr();
						if(invokeExpr instanceof VirtualInvokeExpr) {
							//virtualinvoke view.<interface: void setOnXXXListener(param)>(callback)
							String registerName = suc.getInvokeExpr().getMethod().getName();
							switch (registerName) {
								case "setOnClickListener":
								case "setOnItemClickListener":
									widget.setEventType(EventType.CLICK);
									return;
								case "setOnLongClickListener":
								case "setOnItemLongClickListener":
									widget.setEventType(EventType.LONGCLICK);
									return;
								default:
									break;
							}
							return;
						}
					}
				}
				
			}
		}
	}
	
	public boolean isUsingButterKnife(SootClass sc) {
		if(usingButterKnifeClass.contains(sc)) {
			return true;
		}
		return false;
	}
	
	public Set<Widget> getWidget(SootClass sc){
		if(map2.containsKey(sc)) {
			return map2.get(sc);
		}
		return new HashSet<Widget>();
	}
	
	public int getResId(SootClass sc, SootField field) {
		if(map.containsKey(sc) && map.get(sc).containsKey(field)) {
			return map.get(sc).get(field);
		}
		return -1;
	}
}
