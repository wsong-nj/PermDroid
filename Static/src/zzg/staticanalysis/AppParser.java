package zzg.staticanalysis;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import brut.androlib.ApkDecoder;
import soot.Body;
import soot.G;
import soot.Kind;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlDocument;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.ApkHandler;
import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.tagkit.Tag;
import soot.util.Chain;
import zzg.staticanalysis.analyzer.ButterKnifeHandler;
import zzg.staticanalysis.intent.Activity;
import zzg.staticanalysis.intent.IntentFilter;
import zzg.staticanalysis.utils.ClassService;
import zzg.staticanalysis.utils.ExcludePackage;
import zzg.staticanalysis.utils.IOService;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.SystemicResouces;
import zzg.staticanalysis.utils.Utils;
import zzg.staticguimodel.EventType;
import zzg.staticguimodel.MenuItem;
import zzg.staticguimodel.SubMenu;
import zzg.staticguimodel.Widget;


public class AppParser {
	private static final String TAG = "[App]";
	
	public static void reset() {
		instance = null;
		ResConvertor.reset();
		IOService.reset();
		ButterKnifeHandler.reset();
		IdProvider.reset();
		Manager.reset();
	}
	private static AppParser instance = null;
	private AppParser() {}
	public static AppParser v() {
		if(instance == null) {
			synchronized(AppParser.class) {
				if(instance == null) {
					instance = new AppParser();
				}
			}
		}
		return instance;
	}
	
	private String apkDir;
	private String apkName;
	private String apkFile;
	private String pkg;
	private String launchActivity;
	
	private List<String> activityNames = new ArrayList<String>();
	private List<Activity> implicitActivities = new ArrayList<Activity>();
	
	private ApkHandler apkHandler;
	
//	private List<File> preferenceFiles = new ArrayList<File>();
	
	private CallGraph cg;
	public long constractCgTime = 0;
	
	/* All classes of application. Exclude classes of library as much as possible. */
	private List<SootClass> allClasses = new ArrayList<>();
	/* R$id, R$string, R$menu, R$layout, R$xml, R$navigation */
	private List<SootClass> rClasses = new ArrayList<>();
	
	private List<SootClass> activities = new ArrayList<SootClass>();
	
	private List<SootClass> fragments = new ArrayList<SootClass>();
	
	private List<SootClass> asynctasks = new ArrayList<SootClass>();
	
	private List<SootClass> listeners = new ArrayList<SootClass>();
	
	private List<SootClass> adapters = new ArrayList<SootClass>();
	
	private List<SootClass> viewBindings = new ArrayList<SootClass>();
	
	public List<SootClass> getAllClasses() {
		return allClasses;
	}
	public List<SootClass> getActivities() {
		return activities;
	}
	public List<SootClass> getFragments() {
		return fragments;
	}
	public List<SootClass> getAsynctasks() {
		return asynctasks;
	}
	public List<SootClass> getListeners() {
		return listeners;
	}
	public List<SootClass> getAdapters() {
		return adapters;
	}
	public List<SootClass> getViewBindings(){
		return viewBindings;
	}
	
	public CallGraph getCg() {
		return cg;
	}
	public void setCg(CallGraph cg) {
		this.cg = cg;
	}
	public String getPkg() {
		return pkg;
	}
	
