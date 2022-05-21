package zzg.staticanalysis.utils;

public class IdProvider {
	
	public static void reset() {
		instance = null;
	}
	
	private static IdProvider instance = null;
	private IdProvider() {}
	public static IdProvider v() {
		if(instance == null) {
			synchronized (IdProvider.class) {
				if(instance == null) {
					instance = new IdProvider();
				}
			}
		}
		return instance;
	}

	private long curNodeId = 0;
	private long curEdgeId = 0;
	private long curWidgetId = 0;
	
	public long nodeId() {
		return ++curNodeId;
	}
	public long edgeId() {
		return ++curEdgeId;
	}
	public long widgetId() {
		return ++curWidgetId;
	}
}
