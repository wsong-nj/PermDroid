package zzg.staticanalysis.utils;

import soot.SootClass;

public class ClassService {

	public static boolean isAlertDialogBuilder(SootClass sc) {
		String clsName = sc.getName();
		if(clsName.equals("android.app.AlertDialog$Builder")
				|| clsName.equals("android.support.v7.app.AlertDialog$Builder")
				|| clsName.equals("androidx.appcompat.app.AlertDialog$Builder")) {
			return true;
		}
		return false;
	}
	
	public static boolean isAlertDialog(SootClass sc) {
		String clsName = sc.getName();
		if(clsName.equals("android.app.AlertDialog")
				|| clsName.equals("android.support.v7.app.AlertDialog")
				|| clsName.equals("androidx.appcompat.app.AlertDialog")) {
			return true;
		}
		return false;
	}

	public static boolean isImageView(SootClass sc) {
		SootClass superCls = sc;
		if(superCls.getName().equals("android.widget.ImageView")) {
			return true;
		}
		while(superCls.hasSuperclass()) {
			superCls = superCls.getSuperclass();
			if(superCls.getName().equals("android.widget.ImageView")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isListener(SootClass sc) {
		
		return false;
	}

	public static boolean isAsyncTask(SootClass sc) {
		SootClass superCls = sc;
		while(superCls.hasSuperclass()) {
			superCls = superCls.getSuperclass();
			if(superCls.getName().equals("android.os.AsyncTask"))
				return true;
		}
		return false;
	}
	
	public static boolean isRunnable(SootClass sc) {
		SootClass superCls = sc;
		for(SootClass inter : superCls.getInterfaces()) {
			if(inter.getName().equals("java.lang.Runnable")) {
				return true;
			}
		}
		while(superCls.hasSuperclass()) {
			superCls = superCls.getSuperclass();
			for(SootClass inter : superCls.getInterfaces()) {
				if(inter.getName().equals("java.lang.Runnable")) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean isCustomThread(SootClass sc) {
		if(sc.getName().equals("java.lang.Thread"))
			return false;
		else {
			SootClass superCls = sc;
			while(superCls.hasSuperclass()) {
				superCls = superCls.getSuperclass();
				if(superCls.getName().equals("java.lang.Thread"))
					return true;
			}
		}
		return false;
	}

	public static boolean isAdapterView(SootClass sc) {
		SootClass superCls = sc;
		while(superCls.hasSuperclass()) {
			superCls = superCls.getSuperclass();
			if(superCls.getName().equals("android.widget.AdapterView")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAdapter(SootClass sc) {
		SootClass superCls = sc;
		while(superCls.hasSuperclass()) {
			superCls = superCls.getSuperclass();
			if(superCls.getName().equals("android.widget.Adapter")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isRecyclerView(SootClass sc) {
		SootClass superCls = sc;
		while(superCls.hasSuperclass()) {
			superCls = superCls.getSuperclass();
			String superClsName = superCls.getName();
			if(superClsName.equals("androidx.recyclerview.widget.RecyclerView")
					|| superClsName.equals("android.support.v7.widget.RecyclerView")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isRecyclerViewAdapter(SootClass sc) {
		SootClass superCls = sc;
		while(superCls.hasSuperclass()) {
			superCls = superCls.getSuperclass();
			String superClsName = superCls.getName();
			if(superClsName.equals("androidx.recyclerview.widget.RecyclerView$Adapter")
					|| superClsName.equals("android.support.v7.widget.RecyclerView$Adapter")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isActivity(SootClass sc) {
		SootClass superCls = sc;
	    while(superCls.hasSuperclass()) {
	    	superCls = superCls.getSuperclass();
	    	if(superCls.getName().equals("android.app.Activity")) {
	    		//androidx.appcompat.app.AppCompatActivity
				//android.support.v7.app.AppCompatActivity
	    		//¼Ì³ÐÓÚandroid.app.Activity
	    		return true;
	    	}
	    }
		return false;
	}

	public static boolean isFragment(SootClass sc) {
		SootClass superCls = sc;
	    while(superCls.hasSuperclass()) {
	    	superCls = superCls.getSuperclass();
	    	String superClsName = superCls.getName();
	    	if(superClsName.equals("android.app.Fragment")
	    			|| superClsName.equals("androidx.fragment.app.Fragment")
	    			|| superClsName.equals("android.support.v4.app.Fragment")) {
	    		return true;
	    	}
	    }
	    return false;
	}

	public static boolean isView(SootClass sc) {
		if(sc.getName().equals("android.view.View")) {
			return true;
		}else {
			SootClass superCls = sc;
		    while(superCls.hasSuperclass()) {
		    	superCls = superCls.getSuperclass();
		    	if(superCls.getName().equals("android.view.View")) {
		    		return true;
		    	}
		    }
		}
		return false;
	}
	
	public static boolean isPopupMenu(SootClass sc) {
		String cName = sc.getName();
		if(cName.equals("android.widget.PopupMenu")
				|| cName.equals("android.support.v7.widget.PopupMenu")
				|| cName.equals("androidx.appcompat.widget.PopupMenu")) {
			return true;
		}
		return false;
	}
	
	public static boolean isContext(SootClass sc) {
		String cName = sc.getName();
		if(cName.equals("android.content.Context")) {
			return true;
		}else {
			SootClass superCls = sc;
		    while(superCls.hasSuperclass()) {
		    	superCls = superCls.getSuperclass();
		    	if(superCls.getName().equals("android.content.Context")) {
		    		return true;
		    	}
		    }
		}
		return false;
	}
	
	
}
