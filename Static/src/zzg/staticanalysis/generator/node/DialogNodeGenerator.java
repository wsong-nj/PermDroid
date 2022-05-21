package zzg.staticanalysis.generator.node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.Manager;
import zzg.staticanalysis.staticTepOne;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.dataflow.WidgetFinder;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.utils.ClassService;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.MethodService;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.EventType;
import zzg.staticguimodel.NodeType;
import zzg.staticguimodel.Widget;

public class DialogNodeGenerator {
	private static final String TAG = "[Generator-DialogNode]";
	
	public static BaseNode build(Stmt stmt, UnitGraph cfg, Value dialogValue, BaseNode dialogNode) {
		Logger.i(TAG, "Start to generate DialogNode [" + cfg.getBody().getMethod().getSignature() + "]");
		Logger.i(TAG, "Stmt: " + (stmt == null ? null : stmt.toString()));
		BaseNode res = new DialogNodeGenerator(dialogNode).generate(stmt, cfg, dialogValue);
		if(res != null) {
			Logger.i(TAG, "Successful");
		}
		Logger.i(TAG, "End");
		//Manager.v().add(dialogNode);//我加的，因为这里的dialog好像没添加到baseNodes里面  ,尽管有可能和fragment重复
		return res;
	}
	
	private DialogNodeGenerator(BaseNode node) {
		this.dialogNode = node;
		
	}
	
	private BaseNode dialogNode;
	//DialogNodeGenerator.build(null, cfg, null, fragNode);
	public BaseNode generate(Stmt stmt, UnitGraph cfg, Value dialogValue) {
		if(stmt == null) {
			//来自FragmentNodeGenerator，找到cfg（onCreateDialog）的返回语句
			ReturnStmt returnStmt = Utils.getReturnStmt(cfg);
			if(returnStmt != null) {
				dialogValue = returnStmt.getOp();
				stmt = returnStmt;
			}
		}
		handleDialog(stmt, cfg, dialogValue, new HashSet<SootMethod>());
		return dialogNode;
	}

	/**
	 * 
	 * @param sm stmt in
	 * @param stmt Dialog::show() invoke stmt
	 * @param dialogNode
	 */
	private void handleDialog(Stmt stmt, UnitGraph cfg, Value dialogValue, Set<SootMethod> records) {
		System.out.println("handleDialog");
		SootMethod sm = cfg.getBody().getMethod();
		records.add(sm);
		Stmt curStmt = stmt, dataflowStmt = stmt;
		Value curValue = dialogValue;
		while(!cfg.getPredsOf(curStmt).isEmpty()) {
			curStmt = (Stmt)cfg.getPredsOf(curStmt).get(0);
			if(curStmt instanceof DefinitionStmt) {
				Value left = ((DefinitionStmt) curStmt).getLeftOp();
				if(left.equivTo(curValue)) {
					dataflowStmt = curStmt;
					if(curStmt.containsInvokeExpr()) {
						InvokeExpr invokeExpr = curStmt.getInvokeExpr();
						SootMethod invokee = invokeExpr.getMethod();
						if(invokeExpr instanceof InstanceInvokeExpr) {
							Value invokerValue = ((InstanceInvokeExpr)invokeExpr).getBase();
							if(ClassService.isAlertDialogBuilder(invokee.getDeclaringClass()) && invokee.getName().equals("create")) {
								curValue = invokerValue;
								continue;
							}
						}
					}
				}
			}
			if(curStmt.containsInvokeExpr()) {
				InvokeExpr invokeExpr = curStmt.getInvokeExpr();
				SootMethod invokee = invokeExpr.getMethod();
				if(invokeExpr instanceof InstanceInvokeExpr) {
					Value invokerValue = ((InstanceInvokeExpr)invokeExpr).getBase();
					if(invokerValue.equivTo(curValue) && ClassService.isAlertDialogBuilder(invokee.getDeclaringClass())) {
						analysisButton(curStmt, invokeExpr, cfg);
						analysisListListener(invokeExpr);
						analysisSetView(invokeExpr, cfg, curStmt);
					}else if(AppParser.v().isAppClass(invokee.getDeclaringClass().getName()) 
								&& !records.contains(invokee)) {
//						handleDialog(curStmt, new BriefUnitGraph(invokee.getActiveBody()), curValue, dialogNode, records);
					}
				}
				if(invokeExpr instanceof SpecialInvokeExpr) {
					if(ClassService.isAlertDialogBuilder(invokee.getDeclaringClass()) && invokee.getName().equals("<init>")) {
						break;
					}
				}
			}
		}
	}
	
