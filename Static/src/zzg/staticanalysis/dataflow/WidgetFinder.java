package zzg.staticanalysis.dataflow;

import java.util.Iterator;
import java.util.List;

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
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.AppParser;
import zzg.staticanalysis.analyzer.ActiveBodyNotFoundException;
import zzg.staticanalysis.analyzer.ButterKnifeHandler;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticguimodel.Widget;

public class WidgetFinder {
	private static final String TAG = "[WidgetFinder]";

	public static Widget find(Stmt stmt, UnitGraph cfg, Value widgetValue, SootMethod caller) {
		Logger.i(TAG, "----Strat to fing target fragment/widget for:");
		Logger.i(TAG, "--------Stmt: " + stmt.toString());
		Logger.i(TAG, "--------Method: " + cfg.getBody().getMethod().getSignature());
		Logger.i(TAG, "--------Caller: " + (caller == null ? "null" : caller.getSignature()));
		Widget result = new WidgetFinder().start(stmt, cfg, widgetValue, caller);
		Logger.i(TAG, "----End");
		return result;
	}

	private Widget widget;
	
	private Widget start(Stmt stmt, UnitGraph cfg, Value widgetValue, SootMethod caller) {
		SootMethod sm = cfg.getBody().getMethod();
		Stmt curStmt = stmt, dataflowStmt = stmt;
		Value curValue = widgetValue;
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
							widget = new Widget();
							widget.setId(IdProvider.v().widgetId());
							if(arg0 instanceof IntConstant) {
								int invokerViewId = ((IntConstant)arg0).value;
								widget.setResId(invokerViewId);
								try {
									String invokerViewName = AppParser.v().getWidgetNameById(invokerViewId);
									widget.setResName(invokerViewName);
								}catch (Exception e) {
									Logger.e(TAG, e);
								}
							}
							widget.setType(widgetValue.getType().toQuotedString());
							return widget;
						}
					}
					if(right instanceof NewExpr) {
						NewExpr newExpr = (NewExpr) right;
						if(curValue.getType().equals(newExpr.getType())) {
							widget = new Widget();
							widget.setId(IdProvider.v().widgetId());
							widget.setType(widgetValue.getType().toQuotedString());
							return widget;
						}
					}
				}
			}
		}
		//widget = $r1.<className: fieldType fieldName>
		if(dataflowStmt instanceof AssignStmt && dataflowStmt.containsFieldRef()) {
			AssignStmt assignStmt = (AssignStmt)dataflowStmt;
			Value leftOp = assignStmt.getLeftOp();
			if(leftOp.equivTo(curValue)) {
				Value rightOp = assignStmt.getRightOp();
				if(rightOp instanceof FieldRef) {
					FieldRef fieldRef = (FieldRef)rightOp;
					fromField(widgetValue, sm, fieldRef, caller);
					if(widget != null)
						return widget;
				}
			}
		}
		//widget := @paramterX: parameterType
		if(dataflowStmt instanceof IdentityStmt) {
			IdentityStmt identityStmt = (IdentityStmt)dataflowStmt;
			Value leftOp = identityStmt.getLeftOp();
			if(leftOp.equivTo(curValue)) {
				Value rightOp = identityStmt.getRightOp();
				if(rightOp instanceof ParameterRef) {
					int position = ((ParameterRef)rightOp).getIndex();
					fromParam(sm, position, caller);
					if(widget != null)
						return widget;
				}
			}
		}
		return widget;
	}
	
	private void fromField(Value widgetValue, SootMethod sm, FieldRef fieldRef, SootMethod caller) {
		//check is it bound with ButterKnife
		SootField field = fieldRef.getField();
		SootClass owner = field.getDeclaringClass();
		int id = ButterKnifeHandler.v().getResId(owner, field);
		if(id != -1) {
			widget = new Widget();
			widget.setId(IdProvider.v().widgetId());
			widget.setResId(id);
			try {
				String resName = AppParser.v().getWidgetNameById(id);
				widget.setResName(resName);
			}catch (Exception e) {
				Logger.e(TAG, e);
			}
			widget.setType(widgetValue.getType().toQuotedString());
			return;
		}else {
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
						Stmt curStmt = (Stmt)units.next();
						if(curStmt instanceof AssignStmt && curStmt.containsFieldRef()) {
							Value leftOp = ((AssignStmt) curStmt).getLeftOp();
							if(leftOp instanceof FieldRef) {
								if(((FieldRef)leftOp).getField().equals(fieldRef.getField())) {
									Value curValue = ((AssignStmt) curStmt).getRightOp();
									if (curValue instanceof CastExpr) {
                                        CastExpr extraExpr = (CastExpr) curValue;
                                        curValue = extraExpr.getOp();
                                    }
									start(curStmt, cfg, curValue, null);
									if(widget != null)
										return;
								}
							}
                        }
					}
				}
			}
			if(sm.getDeclaringClass().getName() != owner.getName()) {
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
							Stmt curStmt = (Stmt)units.next();
							if(curStmt instanceof AssignStmt && curStmt.containsFieldRef()) {
								Value leftOp = ((AssignStmt) curStmt).getLeftOp();
								if(leftOp instanceof FieldRef) {
									if(((FieldRef)leftOp).getField().equals(fieldRef.getField())) {
										Value curValue = ((AssignStmt) curStmt).getRightOp();
										if (curValue instanceof CastExpr) {
	                                        CastExpr extraExpr = (CastExpr) curValue;
	                                        curValue = extraExpr.getOp();
	                                    }
										start(curStmt, cfg, curValue, null);
										if(widget != null)
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
	
	private void fromParam(SootMethod sm, int position, SootMethod caller) {
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
							Value wigetValue = invokeExpr.getArg(position);
							start(stmt, cfg, wigetValue, null);
							if(widget != null)
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
								Value wigetValue = invokeExpr.getArg(position);
								start(stmt, cfg, wigetValue, null);
								if(widget != null)
									return;
							}
						}
					}
				}
			}
		}
	}
	
	
	
	
	
}
