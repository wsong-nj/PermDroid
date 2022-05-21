package zzg.staticguimodel;

import java.io.Serializable;

public enum NodeType implements Serializable {
	ACTIVITY, //活动
	FRAGMENT, //碎片
	DIALOG, //弹窗
	
	OPTIONSMENU, //选项菜单
	CONTEXTMENU, //上下文菜单
	POPUPMENU, //弹出菜单
	DRAWER //侧边菜单
}