	private void analysisButton(Stmt stmt, InvokeExpr invokeExpr, UnitGraph cfg) {
		SootMethod invokee = invokeExpr.getMethod();
		if(invokee.getName().equals("setPositiveButton")
				|| invokee.getName().equals("setNegativeButton")
				|| invokee.getName().equals("setNeutralButton")) {
			Logger.i(TAG, "   Find A Button Setter: " + invokeExpr.toString());
			//分析Button
			//setPositiveButton|setNegativeButton|setNeutralButton
			//(int_string|CharSequence, DialogInterface.OnClickListener)
			Widget button = new Widget();
			button.setId(IdProvider.v().widgetId());
			button.setType("android.widget.Button");
			//text
			if(invokee.getParameterType(0).toQuotedString().equals("int")) {
				Value textId = invokeExpr.getArg(0);
				if(textId instanceof IntConstant) {
					try {
						String text = AppParser.v().getStringById(((IntConstant)textId).value);
						button.setText(text);
					}catch (Exception e) {
						Logger.e(TAG, e);
					}
				}
			}else {
				Value arg0 = invokeExpr.getArg(0);
				if(arg0 instanceof StringConstant) {
					String text = ((StringConstant) arg0).toString();
					button.setText(text);
				}else if(arg0 instanceof Local) {
					SmartLocalDefs localDefs = new SmartLocalDefs(cfg, new SimpleLiveLocals(cfg));
					List<Unit> defs = localDefs.getDefsOfAt((Local)arg0, stmt);
					if(!defs.isEmpty()) {
						Stmt def = (Stmt)defs.get(0);
						if(def.containsInvokeExpr()) {
							InvokeExpr stringInvokeExpr = def.getInvokeExpr();
							if(MethodService.isGetStringOrText(stringInvokeExpr.getMethod())) {
								arg0 = stringInvokeExpr.getArg(0);
								if(arg0 instanceof IntConstant) {
									try {
										String text = AppParser.v().getStringById(((IntConstant) arg0).value);
										button.setText(text);
									}catch (Exception e) {
										Logger.e(TAG, e);
									}
								}
							}
						}
					}
				}
			}
			//res Id & name
			if(invokee.getName().equals("setPositiveButton")) {
				button.setResId(16908313);
				button.setResName("button1");
			}else if(invokee.getName().equals("setNegativeButton")) {
				button.setResId(16908314);
				button.setResName("button2");
			}else {
				button.setResId(16908315);
				button.setResName("button3");
			}
			button.setEventType(EventType.CLICK);
			button.setEventMethod("onClick");
			Value listenerValue = invokeExpr.getArg(1);
			String listenerClassName = listenerValue.getType().toQuotedString();
			SootClass listenerClass = Scene.v().getSootClassUnsafe(listenerClassName);
			if(listenerClass != null) {
				SootMethod eventHandler = listenerClass.getMethodByNameUnsafe("onClick");
				if(eventHandler != null) {
					button.setEventHandler(eventHandler);
				}
			}
			
			Logger.i(TAG, "     ResName: " + button.getResName());
			Logger.i(TAG, "     Text: " + (button.getText() == null ? "null" : button.getText()));
			Logger.i(TAG, "     Callback Class: " + (listenerClass == null ? "null" : listenerClassName));
			Logger.i(TAG, "     Callback Method: " + (button.getEventHandler() == null ? "null" : button.getEventHandler().getSignature()));
			if(dialogNode == null) {
				dialogNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DIALOG);
			}
			dialogNode.addWidget(button);
		}
	}
	
	private void analysisListListener(InvokeExpr invokeExpr) {
		SootMethod invokee = invokeExpr.getMethod();
		//分析List的Listener（if contains），此时的ListView是没有ID的，所以直接靠类型获取
		if(invokee.getName().equals("setItems")
				|| invokee.getName().equals("setAdapter")
				|| invokee.getName().equals("setCursor")) {
			Logger.i(TAG, "   Find The ListView Stuff: " + invokeExpr.toString());
			//(any, DialogInterface.OnClickListener, ..)
			Widget listView = new Widget();
			listView.setId(IdProvider.v().widgetId());
			listView.setType("android.widget.ListView");
			listView.setEventType(EventType.CLICK);
			listView.setEventMethod("onClick");
			Value listenerValue = invokeExpr.getArg(1);
			String listenerClassName = listenerValue.getType().toQuotedString();
			SootClass listenerClass = Scene.v().getSootClassUnsafe(listenerClassName);
			if(listenerClass != null) {
				SootMethod eventHandler = listenerClass.getMethodByNameUnsafe("onClick");
				if(eventHandler != null) {
					listView.setEventHandler(eventHandler);
				}
			}
			
			Logger.i(TAG, "     Callback Class: " + (listenerClass == null ? "null" : listenerClassName));
			Logger.i(TAG, "     Callback Method: " + (listView.getEventHandler() == null ? "null" : listView.getEventHandler().getSignature()));
			
			if(dialogNode == null) {
				dialogNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DIALOG);
			}
			dialogNode.addWidget(listView);
		}
	}
	
	private void analysisSetView(InvokeExpr invokeExpr, UnitGraph cfg, Stmt stmt) {
		SootMethod invokee = invokeExpr.getMethod();
		//setView
		if(invokee.getName().equals("setView")) {
			Logger.i(TAG, "   Find A View Setter: " + invokeExpr.toString());
			Value viewValue = invokeExpr.getArg(0);
			String viewClassName = viewValue.getType().toQuotedString();
			//非自定义控件，只分析会显示图片的控件和通常一看就可点击的控件吧？
			if(viewClassName.equals("android.view.View")) {
				//viewValue = virtualinvoke $r3.<android.view.LayoutInflater: android.view.View inflate(int,android.view.ViewGroup)>(layout-res-id, $r6);
				SmartLocalDefs slds = new SmartLocalDefs(cfg, new SimpleLiveLocals(cfg));
				List<Unit> defs = slds.getDefsOfAt((Local)viewValue, stmt);
				for(Unit defu : defs) {
					if(defu instanceof AssignStmt) {
						AssignStmt assignStmtDef = (AssignStmt) defu;
						if(assignStmtDef.containsInvokeExpr()) {
							if(assignStmtDef.getInvokeExpr().getMethod().getSignature().equals("<android.view.LayoutInflater: android.view.View inflate(int,android.view.ViewGroup)>")) {
								Value layoutIdValue = assignStmtDef.getInvokeExpr().getArg(0);
								if(layoutIdValue instanceof IntConstant) {
									int layoutId = ((IntConstant) layoutIdValue).value;
									try {
										String layoutName = AppParser.v().getLayoutNameById(layoutId);
										if(dialogNode == null) {
											dialogNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DIALOG);
										}
										dialogNode.addAllWidgets(AppParser.v().getEventWidget(layoutName));
									}catch (Exception e) {
										Logger.e(TAG, e);
									}
								}else if(layoutIdValue instanceof Local) {
									System.out.println("TEST\t[dialog-view-layout-id-not-int] "+cfg.getBody().getMethod().getName()+" "+assignStmtDef.toString());
								}
							}else {
								//View from other methods??
								System.out.println("TEST\t[dialog-view] "+cfg.getBody().getMethod().getName()+" "+assignStmtDef.toString());
							}
						}
					}else if(defu instanceof IdentityStmt) {
						IdentityStmt identityStmtDef = (IdentityStmt) defu;
						if(identityStmtDef.containsFieldRef()) {
							FieldRef fieldRef = identityStmtDef.getFieldRef();
							//Find the statement that assigns a value to the field and see if it is inflate. If so, continue the analysis
						}
					}
				}
			}
			
			//Custom widget
			SootClass viewClass = Scene.v().getSootClassUnsafe(viewClassName);
			if(viewClass != null && AppParser.v().isAppClass(viewClassName)) {
				Widget v = new Widget();
				v.setId(IdProvider.v().widgetId());
				v.setType(viewClassName);
				if(dialogNode == null) {
					dialogNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DIALOG);
				}
				dialogNode.addWidget(v);
				//How to analyze custom widgets?
				//1. Combination type custom control (must inherit ViewGroup directly or indirectly): combine a series of controls into one to reuse this combination. In this case, a layout layout will be expanded in the < init > method, and then the custom view will contain the controls in the layout file. At the same time, whether there is setonxxlister method in this class will be analyzed
				for(SootMethod sm1 : viewClass.getMethods()) {
					if(sm1.isConcrete()) {
						Body body1 = null;
						try {
							body1 = sm1.retrieveActiveBody();
						} catch (RuntimeException e) {
							Logger.e(TAG, new ActiveBodyNotFoundException(e));
						}
						if(body1 != null) {
							//In fact, we mainly need to find out whether there is a setlistener method and an tgt API
							for(Unit u : body1.getUnits()) {
								Stmt s = (Stmt) u;
								if(s.containsInvokeExpr()) {
									InvokeExpr invokeExpr1 = s.getInvokeExpr();
									SootMethod invokee1 = invokeExpr1.getMethod();
									if(invokeExpr1 instanceof VirtualInvokeExpr && MethodService.isViewCallbackRegister(invokee1)) {
//										Widget w = Dataflow.getWidget(s, new BriefUnitGraph(body1), ((VirtualInvokeExpr) invokeExpr1).getBase());
										Widget w = WidgetFinder.find(s, new BriefUnitGraph(body1), ((VirtualInvokeExpr) invokeExpr1).getBase(), null);
										if(w != null) {
											String eventMethod = Utils.getViewCallbackMethod(invokee1.getName());
											w.setEventMethod(eventMethod);
											SootClass callbackClass = Scene.v().getSootClassUnsafe(invokeExpr1.getArg(0).getType().toQuotedString());
											if(callbackClass != null) {
												SootMethod eventHandler = callbackClass.getMethodByNameUnsafe(eventMethod);
												if(eventHandler != null) {
													w.setEventHandler(eventHandler);
												}
											}
											if(dialogNode == null) {
												dialogNode = new BaseNode(IdProvider.v().nodeId(), NodeType.DIALOG);
											}
											dialogNode.addWidget(w);
										}
									}
									String tagPermissionString= staticTepOne.v().isTestingPoint(s,body1);
									if (!tagPermissionString.equals("00")) {
										v.setTest(true);
										if (!tagPermissionString.equals("01")) {
											v.setPermissions(tagPermissionString);
										}
										Utils.buildTestPointMsg(sm1, invokee1);
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
