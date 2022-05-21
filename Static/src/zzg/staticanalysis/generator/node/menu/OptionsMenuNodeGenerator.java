package zzg.staticanalysis.generator.node.menu;

import soot.SootMethod;
import zzg.staticanalysis.Manager;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.utils.Logger;
import zzg.staticguimodel.NodeType;

public class OptionsMenuNodeGenerator extends AMenuNodeGenerator {
	
	public static BaseNode build(SootMethod sm) {
		Logger.i(TAG, "Start to generate OptionsMenuNode ["+sm.getSignature()+"]");
		return new OptionsMenuNodeGenerator(sm).generate();
	}
	
	private OptionsMenuNodeGenerator(SootMethod sm) {
		super(sm);
	}

	@Override
	protected BaseNode generate() {
		BaseNode optionsMenuNode = super.generate();
		optionsMenuNode.setNodeType(NodeType.OPTIONSMENU);
		return optionsMenuNode;
	}
}
