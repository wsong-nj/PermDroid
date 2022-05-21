package zzg.staticguimodel;

public class MenuItem extends Widget {
	private static final long serialVersionUID = 1L;
	
	private int itemId;

	public MenuItem() {
		this.setEventType(EventType.CLICK);
		this.setType("android.view.MenuItem");
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	
}