	//////////////////////////////////////////////////
	//        Initialize, load analysis scene       //
	//////////////////////////////////////////////////
	private void initialiseSoot(){
		G.reset();
		
    	Options.v().set_allow_phantom_refs(true);
        Options.v().set_src_prec(Options.src_prec_apk); 
        Options.v().set_android_jars(Main.androidPlatformLocation);
        Options.v().set_process_dir(Collections.singletonList(apkFile));
        
        Options.v().set_prepend_classpath(true);
        Options.v().set_whole_program(true);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_force_overwrite(true);
        
        Options.v().set_keep_line_number(true);
        Options.v().set_include_all(true);
        Options.v().set_validate(true);
		Options.v().set_no_writeout_body_releasing(true);
        Options.v().set_output_format(Options.output_format_dex);
        
		
		Scene.v().loadNecessaryClasses();
		Scene.v().loadBasicClasses();
          
        Scene.v().addBasicClass("android.app.Dialog",SootClass.BODIES);  
        Scene.v().addBasicClass("android.view.View",SootClass.BODIES);
        Scene.v().addBasicClass("android.view.MenuItem",SootClass.BODIES);
        Scene.v().addBasicClass("android.app.Activity", SootClass.SIGNATURES);
		PackManager.v().runPacks();
	}
	private void decompileApk() {   
		File apk = new File(apkFile);
		ApkDecoder apkDecoder = new ApkDecoder();
		try {
			File decompileFile = new File(apkDir + apkName, "decompile");
			apkDecoder.setOutDir(decompileFile);
			apkDecoder.setForceDelete(true);
			apkDecoder.setApkFile(apk);
			apkDecoder.setDecodeSources((short)0);
			apkDecoder.decode();
			Logger.i(TAG, "Decompile Apk successful!");
		}catch (Exception e) {
			Logger.e(TAG, new DecompileException("Decompile Apk fail!", e));
		}
	}
	private void readManifest() {
		File apk = new File(apkFile);
		try(ProcessManifest processManifest = new ProcessManifest(apk)) {
			pkg = processManifest.getPackageName();
			Logger.i(TAG, "============ Manifest ============");
			Logger.i(TAG, "Package name: " + pkg);
			//collect activities
			List<AXmlNode> activityNodes = processManifest.getActivities();

			for(AXmlNode actNode : activityNodes) {
				String actName = actNode.getAttribute("name").getValue().toString();
				Logger.i(TAG, actName);
				if(actName.startsWith(".")) {
					actName = pkg + actName;
				}
				activityNames.add(actName);
				//collect the activities which can be started implicitly
				List<AXmlNode> intentFilterNodes = actNode.getChildrenWithTag("intent-filter");
				ArrayList<IntentFilter> intentFilters = new ArrayList<IntentFilter>();
				for(AXmlNode intentFilterNode : intentFilterNodes) {
					boolean isImplicit = false;
					//categories
					List<AXmlNode> categoryNodes = intentFilterNode.getChildrenWithTag("category");
					ArrayList<String> categories = new ArrayList<String>();
					for(AXmlNode categoryNode : categoryNodes) {
						String category = categoryNode.getAttribute("name").getValue().toString();
						if(category.equals("android.intent.category.DEFAULT")) {
							isImplicit = true;
						}else
							categories.add(category);
					}
					if(!isImplicit) {
						continue;
					}
					//actions
					List<AXmlNode> actionNodes = intentFilterNode.getChildrenWithTag("action");
					ArrayList<String> actions = new ArrayList<String>();
					for(AXmlNode actionNode : actionNodes) {
						String action = actionNode.getAttribute("name").getValue().toString();
						actions.add(action);
					}
					IntentFilter intentFilter = new IntentFilter(actions, categories);
					//data types
					List<AXmlNode> dataNodes = intentFilterNode.getChildrenWithTag("data");
					for(AXmlNode dataNode : dataNodes) {
						if(dataNode.hasAttribute("mimeType")) {
							String dataType = dataNode.getAttribute("mimeType").getValue().toString();
							intentFilter.addDataType(dataType);
						}
					}
					intentFilters.add(intentFilter);
				}
				if(!intentFilters.isEmpty()) {
					Activity implicitActivity = new Activity(actName, intentFilters);
					implicitActivities.add(implicitActivity);
				}
				
			}
			//launch activity
			Set<AXmlNode> launchActivities = processManifest.getLaunchableActivities();
			for(AXmlNode la : launchActivities) {
				launchActivity = la.getAttribute("name").getValue().toString();
				Logger.i(TAG, "launch activity: " + launchActivity);
				if(ProcessManifest.isAliasActivity(la)) {
					AXmlNode lac = processManifest.getAliasActivityTarget(la);
					launchActivity = lac.getAttribute("name").getValue().toString();
				}
				Logger.i(TAG, "real launch activity: " + launchActivity);
				break;
			}
			apkHandler = processManifest.getApk();
			Logger.i(TAG, "==================================");
		} catch (Exception e) {
			Logger.e(TAG, new AppParseException("Read manifest file exception", e));
		}
	}
	private void resolveAllClasses() {
		Logger.i(TAG, "============ R class ============");
		boolean flag = false;
		Chain<SootClass> classChain = Scene.v().getClasses();
		if(launchActivity.startsWith(pkg) || launchActivity.startsWith(".")) {
			flag = true;
			for (SootClass s : classChain) {
				if(s.getName().startsWith(pkg))
					allClasses.add(s);
				//collect R$id, R$string, R$menu, R$layout, R$xml
				if(s.getName().endsWith("R$id") 
						|| s.getName().endsWith("R$string") 
						|| s.getName().endsWith("R$xml")
						|| s.getName().endsWith("R$menu") 
						|| s.getName().endsWith("R$layout") 
						|| s.getName().endsWith("R$navigation")) {
					rClasses.add(s);
					Logger.i(TAG, s.getName());
				}
			}
		}else {
			flag = false;
			for (SootClass s : classChain) {
				if(!ExcludePackage.v().isExclude(s))
					allClasses.add(s);
				//collect R$id, R$string, R$menu, R$layout, R$xml
				if(s.getName().endsWith("R$id") 
						|| s.getName().endsWith("R$string") 
						|| s.getName().endsWith("R$xml")
						|| s.getName().endsWith("R$menu") 
						|| s.getName().endsWith("R$layout") 
						|| s.getName().endsWith("R$navigation")) {
					rClasses.add(s);
					Logger.i(TAG, s.getName());
				}
			}
		}
		Logger.i(TAG, "=================================");
		if(flag)
			Logger.i(TAG, "---------Exclude classes using package names..");
		else
			Logger.i(TAG, "---------Exclude classes using ExcludePakage.txt file..");
	}
	private void collectElement() {
		//collect activity
		for(String act : activityNames) {
			try {
				SootClass actClass = Scene.v().forceResolve(act, SootClass.BODIES);
				if(actClass.getMethods().size() > 0) {
					activities.add(actClass);
				}else {
					Logger.i(TAG, "act["+act+"] is empty,try to splice the package name");
					String actName = pkg + "." + act;
					actClass = Scene.v().getSootClassUnsafe(actName);
					if(actClass != null && actClass.getMethods().size() > 0) {
						activities.add(actClass);
					}else {
						Logger.i(TAG, "collect act exception, could not find the act["+act+"]");
					}
				}
			}catch (RuntimeException e) {
				Logger.e(TAG, new AppParseException("extrac act["+act+"] fail!", e));
			}
		}
		for(SootClass sc : allClasses) {
			//collect fragments
			if(ClassService.isFragment(sc)) {
				fragments.add(sc);
			}
			//
			if(ClassService.isAsyncTask(sc)) {
				asynctasks.add(sc);
			}
			//
			if(ClassService.isAdapter(sc) || ClassService.isRecyclerViewAdapter(sc)) {
				adapters.add(sc);
			}
			//collect ViewBinding
			if(sc.getName().endsWith("_ViewBinding")) {
				viewBindings.add(sc);
			}
		}
	}
	private void buildCG() {
		long startTime = System.currentTimeMillis();
		Logger.i(TAG, "Start to construct Callgraph...");
		SetupApplication setupApplication = new SetupApplication(Main.androidPlatformLocation, apkFile);
		SootConfigForAndroid sootConf = new SootConfigForAndroid() {
            @Override
            public void setSootOptions(Options options, InfoflowConfiguration config) {
                Options.v().set_process_multiple_dex(true);
                Options.v().set_allow_phantom_refs(true);
                Options.v().set_whole_program(true);
                List<String> excludeList = new LinkedList<String>();
        		excludeList.addAll(ExcludePackage.v().excludes);
        		options.set_exclude(excludeList);
        		Options.v().set_no_bodies_for_excluded(true);
            }        	
        };
        setupApplication.setSootConfig(sootConf);
        setupApplication.setCallbackFile("./AndroidCallbacks.txt");
        setupApplication.constructCallgraph();
		long endTime = System.currentTimeMillis();
		constractCgTime = endTime - startTime;
		Logger.i(TAG, "End, took "+constractCgTime+"ms");
		cg = Scene.v().getCallGraph();
		Logger.i(TAG, "Callgraph has "+cg.size()+" edges");
		//handle callgraph
		//remove  
//		int removeSize = 0;
//		Iterator<soot.jimple.toolkits.callgraph.Edge> cgIter = cg.iterator();
//		while(cgIter.hasNext()) {
//			soot.jimple.toolkits.callgraph.Edge e = cgIter.next();
//			SootMethod src = e.src();
//			SootMethod tgt = e.tgt();
//			if(!src.getDeclaringClass().getName().equals("dummyMainClass")
//					&& !tgt.getDeclaringClass().getName().equals("dummyMainClass")) {
//				if(!isAppClass(src.getDeclaringClass().getName()) 
//						|| !isAppClass(tgt.getDeclaringClass().getName())) {
//					cgIter.remove();
//					removeSize++;
//				}
//			}
//		}
//		Logger.i(TAG, "Remove "+removeSize+" edges");
		int addSize = 0;
		//add
		for(SootClass sc : allClasses) {
			for(SootMethod sm : sc.getMethods()) {
				try {
					Body body = sm.retrieveActiveBody();
					Set<String> targetMethodSig = new HashSet<String>();
					
					Iterator<soot.jimple.toolkits.callgraph.Edge> iter = cg.edgesOutOf(sm);
					while(iter.hasNext()) {
						soot.jimple.toolkits.callgraph.Edge e = iter.next();
						SootMethod target = e.tgt();
						if(isAppClass(target.getDeclaringClass().getName())) {
							targetMethodSig.add(target.getSignature());
						}
					}
					for(Unit unit : body.getUnits()) {
						Stmt stmt = (Stmt)unit;
						if(stmt.containsInvokeExpr() || stmt instanceof InvokeStmt) {
							SootMethod invokee = stmt.getInvokeExpr().getMethod();
							if(isAppClass(invokee.getDeclaringClass().getName())) {
								if(!targetMethodSig.contains(invokee.getSignature())) {
									try {
										soot.jimple.toolkits.callgraph.Edge newEdge = new soot.jimple.toolkits.callgraph.Edge(sm, stmt, invokee);
										cg.addEdge(newEdge);
										addSize++;
									} catch (RuntimeException e) {
										soot.jimple.toolkits.callgraph.Edge newEdge = new soot.jimple.toolkits.callgraph.Edge(sm, unit, invokee, Kind.INVALID);
										cg.addEdge(newEdge);
										addSize++;
									}
								}
							}
						}
					}
				} catch (RuntimeException e) {
					
				}
			}
		}
		Logger.i(TAG, "Add "+addSize+" edges");
	}
	
	
	public void init(String apkDir, String apkName) {
		this.apkDir = apkDir;
		this.apkName = apkName;
		this.apkFile = apkDir + apkName + ".apk";
		
		initialiseSoot();
		
		decompileApk();
		
		readManifest();
		
		resolveAllClasses();
		
		collectElement();
		
		buildCG();
	
		IOService.v().init(apkDir, apkName);
		IOService.v().writePkg(pkg);
		
		ResConvertor.v().parse(apkDir, apkName, rClasses);
		
		ButterKnifeHandler.v().handleButterKnife(viewBindings);
	}
	
