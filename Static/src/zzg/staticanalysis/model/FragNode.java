package zzg.staticanalysis.model;

import java.util.HashSet;
import java.util.Set;

import org.jf.smali.smaliParser.integer_literal_return;

import zzg.staticanalysis.Manager;
import zzg.staticguimodel.NodeType;

public class FragNode extends BaseNode {
	protected FragNode(long id) {
		super(id);
	}
	
	public FragNode(long id, String name) {
		super(id);
		this.setNodeType(NodeType.FRAGMENT);
		this.name = name;
		Manager.v().putFragNode(name, this);
	}
	
	private String name;
	private BaseNode contextMenu;
	private BaseNode optionsMenu;

	//该fragment可以达到的fragment（通过replace或add）
	private Set<String> fragmentsName = new HashSet<String>();

	
	

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public BaseNode getOptionsMenu() {
		return optionsMenu;
	}
	public void setOptionsMenu(BaseNode optionsMenu) {
		this.optionsMenu = optionsMenu;
	}
	
	public BaseNode getContextMenu() {
		return contextMenu;
	}
	public void setContextMenu(BaseNode contextMenu) {
		this.contextMenu = contextMenu;
	}
	
	public Set<String> getFragmentsName() {
		return fragmentsName;
	}
	public void setFragmentsName(Set<String> fragmentsName) {
		this.fragmentsName = fragmentsName;
	}
}
