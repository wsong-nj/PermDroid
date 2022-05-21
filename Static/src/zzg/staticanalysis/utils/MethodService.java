package zzg.staticanalysis.utils;

import soot.SootClass;
import soot.SootMethod;

public class MethodService {

	public static boolean isImageAPI(SootMethod sm) {
		String mName = sm.getName();
		String cName = sm.getDeclaringClass().getName();
		if(cName.equals("android.graphics.BitmapFactory") 
				&& (mName.equals("decodeFile")
				|| mName.equals("decodeFileDescriptor")
				|| mName.equals("decodeStream")
				|| mName.equals("decodeByteArray"))){
			return true;
		}
		if(cName.equals("android.graphics.BitmapRegionDecoder") && mName.equals("decodeRegion"))
			return true;
//		if(cName.equals("android.widget.ImageView") && mName.equals("setImageURI"))
//			return true;
//		if(cName.equals("android.graphics.drawable.Drawable") && (mName.equals("createFromPath") || mName.equals("createFromStream")))
//			return true;
		if(cName.equals("com.squareup.picasso.Picasso") && mName.equals("load"))
			return true;
		if(cName.equals("com.bumptech.glide.RequestManager") && mName.equals("load"))
			return true;
		if(cName.equals("com.nostra13.universalimageloader.core.ImageLoader") 
				&& (mName.equals("displayImage") || mName.equals("loadImage") || mName.equals("loadImageSync")))
			return true;
		if(cName.equals("com.facebook.drawee.view.DraweeView") && mName.equals("setController"))
			return true;
		return false;
	}

	public static boolean isFragmentTransaction(SootMethod sm) {
		String cName = sm.getDeclaringClass().getName();
		if(cName.equals("android.app.FragmentTransaction") 
				|| cName.equals("android.support.v4.app.FragmentTransaction")
				|| cName.equals("androidx.fragment.app.FragmentTransaction")) {
			String mName = sm.getName();
			if(mName.equals("replace") || mName.equals("add")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isStartActivity(SootMethod sm) {
		String subSignature = sm.getSubSignature();
		if(subSignature.equals("void startActivity(android.content.Intent)")
				|| subSignature.equals("void startActivity(android.content.Intent,android.os.Bundle)")
				|| subSignature.equals("void startActivityForResult(android.content.Intent,int)")
				|| subSignature.equals("void startActivityForResult(android.content.Intent,int,android.os.Bundle)")
				|| subSignature.equals("void startActivityIfNeeded(android.content.Intent,int,android.os.Bundle)")
				|| subSignature.equals("void startActivityIfNeeded(android.content.Intent,int)")) {
			return true;
		}
		return false;
	}

	public static boolean isDialogFragmentShow(SootMethod sm) {
		String subSignature = sm.getSubSignature();
		if(subSignature.equals("void show(androidx.fragment.app.FragmentManager,java.lang.String)") 
				|| subSignature.equals("void show(android.support.v4.app.FragmentManager,java.lang.String)")
				|| subSignature.equals("int show(androidx.fragment.app.FragmentTransaction,java.lang.String)")
				|| subSignature.equals("int show(android.support.v4.app.FragmentTransaction,java.lang.String)")
				|| subSignature.equals("void showNow(androidx.fragment.app.FragmentManager,java.lang.String)")
				|| subSignature.equals("void showNow(android.support.v4.app.FragmentManager,java.lang.String)")
				|| subSignature.equals("void show()")) {
			return true;
		}
		return false;
	}
	
	public static boolean isAlertDialogShow(SootMethod sm) {
		String mName = sm.getName();
		SootClass sc = sm.getDeclaringClass();
		if((ClassService.isAlertDialog(sc) 
				|| ClassService.isAlertDialogBuilder(sc))
				&& mName.equals("show")) {
			return true;
		}
		return false;
	}
	
	public static boolean isDialogShow(SootMethod sm) {
		String mName = sm.getName();
		SootClass sc = sm.getDeclaringClass();
		if(sc.getName().equals("android.app.Dialog")
				&& mName.equals("show")) {
			return true;
		}
		return false;
	}

	public static boolean isViewCallbackRegister(SootMethod sm) {
		String mName = sm.getName();
		if(mName.equals("setOnClickListener")
				|| mName.equals("setOnItemClickListener")
				|| mName.equals("setOnLongClickListener")
				|| mName.equals("setOnItemLongClickListener")
				|| mName.equals("setOnScrollListener")
				|| mName.equals("setOnDragListener")
				|| mName.equals("setOnHoverListener")
				|| mName.equals("setOnTouchListener")) {
			return true;
		}
		return false;
	}

	public static boolean isPopupMenuShow(SootMethod sm) {
		String mSignature = sm.getSignature();
		if(mSignature.equals("<android.widget.PopupMenu: void show()>")
				|| mSignature.equals("<android.support.v7.widget.PopupMenu: void show()>")
				|| mSignature.equals("<androidx.appcompat.widget.PopupMenu: void show()>")) {
			return true;
		}
		return false;
	}
	
	public static boolean isGetStringOrText(SootMethod sm) {
		if(sm.getName().equals("getString") || sm.getName().equals("getText")) {
			SootClass sc = sm.getDeclaringClass();
			if(ClassService.isContext(sc))
				return true;
		}
		return false;
	}
}