	//////////////////////////////////////////////////
	//         Provide resource information         //
	//////////////////////////////////////////////////
	private AXmlNode parseResXmlFile(String xmlPath){
		try {
			InputStream is = apkHandler.getInputStream(xmlPath);
			if(is != null) {
				AXmlHandler aXmlHandler = new AXmlHandler(is);
				AXmlDocument aXmlDocument = aXmlHandler.getDocument();
				return aXmlDocument.getRootNode();
			}else {
				System.out.println("!!!xmlPath"+xmlPath+"File fail ");
			}
		} catch (Exception e) {
			Logger.e(TAG, new AppParseException("Parse ResXml File fail", e));
		}
		return null;
	}
	
	public Set<String> getStaticFragmentClassName(String layout) {
		Set<String> staticFragClassName = new HashSet<String>();
		String layoutPath = "res/layout/" + layout + ".xml";
		AXmlNode rootNode = parseResXmlFile(layoutPath);
		if(rootNode != null) {
			Set<AXmlNode> allNodes = new HashSet<AXmlNode>();
			Set<String> includes = new HashSet<String>();
			allNodes.add(rootNode);
			getAllNode(rootNode, allNodes, includes);
			for(AXmlNode node : allNodes) {
				String nodeType = node.getTag();
				if(nodeType.equals("fragment")) {
					if(node.hasAttribute("class")) {
						staticFragClassName.add(node.getAttribute("class").getValue().toString());
					}
					if(node.hasAttribute("name")) {
						staticFragClassName.add(node.getAttribute("name").getValue().toString());
					}
				}
			}
			for(String includeLayout : includes) {
				staticFragClassName.addAll(getStaticFragmentClassName(includeLayout));
			}
		}
		return staticFragClassName;
	}
//	// useless 
//	public boolean containsImageView(String layout) {
//		String layoutPath = "res/layout/" + layout + ".xml";
//		AXmlNode rootNode = parseResXmlFile(layoutPath);
//		if(rootNode != null) {
//			Set<AXmlNode> allNodes = new HashSet<AXmlNode>();
//			Set<String> includes = new HashSet<String>();
//			allNodes.add(rootNode);
//			getAllNode(rootNode, allNodes, includes);
//			for(AXmlNode node : allNodes) {
//				String nodeType = node.getTag();
//				if(nodeType.equals("ImageView") || nodeType.equals("ImageButton")
//						|| nodeType.equals("ZoomButton") || nodeType.equals("QuickContactBadge")) {
//					return true;
//				}else if(nodeType.contains(".")) {
//					SootClass customizedViewClass = Scene.v().getSootClassUnsafe(nodeType, false);
//					if(customizedViewClass != null && ClassService.isImageView(customizedViewClass)) {
//						return true;
//					}
//				}
//			}
//			for(String includeLayout : includes) {
//				if(containsImageView(includeLayout))
//					return true;
//			}
//		}
//		return false;
//	}
	
