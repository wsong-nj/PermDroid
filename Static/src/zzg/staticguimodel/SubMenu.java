package zzg.staticguimodel;

import java.util.ArrayList;
import java.util.List;

public class SubMenu extends Widget {
	private static final long serialVersionUID = 1L;

	private int subMenuId;
	private List<MenuItem> items = new ArrayList<MenuItem>();
	
	public String getItemIDString() {
		String string="";
		for(MenuItem mItem: items) {
			string+=mItem.getItemId()+" ";
		}
		return string;
	}
	public SubMenu() {
		this.setEventType(EventType.CLICK);
		this.setType("android.view.SubMenu");
	}

	public int getSubMenuId() {
		return subMenuId;
	}

	public void setSubMenuId(int subMenuId) {
		this.subMenuId = subMenuId;
	}

	public List<MenuItem> getItems() {
		return items;
	}

	public void setItems(List<MenuItem> items) {
		this.items = items;
	}
	
	public void addItem(MenuItem item) {
		items.add(item);
	}
}
