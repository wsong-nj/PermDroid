package zzg.staticanalysis.analyzer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.staticTepOne;
import zzg.staticanalysis.dataflow.WidgetFinder;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.utils.ClassService;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.MethodService;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.EventType;
import zzg.staticguimodel.Widget;

public class AdapterAnalyzer {
	private static final String TAG = "[AdapterAnalyzer]";

	public static void analysisEvent(SootMethod sm, Widget adapter, BaseNode node, SootMethod caller) {
		new AdapterAnalyzer().analysisAdapter(sm, adapter, node, caller, new HashSet<SootMethod>());
	}
	
	public static void analysis(Stmt stmt, UnitGraph cfg, BaseNode node, SootMethod caller) {
		new AdapterAnalyzer().findAdapterViewAndAnalysisEvent(stmt, cfg, node, caller);
	}

	private void findAdapterViewAndAnalysisEvent(Stmt stmt, UnitGraph cfg, BaseNode node, SootMethod caller) {
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		if(invokeExpr instanceof InstanceInvokeExpr) {
			Value adapterViewValue = ((InstanceInvokeExpr)invokeExpr).getBase();
			String invokerType = adapterViewValue.getType().toString();
			SootClass invokerClass = Scene.v().getSootClassUnsafe(invokerType);
			if(invokerClass != null && (ClassService.isAdapterView(invokerClass) || ClassService.isRecyclerView(invokerClass))) {
				Widget adapterView = WidgetFinder.find(stmt, cfg, adapterViewValue, caller);
				if(adapterView == null) {
					adapterView = new Widget();
					adapterView.setType(invokerType);
					adapterView.setEventMethod(Utils.ADAPTER);
				}
				node.addWidget(adapterView);
				
				Value adapterValue = invokeExpr.getArg(0);
				String adapterClassName = adapterValue.getType().toQuotedString();
				SootClass adapter = Scene.v().getSootClassUnsafe(adapterClassName);
				if(adapter != null) {
					if(ClassService.isRecyclerViewAdapter(adapter)) {
						for(SootMethod onBindViewHolder : adapter.getMethods()) {
							if(onBindViewHolder.getName().equals("onBindViewHolder") && onBindViewHolder.getParameterCount() == 2) {
								String viewHolderClassName = onBindViewHolder.getParameterType(0).toQuotedString();
								//排除public volatile void onBindViewHolder(androidx.recyclerview.widget.RecyclerView$ViewHolder, int)
								if(viewHolderClassName.equals("androidx.recyclerview.widget.RecyclerView$ViewHolder")
										|| viewHolderClassName.equals("android.support.v7.widget.RecyclerView$ViewHolder")) {
									continue;
								}
								analysisAdapter(onBindViewHolder, adapterView, node, null, new HashSet<SootMethod>());
								break;
							}
						}
					}else if(ClassService.isAdapter(adapter)) {
						try {
							SootMethod getView = adapter.getMethod("android.view.View getView (int,android.view.View,android.view.ViewGroup)");
							analysisAdapter(getView, adapterView, node, null, new HashSet<SootMethod>());
						}catch (RuntimeException e) {
							Logger.e(TAG, e);
						}
					}
				}
			}
		}
	}
	
	
	
	
	
	
	
	//////////////////////////////////////
	
	private void analysisAdapter(SootMethod sm, Widget adapter, BaseNode node, SootMethod caller, Set<SootMethod> records) {
		records.add(sm);
		Body body = null;
		try{
			body = sm.retrieveActiveBody();
		}catch (Exception e) {
			Logger.e(TAG, new ActiveBodyNotFoundException(e));
		}
		if(body == null)
			return;
		UnitGraph cfg = new BriefUnitGraph(body);
		Iterator<Unit> units = cfg.iterator();
		while(units.hasNext()) {
			Stmt stmt = (Stmt)units.next();
			if(stmt.containsInvokeExpr()) {
				InvokeExpr invokeExpr = stmt.getInvokeExpr();
				SootMethod invokee = invokeExpr.getMethod();
				String invokeeName = invokee.getName();
				//我改过了
				String tagPermissiString=staticTepOne.v().isTestingPoint(stmt,body);
				if(!tagPermissiString.equals("00")) {
					adapter.setTest(true);
					if(!tagPermissiString.equals("01")) {
						adapter.setPermissions(tagPermissiString);
					}
					//print & record
					Utils.buildTestPointMsg(sm, invokee);
					return;
				}else if(MethodService.isViewCallbackRegister(invokee)) {
					if(invokeExpr instanceof VirtualInvokeExpr) {
						EventType eventType = EventType.getEventType(invokeeName);
						String eventMethodName = Utils.getViewCallbackMethod(invokeeName);
						Value widgetValue = ((VirtualInvokeExpr) invokeExpr).getBase();//widget
						Widget widget = WidgetFinder.find(stmt, cfg, widgetValue, caller);//Dataflow.getWidget(stmt, cfg, widgetValue);
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
							node.addWidget(widget);
						}
					}
				}else if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
						&& !records.contains(invokee)) {
					analysisAdapter(invokee, adapter, node, sm, records);
				}
			}
		}
	}
}