	public List<Widget> getEventWidget(String layout){
		List<Widget> widgets = new ArrayList<Widget>();
		String layoutPath = "res/layout/" + layout + ".xml";
		AXmlNode rootNode = parseResXmlFile(layoutPath);
		if(rootNode != null) {
			Set<AXmlNode> allNodes = new HashSet<AXmlNode>();
			Set<String> includes = new HashSet<String>();
			allNodes.add(rootNode);
			getAllNode(rootNode, allNodes, includes);
			for(AXmlNode node : allNodes) {
				Widget w = parseAXmlNode(node);
				if(w != null) {
					widgets.add(w);
				}
			}
			for(String includeLayout : includes) {
				widgets.addAll(getEventWidget(includeLayout));
			}
		}else {
			System.out.println("TAG  :con't find layout£¬ path:"+layoutPath);
		}
		return widgets;
	}
	
	private void getAllNode(AXmlNode node, Set<AXmlNode> allNodes, Set<String> includes) {
		for(AXmlNode child : node.getChildren()) {
			if(child.getTag().equals("include")) {
//				System.out.println(child.toString());
				if(child.hasAttribute("layout")) {
					int layoutId = (int) child.getAttribute("layout").getValue();
					System.out.println("!!! included layoutID"+layoutId); //<include layout="@layout/abc_screen_content_include" />
					try {
						String includeLayout = getLayoutNameById(layoutId);
						System.out.println("!!! included  layoutName"+includeLayout);
						includes.add(includeLayout);
					} catch (RecourseMissingException e) {
						Logger.e(TAG, e);
					}
				}
			}else {
//				Logger.i(TAG, child.toString()+" "+child.isIncluded());
				allNodes.add(child);
				List<AXmlNode> chileren = child.getChildren();
				if(!chileren.isEmpty())
					getAllNode(child, allNodes, includes);
			}
		}
	}
	
