package zzg.staticanalysis.generator.node.menu;

import soot.SootMethod;
import zzg.staticanalysis.Manager;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.utils.Logger;
import zzg.staticguimodel.NodeType;

public class ContextMenuNodeGenerator extends AMenuNodeGenerator {

	public static BaseNode build(SootMethod sm) {
		Logger.i(TAG, "Start to generate ContextMenuNode ["+sm.getSignature()+"]");
		return new ContextMenuNodeGenerator(sm).generate();
	}
	
	private ContextMenuNodeGenerator(SootMethod sm) {
		super(sm);
	}
	
	@Override
	protected BaseNode generate() {
		BaseNode contextMenuNode = super.generate();
		contextMenuNode.setNodeType(NodeType.CONTEXTMENU);
		return contextMenuNode;
	}
}