	private Widget parseAXmlNode(AXmlNode node) {
		if(node.hasAttribute("onClick")) {  //future need add this  "onLongClick""onKey""onTouch"
			String eventMethod = (String) node.getAttribute("onClick").getValue();
			String type = node.getTag();
			Widget widget = new Widget();
			widget.setId(IdProvider.v().widgetId());
			widget.setType(type);  
			widget.setEventMethod(eventMethod);
			widget.setEventType(EventType.CLICK);
			widget.isStatic=true;  //Static registration of event handling callbacks
			if(node.hasAttribute("id")) {
				Object id = node.getAttribute("id").getValue();
				System.out.println("!!! for to see Widget's ID value£º"+id.toString());
				if(id instanceof Integer) {
					int resId = (int) id;
					widget.setResId(resId);
					try {
						String resName = getWidgetNameById(resId);
						widget.setResName(resName);
					}catch (Exception e) {
						Logger.e(TAG, e);
					}
				}
			}
			if(node.hasAttribute("text")) {
				Object textValue = node.getAttribute("text").getValue();
				System.out.println("!!! for to see Widget's text value£º£º"+textValue.toString());
				if(textValue instanceof Integer) {
					int textId = (int)textValue;
					try {
						String str = getStringById(textId);
						widget.setText(str);
					}catch (Exception e) {
						Logger.e(TAG, e);
					}
				}else if(textValue instanceof String) {
					widget.setText((String)textValue);
        		}
			}
			return widget;
		}
		return null;
	}
	//Parameter: layout->onCreate()'s  layout &  widgets->drawer's  sonWidget.
	public Widget getLeftDrawer(String layout, List<Widget> widgets){  
		String layoutPath = "res/layout/" + layout + ".xml";
		AXmlNode rootNode = parseResXmlFile(layoutPath);
		if(rootNode != null) {
			Set<AXmlNode> allNodes = new HashSet<AXmlNode>();
			Set<String> includes = new HashSet<String>();
			allNodes.add(rootNode);
			getAllNode(rootNode, allNodes, includes);//
			for(AXmlNode node : allNodes) {
				if(node.getTag().equals("android.support.v4.widget.DrawerLayout")
						|| node.getTag().equals("androidx.drawerlayout.widget.DrawerLayout")) {
					List<AXmlNode> children = node.getChildren();
					for(AXmlNode child : children) {
						if(child.hasAttribute("layout_gravity")) {  //One of the circumstances£¬£¨There are others :start->Left slide|end->Right slide£©
							String type = child.getAttribute("layout_gravity").getValue().toString();
							if(type.equals("8388611") || type.equals("3")) {
								if(child.getTag().equals("com.google.android.material.navigation.NavigationView")) {
									return getNavigationView(child, widgets);
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public Widget getRightDrawer(String layout, List<Widget> widgets){
		String layoutPath = "res/layout/" + layout + ".xml";
		AXmlNode rootNode = parseResXmlFile(layoutPath);
		if(rootNode != null) {
			Set<AXmlNode> allNodes = new HashSet<AXmlNode>();
			Set<String> includes = new HashSet<String>();
			allNodes.add(rootNode);
			getAllNode(rootNode, allNodes, includes);
			for(AXmlNode node : allNodes) {
				if(node.getTag().equals("android.support.v4.widget.DrawerLayout")
						|| node.getTag().equals("androidx.drawerlayout.widget.DrawerLayout")) {
					List<AXmlNode> children = node.getChildren();
					for(AXmlNode child : children) {
						if(child.hasAttribute("layout_gravity")) {
							String type = child.getAttribute("layout_gravity").getValue().toString();
							if(type.equals("8388613") || type.equals("5")) {
								if(child.getTag().equals("com.google.android.material.navigation.NavigationView")) {
									return getNavigationView(child, widgets);
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	private Widget getNavigationView(AXmlNode node, List<Widget> widgets) {
		Widget widget = new Widget();
		widget.setId(IdProvider.v().widgetId());
		widget.setType(node.getTag());//navigation
		//set res id
		if(node.hasAttribute("id")) {
			Object id = node.getAttribute("id").getValue();
			if(id instanceof Integer) {
				int resId = (int) id;
				widget.setResId(resId);
				try {
					String resName = getWidgetNameById(resId);
					widget.setResName(resName);
				}catch (Exception e) {
					Logger.e(TAG, e);
				}
			}
		}
		//set content-desc
		if(node.hasAttribute("text")) {
			Object textValue = node.getAttribute("text").getValue();
			if(textValue instanceof Integer) {
				int textId = (int)textValue;
				try {
					widget.setText(getStringById(textId));
				}catch (Exception e) {
					Logger.e(TAG, e);
				}
			}else if(textValue instanceof String) {
				widget.setText((String)textValue);
    		}
		}
		//fill menu items
		if(node.hasAttribute("menu")) {
			Object menuId = node.getAttribute("menu").getValue();
			if(menuId instanceof Integer) {
				int menuLayoutId = (int)menuId;
				try {
					String menuLayout = getMenuNameById(menuLayoutId);
					if(menuLayout != null) {
						widgets.addAll(parseMenu(menuLayout));
					}
				}catch (Exception e) {
					Logger.e(TAG, e);
				}
			}
		}
		return widget;
	}
	
	public Map<Integer, String> getNavHostFragment(String layout) {
		Map<Integer, String> itemId_targetFragName = new HashMap<Integer, String>();
		String layoutPath = "res/layout/" + layout + ".xml";
		AXmlNode rootNode = parseResXmlFile(layoutPath);
		if(rootNode != null) {
			Set<AXmlNode> allNodes = new HashSet<AXmlNode>();
			Set<String> includes = new HashSet<String>();
			allNodes.add(rootNode);
			getAllNode(rootNode, allNodes, includes);
			for(AXmlNode node : allNodes) {
				if(node.getTag().equals("fragment")) {
					if(node.hasAttribute("name")) {
						Object name = node.getAttribute("name").getValue();
						if(name instanceof String) {
							String fragName = (String) name;
							if(fragName.equals("androidx.navigation.fragment.NavHostFragment")) {
								if(node.hasAttribute("navGraph")) {
									Object navGraph = node.getAttribute("navGraph").getValue();
									if(navGraph instanceof Integer) {
										int navGraphId = (int) navGraph;
										try {
											String navName = getNavigationNameById(navGraphId);
											String xmlPath = "res/navigation/"+navName+".xml";
											AXmlNode navNode = parseResXmlFile(xmlPath);
											List<AXmlNode> navChildren = navNode.getChildren();
											for(AXmlNode navChild : navChildren) {
												if(navChild.getTag().equals("fragment") && navChild.hasAttribute("name") && navChild.hasAttribute("id")) {
													Object navChildName = navChild.getAttribute("name").getValue();
													if(navChildName instanceof String) {
														String navFragName = (String) navChildName;
														System.out.println("AAA "+navFragName);
														
														Object navChildId = navChild.getAttribute("id").getValue();
														if(navChildId instanceof Integer) {
															int navItemId = (int) navChildId;
															itemId_targetFragName.put(navItemId, navFragName);
														}
													}
												}
											}
										}catch (Exception e) {
											Logger.e(TAG, e);
										}
									}
								}
								return itemId_targetFragName;
							}
						}
					}
				}
			}
			for(String includeLayout : includes) {
				itemId_targetFragName.putAll(getNavHostFragment(includeLayout));
			}
		}
		return itemId_targetFragName;
	}
	
	
	public List<Widget> parseMenu(String menu){
		List<Widget> widgets = new ArrayList<Widget>();
		String menuPath = "res/menu/" + menu + ".xml";
		AXmlNode root = parseResXmlFile(menuPath);
		if(root == null)
			return widgets;
		List<AXmlNode> itemNodes = root.getChildrenWithTag("item");
		if(!itemNodes.isEmpty()) {
			for (AXmlNode itemNode : itemNodes) {
                List<AXmlNode> sub = itemNode.getChildrenWithTag("menu");
                if (sub.isEmpty()) {//itemNode is MenuItem
                	MenuItem menuItem = new MenuItem();
                	menuItem.setId(IdProvider.v().widgetId());
                	AXmlAttribute<Integer> id = (AXmlAttribute<Integer>) itemNode.getAttribute("id");
                	if(id != null) {
                		menuItem.setItemId(id.getValue());
                	}
                	String text = getTitleFromMenuItem(itemNode);
                	if(text != null) {
                		menuItem.setText(text);
                	}
                	widgets.add(menuItem);
                }else {
                	SubMenu subMenu = new SubMenu();
                	subMenu.setId(IdProvider.v().widgetId());
                	AXmlAttribute<Integer> id = (AXmlAttribute<Integer>) itemNode.getAttribute("id");
                	if(id != null) {
                		subMenu.setSubMenuId(id.getValue());
                	}
                	String text = getTitleFromMenuItem(itemNode);
                	if(text != null) {
                		subMenu.setText(text);
                	}
                	
                	List<AXmlNode> subItemNodes = sub.get(0).getChildrenWithTag("item");
                	List<MenuItem> subItems = new ArrayList<MenuItem>();
                	for(AXmlNode subItemNode : subItemNodes) {
                		MenuItem subItem = new MenuItem();
                		subItem.setId(IdProvider.v().widgetId());
                		AXmlAttribute<Integer> subId = (AXmlAttribute<Integer>) subItemNode.getAttribute("id");
                        if (subId != null) {
                            subItem.setItemId(subId.getValue());
                        }
                        String subItemText = getTitleFromMenuItem(subItemNode);
                    	if(subItemText != null) {
                    		subMenu.setText(subItemText);
                    	}
                    	subItems.add(subItem);
                	}
                	subMenu.setItems(subItems);
                	widgets.add(subMenu);
                }
			}
		}
		return widgets;
	}
	
	private String getTitleFromMenuItem(AXmlNode itemNode) {
		if(itemNode.hasAttribute("title")) {
    		Object titleValue = itemNode.getAttribute("title").getValue();
    		if(titleValue instanceof Integer) {
    			int titleId = (int)titleValue;
    			try {
					return getStringById(titleId);
				} catch (RecourseMissingException e) {
					Logger.e(TAG, e);
				}
    		}else if(titleValue instanceof String) {
    			return (String)titleValue;
    		}
    	}
		return null;
	}
	
	

	public Map<String, String> parsePreferenceFile(String fileName){
		Map<String, String> key_title = new HashMap<String, String>();
		File file = new File(apkDir + apkName, "decompile/res/xml/"+fileName+".xml");
		if(!file.exists() || !file.isFile()) {
			Logger.e(TAG, new FileNotFoundException("No such file: " + fileName + ".xml"));
			return key_title;
		}else {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
 			try(FileInputStream fis = new FileInputStream(file);
 				BufferedInputStream bis = new BufferedInputStream(fis)){
 				DocumentBuilder builder = factory.newDocumentBuilder();
 				Document document = builder.parse(bis);
 				Element root = document.getDocumentElement();
 				if(root.hasAttribute("android:key") && root.hasAttribute("android:title")) {
 					String key = root.getAttribute("android:key");
 					
 					String title = root.getAttribute("android:title");  //Here may be the ID, you have to find the string through the ID, right?
 					if(title.contains("@string")) {
 						title = ResConvertor.v().getStringByName(title.replace("@string", ""));
 					}
 					key_title.put(key, title);
 				}
 				if(root.hasChildNodes()) {
 					preferenceN(key_title, root);
 				}
 			} catch (Exception e) {
 				Logger.e(TAG, new AppParseException("Parse public.xml fail", e));
 			}
		}
		return key_title;
	}
	private void preferenceN(Map<String, String> key_title, Element root) {
		NodeList nodeList = root.getChildNodes();
		for(int i = 0; i < nodeList.getLength(); i++) {
			if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE){
				Element element = (Element) nodeList.item(i);
				if(element.hasAttribute("android:key") && element.hasAttribute("android:title")) {
					String key = element.getAttribute("android:key");
 					
 					String title = element.getAttribute("android:title");
 					if(title.contains("@string")) {
 						title = ResConvertor.v().getStringByName(title.replace("@string", ""));
 					}
 					key_title.put(key, title);
				}
				if(element.hasChildNodes()) {
					preferenceN(key_title, element);
				}
			}
		}
	}
	
	/////////////////  Resources  //////////////////
	public String getWidgetNameById(int id) throws RecourseMissingException {
		String res = ResConvertor.v().getWidgetNameById(id);
		if(res != null)
			return res;
		throw new RecourseMissingException("Widget id: " + id);
	}
	public String getLayoutNameById(int id) throws RecourseMissingException {
		String res = ResConvertor.v().getLayoutNameById(id);
		if(res != null)
			return res;
		throw new RecourseMissingException("Layout id: " + id);
	}
	public String getXmlNameById(int id) throws RecourseMissingException {
		String res = ResConvertor.v().getXmlNameById(id);
		if(res != null)
			return res;
		throw new RecourseMissingException("Xml id: " + id);
	}
	public String getMenuNameById(int id) throws RecourseMissingException {
		String res = ResConvertor.v().getMenuNameById(id);
		if(res != null)
			return res;
		throw new RecourseMissingException("Menu id: " + id);
	}
	public String getNavigationNameById(int id) throws RecourseMissingException {
		String res = ResConvertor.v().getNavigationNameById(id);
		if(res != null)
			return res;
		throw new RecourseMissingException("Navigation id: " + id);
	}
	public String getStringById(int id) throws RecourseMissingException {
		String res = ResConvertor.v().getStringById(id);
		if(res != null)
			return res;
		throw new RecourseMissingException("String id: " + id);
	}
	/**
	 * parse R class, and get res name by res id
	 * @author zzg
	 *
	 */
 	private static class ResConvertor {
 		public static void reset() {
 			instance = null;
 		}
 		
 		private static ResConvertor instance = null;
		private ResConvertor() {}
 		public static ResConvertor v() {
 			if(instance == null) {
 				synchronized (ResConvertor.class) {
					if(instance == null) {
						instance = new ResConvertor();
					}
				}
 			}
 			return instance;
 		}

 		/**The mapping of resource name and value of string*/
 		private Map<String, String> STRING_name_value = new HashMap<>();
 		/**The mapping between resource ID and resource name of string*/
		private Map<String, String> STRING_id_name = new HashMap<String, String>();
		/**Mapping of resource ID and resource name of widget*/
		private Map<String, String> WIDGET_id_name = new HashMap<String, String>();
		/**Mapping of resource ID and file name of layout file*/
		private Map<String, String> LAYOUTFILE_id_name = new HashMap<String, String>();
		/**Mapping between resource ID and file name of XML document*/
		private Map<String, String> XMLFILE_id_name = new HashMap<String, String>();
		/**Mapping of resource ID and file name of menu layout file*/
		private Map<String, String> MENUFILE_id_name = new HashMap<String, String>();
		/**Mapping of resource ID and file name of navigation file*/
		private Map<String, String> NAVIGATIONFILE_id_name = new HashMap<String, String>();
 		
 		public void parse(String apkDir, String apkName, List<SootClass> rClasses) {
 			//Parse public.xml or rcass to get the mapping between resource ID and resource name
 			File publicXMLFile = new File(apkDir + apkName, "decompile/res/values/public.xml");
 			if(!publicXMLFile.exists() || !publicXMLFile.isFile()) {
 				Logger.e(TAG, new RecourseMissingException("No public.xml ("+publicXMLFile.getAbsolutePath()+")"));
 				parseRClass(rClasses);
 			}else {
 				parsePublicXmlFile(publicXMLFile);
 			}
 			//Parse strings. XML to get the mapping of string name and value
 			File stringsXMLFile = new File(apkDir + apkName, "decompile/res/values/strings.xml");
			if(!stringsXMLFile.exists() || !stringsXMLFile.isFile()) {
				Logger.e(TAG, new RecourseMissingException("No strings.xml ("+stringsXMLFile.getAbsolutePath()+")"));
//				return;
			}else
				parseStringsXml(stringsXMLFile);
			//output
			write();
 		}
 		
 		private void parsePublicXmlFile(File publicXMLFile) {
 			Logger.i(TAG, "Parse public.xml");
 			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
 			try(FileInputStream fis = new FileInputStream(publicXMLFile);
 				BufferedInputStream bis = new BufferedInputStream(fis)){
 				DocumentBuilder builder = factory.newDocumentBuilder();
 				Document document = builder.parse(bis);
 				NodeList nodeList = document.getElementsByTagName("public");
 				for(int i = 0; i < nodeList.getLength(); i++) {
 					if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE){
 						Element element = (Element) nodeList.item(i);
 						if(element.hasAttribute("type")) {
 							String type = element.getAttribute("type");
 							if(type.equals("layout") || type.equals("menu") 
 									|| type.equals("string") || type.equals("xml")
 									|| type.equals("id") || type.equals("navigation")) {
 								String name = element.getAttribute("name");
 								String id = "0 "+name;
 								String idHex = element.getAttribute("id");
 								if(idHex != null) {
 									int idDec = Integer.parseInt(idHex.substring(2), 16);
 									id = String.valueOf(idDec);
 								}
 								switch (type) {
									case "layout":
										LAYOUTFILE_id_name.put(id, name);
										break;
									case "menu":
										MENUFILE_id_name.put(id, name);
										break;
									case "string":
										STRING_id_name.put(id, name);
										break;
									case "xml":
										XMLFILE_id_name.put(id, name);
										break;
									case "id":
										WIDGET_id_name.put(id, name);
										break;
									case "navigation":
										NAVIGATIONFILE_id_name.put(id, name);
										break;
									default:
										break;
								}
 							}
 						}
 					}
 				}
 				Logger.i(TAG, "Parse public.xml succeful!!");
 			} catch (Exception e) {
 				Logger.e(TAG, new RecourseMissingException("Parse public.xml fail", e));
 			}
 		}
		private void parseRClass(List<SootClass> rClasses) {
 			Logger.i(TAG, "Parse R.class");
			for(SootClass rClass : rClasses) {
				if(rClass.getName().endsWith("R$id")) {
					//resolving R$id
					SootClass rIdClass = rClass;
					Iterator<SootField> ids = rIdClass.getFields().iterator();
					while(ids.hasNext()) {
						SootField idField = ids.next();
						if (idField.isFinal() && idField.isStatic()) {
			                String name = idField.getName();
			                Tag fieldTag = idField.getTag("IntegerConstantValueTag");
			                if (fieldTag != null) {
			                    String tagString = fieldTag.toString();
			                    String fieldValue = tagString.split(" ")[1];
			                    WIDGET_id_name.put(fieldValue, name);
			                }
						}
					}
				}
				//resolving R$layout
				if(rClass.getName().endsWith("R$layout")) {
					SootClass rLayoutClass = rClass;
					Iterator<SootField> ids = rLayoutClass.getFields().iterator();
					while(ids.hasNext()) {
						SootField idField = ids.next();
						if (idField.isFinal() && idField.isStatic()) {
			                String name = idField.getName();
			                Tag fieldTag = idField.getTag("IntegerConstantValueTag");
			                if (fieldTag != null) {
			                    String tagString = fieldTag.toString();
			                    String fieldValue = tagString.split(" ")[1];
			                    LAYOUTFILE_id_name.put(fieldValue, name);
			                }
						}
					}
				}
				//resolving R$xml
				if(rClass.getName().endsWith("R$xml")) {
					SootClass rLayoutClass = rClass;
					Iterator<SootField> ids = rLayoutClass.getFields().iterator();
					while(ids.hasNext()) {
						SootField idField = ids.next();
						if (idField.isFinal() && idField.isStatic()) {
			                String name = idField.getName();
			                Tag fieldTag = idField.getTag("IntegerConstantValueTag");
			                if (fieldTag != null) {
			                    String tagString = fieldTag.toString();
			                    String fieldValue = tagString.split(" ")[1];
			                    XMLFILE_id_name.put(fieldValue, name);
			                }
						}
					}
				}
				//resolving R$string
				if(rClass.getName().endsWith("R$string")) {
					SootClass rLayoutClass = rClass;
					Iterator<SootField> ids = rLayoutClass.getFields().iterator();
					while(ids.hasNext()) {
						SootField idField = ids.next();
						if (idField.isFinal() && idField.isStatic()) {
			                String name = idField.getName();
			                Tag fieldTag = idField.getTag("IntegerConstantValueTag");
			                if (fieldTag != null) {
			                    String tagString = fieldTag.toString();
			                    String fieldValue = tagString.split(" ")[1];
			                    STRING_id_name.put(fieldValue, name);
			                }
						}
					}
				}
				//resolving R$menu
				if(rClass.getName().endsWith("R$menu")) {
					SootClass rLayoutClass = rClass;
					Iterator<SootField> ids = rLayoutClass.getFields().iterator();
					while(ids.hasNext()) {
						SootField idField = ids.next();
						if (idField.isFinal() && idField.isStatic()) {
			                String name = idField.getName();
			                Tag fieldTag = idField.getTag("IntegerConstantValueTag");
			                if (fieldTag != null) {
			                    String tagString = fieldTag.toString();
			                    String fieldValue = tagString.split(" ")[1];
			                    MENUFILE_id_name.put(fieldValue, name);
			                }
			            }
					}
				}
				//resolving R$navigation
				if(rClass.getName().endsWith("R$navigation")) {
					SootClass rLayoutClass = rClass;
					Iterator<SootField> ids = rLayoutClass.getFields().iterator();
					while(ids.hasNext()) {
						SootField idField = ids.next();
						if (idField.isFinal() && idField.isStatic()) {
			                String name = idField.getName();
			                Tag fieldTag = idField.getTag("IntegerConstantValueTag");
			                if (fieldTag != null) {
			                    String tagString = fieldTag.toString();
			                    String fieldValue = tagString.split(" ")[1];
			                    NAVIGATIONFILE_id_name.put(fieldValue, name);
			                }
			            }
					}
				}
			}
		}
		private void parseStringsXml(File stringsXMLFile) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try(FileInputStream fis = new FileInputStream(stringsXMLFile);
				BufferedInputStream bis = new BufferedInputStream(fis)){
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(bis);
				NodeList nodeList = document.getElementsByTagName("string");
				for(int i = 0; i < nodeList.getLength(); i++) {
					if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE){
						Element element = (Element) nodeList.item(i);
						if(element.hasAttribute("name")) {
							STRING_name_value.put(element.getAttribute("name"), element.getTextContent());
						}
					}
				}
				Logger.i(TAG, "Parse strings.xml succeful!!");
			} catch (Exception e) {
				Logger.e(TAG, new RecourseMissingException("Parse strings.xml fail", e));
			}
		}
		private void write() {
			//Output these resources
			IOService.v().writeResources(WIDGET_id_name, LAYOUTFILE_id_name, XMLFILE_id_name, MENUFILE_id_name, NAVIGATIONFILE_id_name);
			//strings.xml
			IOService.v().writeString(STRING_id_name, STRING_name_value);
		}

		/**
		 * This is different from other resources. After getting the name of a string, you need to read the strings. XML document to get the real string of the name
		 */
		public String getStringById(int id) {
			String idStr = String.valueOf(id);
			String stringName = STRING_id_name.get(idStr);
			if(stringName == null) {//maybe id systemRes
				stringName = SystemicResouces.getStringNameById(id);
			}
			if(stringName != null) {
				String str = STRING_name_value.get(stringName);
				if(str == null)
					str = SystemicResouces.getStringByStringName(stringName);
				return str;
			}
			Logger.e(TAG, new RecourseMissingException("String id: " + id));
			return null;
		}
		
		public String getStringByName(String name) {
			return STRING_name_value.get(name);
		}
		
		public String getWidgetNameById(int id) {
			String idStr = String.valueOf(id);
			String widgetName = WIDGET_id_name.get(idStr);
			if(widgetName == null) {//may be is system res
				widgetName = SystemicResouces.getWidgetNameById(id);
			}
			return widgetName;
		}
		public String getLayoutNameById(int id) {
			String idStr = String.valueOf(id);
			return  LAYOUTFILE_id_name.get(idStr);
		}
		public String getXmlNameById(int id) {
			String idStr = String.valueOf(id);
			return XMLFILE_id_name.get(idStr);
		}
		public String getMenuNameById(int id) {
			String idStr = String.valueOf(id);
			return MENUFILE_id_name.get(idStr);
		}
		public String getNavigationNameById(int id) {
			String idStr = String.valueOf(id);
			return NAVIGATIONFILE_id_name.get(idStr);
		}
	}
	///////////////////////////////////////
	
	
	
	
	///////////////  other  ///////////////
	public int loc() {
		int loc = 0;
		for(SootClass sc : Scene.v().getApplicationClasses()) {
			if(!sc.isPhantom() && !Utils.isClassInSystemPackage(sc.getName())) {
				for(SootMethod sm : sc.getMethods()) {
					if(sm.hasActiveBody()) {
						loc += sm.getActiveBody().getUnits().size();
					}
				}
			}
		}
		return loc/1000;
	}
 	
 	public boolean isAppClass(String clsName) {
		for(SootClass sc : allClasses) {
			if(sc.getName().equals(clsName)) {
				return true;
			}
		}
		return false;
	}
	
	public String getActivityByImplicitIntent(String action, String type, Set<String> categories) {
		for(Activity implicitActivity : implicitActivities) {
			if(implicitActivity.match(action, type, categories))
				return implicitActivity.mClassName;
		}
		return null;
	}
	
	
	private class DecompileException extends RuntimeException{
		public DecompileException(String msg, Throwable t) {
			super(msg, t);
		}
	}
	private class AppParseException extends RuntimeException{
		public AppParseException(String msg, Throwable t) {
			super(msg, t);
		}
	}
	
	///////////////////////////////////////
	
 	
	///////////////  Test  ////////////////
	
	public static void main(String[] args) {
		AppParser testApp = AppParser.v();
		testApp.init("F:\\00AAZZG\\0TestAPK\\GooglePlayAPP3\\", "LarkMusicPlayer");
		CallGraph cg = testApp.getCg();
		System.out.println(cg.size());
		printCG(cg);
	}
	private static void printCG(CallGraph cg) {
		StringBuilder sb = new StringBuilder();
		Iterator<Edge> iter = cg.iterator();
		while(iter.hasNext()) {
			Edge e = iter.next();
			sb.append(e.src().getSignature());
			sb.append("\n-->");
			sb.append(e.tgt().getSignature());
			sb.append("\n\n");
		}
		try {
			File f1 = new File("F:\\00AAZZG\\Output", "CG_LarkMusicPlayer 1.txt");
			if (!f1.exists())
				f1.createNewFile();
			FileWriter out1 = new FileWriter(f1, false);
			out1.write(sb.toString());
			out1.close();
		}catch (IOException e){
			
		}
	}
	
}
