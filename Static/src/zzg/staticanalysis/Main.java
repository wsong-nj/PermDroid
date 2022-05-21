package zzg.staticanalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import mySQL.EdgeDB;
import mySQL.WidgetDB;
import mySQL.WindowDB;
import mySQL.clearDB;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import zzg.staticanalysis.analyzer.EventAnalyzer;
import zzg.staticanalysis.generator.node.ActivityNodeGenerator;
import zzg.staticanalysis.generator.node.FragmentNodeGenerator;
import zzg.staticanalysis.model.ActNode;
import zzg.staticanalysis.model.BaseNode;
import zzg.staticanalysis.model.FragNode;
import zzg.staticanalysis.model.Transition;
import zzg.staticanalysis.utils.ClassService;
import zzg.staticanalysis.utils.IOService;
import zzg.staticanalysis.utils.IdProvider;
import zzg.staticanalysis.utils.Logger;
import zzg.staticanalysis.utils.MethodService;
import zzg.staticanalysis.utils.Utils;
import zzg.staticanalysis.utils.MappingConstants;
import zzg.staticguimodel.Graph;
import zzg.staticguimodel.Node;
import zzg.staticguimodel.NodeType;
import zzg.staticguimodel.StepOneOutput;
import zzg.staticguimodel.Widget;
import zzg.staticguimodel.AppInfo;
import zzg.staticguimodel.Edge;
import zzg.staticanalysis.staticTepOne;


public class Main {
	private static final String TAG = "[Main]";
	public static String androidPlatformLocation = "E:\\android-sdk-windows\\platforms";
	public static String apkDir = "F:\\";//F:\\APK-ysh\\socialApps33\\
	public static String apkName = "goodweather";//timberfoss_21
	public static String apk = apkDir+apkName+".apk";
	//public static File totalRes = new File("G:\\APK\\result\\totalRes.txt");  
	
	
	public static void main(String[] args) {
		if (staticTepOne.v().getAPILocation()==true) {
				runApp(); 
		}
	}
	public static void runApp() {
		Main m = new Main();
		long startTime = System.currentTimeMillis();
		m.analysis();
		long endTime = System.currentTimeMillis();
        long executeTime = (endTime - startTime)/1000;
		Logger.i(TAG, "End, took "+executeTime+"s");
		Logger.i(TAG, "LOC: "+AppParser.v().loc());
	}
	public void analysis() {
		AppParser.v().init(apkDir, apkName);
		nodesGenerate();
		transitionsGenerate();
		buildGraph();
	}
	private void nodesGenerate() {
		
		Logger.i(TAG, "============ Fragment Nodes ============");
		for(SootClass frag : AppParser.v().getFragments()) {
			FragmentNodeGenerator.build(frag);
		}
		Logger.i(TAG, "============ Fragment Nodes ============");
		Logger.i(TAG, "============ Activity Nodes ============");
		for(SootClass act : AppParser.v().getActivities()) {
			ActivityNodeGenerator.build(act);
		}
		Logger.i(TAG, "============ Activity Nodes =============");
	}
	
	private void transitionsGenerate() {
		Logger.i(TAG, "============ Transitions ============");
		for(BaseNode baseNode : Manager.v().getNodes()) { 
			if(baseNode.getNodeType().equals(NodeType.DIALOG)) {
				List<Widget> ws = baseNode.getWidgets();
				for(Widget w : ws) {
					if(w.getEventHandler() != null) {
						EventAnalyzer.analysis(w.getEventHandler(), baseNode, w);  //���������ǽ��õ��ı߼�¼�ˣ���������һ��
					}
				}
			}
		}
		
		for(FragNode fragNode : Manager.v().getFragNodes()) {
			//���� fragment��OptionsMenu�ı�
			if(fragNode.getOptionsMenu() != null) {
				BaseNode optionsMenu = fragNode.getOptionsMenu();
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setSrc(fragNode.getId());
				t.setTgt(optionsMenu.getId());
				t.setLabel("Open OptionsMenu");
				//ȱ��add�ɣ��Ҽӵ�
				Manager.v().add(t);
			}
			//���� Fragment��Fragment�ı�
			for(String fragName : fragNode.getFragmentsName()) {
				FragNode tgtFragNode = Manager.v().getFragNodeByName(fragName);
				if(tgtFragNode != null && Manager.v().hasTransition(fragNode.getId(), tgtFragNode.getId()))
					continue;
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setSrc(fragNode.getId());
				if(tgtFragNode != null) {
					t.setTgt(tgtFragNode.getId());
					t.setLabel("Load");
				}else {
					t.setLabel("MTN["+fragName+"]");
				}
				//ȱ��add�ɣ��Ҽӵ�
				Manager.v().add(t);
			}
			
			List<Widget> ws = fragNode.getWidgets();
			for(Widget w : ws) {
				if(w.getEventHandler() != null) {
					EventAnalyzer.analysis(w.getEventHandler(), fragNode, w); 
				}else if(w.getEventMethod() != null && w.getEventMethod().equals(Utils.OPENCONTEXTMENU)) {//�����ô���ܰ���
					BaseNode contextMenu = fragNode.getContextMenu();
					if(contextMenu != null) {
						Transition t = new Transition();
						t.setId(IdProvider.v().edgeId());
						t.setTgt(contextMenu.getId());
						t.setSrc(fragNode.getId());
						t.setWidget(w);
						//ȱ��add�ɣ��Ҽӵ�
						Manager.v().add(t);
					}
				}
			}
		}
		
		for(ActNode actNode : Manager.v().getActNodes()) {
			List<Widget> ws = actNode.getWidgets();
			for(Widget w : ws) {
				if(w.getEventHandler() != null) {
					EventAnalyzer.analysis(w.getEventHandler(), actNode, w);
				}else if(w.getEventMethod() != null && w.getEventMethod().equals(Utils.OPENCONTEXTMENU)) {
					BaseNode contextMenu = actNode.getContextMenu();
					if(contextMenu == null)
						continue;
					Transition t = new Transition();
					t.setId(IdProvider.v().edgeId());
					t.setTgt(contextMenu.getId());
					t.setSrc(actNode.getId());
					t.setWidget(w);
					//ȱ��add�ɣ��Ҽӵ�
					Manager.v().add(t);
				}
			}
			//���� Activity��OptionsMenu�ı�
			if(actNode.getOptionsMenu() != null) {
				BaseNode optionsMenu = actNode.getOptionsMenu();
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setSrc(actNode.getId());
				t.setTgt(optionsMenu.getId());
				t.setLabel("Open OptionsMenu");
				//ȱ��add�ɣ��Ҽӵ�
				Manager.v().add(t);
			}
			//���� Activity��Drawer�ı�
			if(actNode.getLeftDrawer() != null) {
				BaseNode leftDrawer = actNode.getLeftDrawer();
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setSrc(actNode.getId());
				t.setTgt(leftDrawer.getId());
				t.setLabel("Open left Drawer");
				//ȱ��add�ɣ��Ҽӵ�               
				Manager.v().add(t);
			}
			if(actNode.getRightDrawer() != null) {
				BaseNode rightDrawer = actNode.getLeftDrawer();
				Transition t = new Transition();
				t.setId(IdProvider.v().edgeId());
				t.setSrc(actNode.getId());
				t.setTgt(rightDrawer.getId());
				t.setLabel("Open right Drawer");
				//ȱ��add�ɣ��Ҽӵ�
				Manager.v().add(t);
			}
			//���� Activity�е�Fragment��OptinsMenu | Drawer�ı�  
			//�Լ� Activity���ܵ���Fragment�ı�
			if(!actNode.getFragmentsName().isEmpty()) {
				BaseNode optionsMenu = actNode.getOptionsMenu();
				BaseNode leftDrawer = actNode.getLeftDrawer();
				BaseNode rightDrawer = actNode.getLeftDrawer();
				for(String fragName : actNode.getFragmentsName()) {
					FragNode fragNode = Manager.v().getFragNodeByName(fragName);//������ҵ���
					if(fragNode == null || Manager.v().hasTransition(actNode.getId(), fragNode.getId()))
						continue;
					//Activity���ܵ���Fragment�ı�
					Transition t1 = new Transition();
					t1.setId(IdProvider.v().edgeId());
					t1.setSrc(actNode.getId());
					t1.setTgt(fragNode.getId());
					t1.setLabel("Load");
					//ȱ��add�ɣ��Ҽӵ�
					Manager.v().add(t1);
					//���� Activity�е�Fragment��OptinsMenu | Drawer�ı�
					if(optionsMenu != null || leftDrawer != null || rightDrawer != null) {
						if(optionsMenu != null) {
							Transition t = new Transition();
							t.setId(IdProvider.v().edgeId());
							t.setSrc(fragNode.getId());
							t.setTgt(optionsMenu.getId());
							t.setLabel("Open OptionsMenu");
							//ȱ��add�ɣ��Ҽӵ�
							Manager.v().add(t);
						}
						if(leftDrawer != null) {
							Transition t = new Transition();
							t.setId(IdProvider.v().edgeId());
							t.setSrc(fragNode.getId());
							t.setTgt(leftDrawer.getId());
							t.setLabel("Open left Drawer");
							//ȱ��add�ɣ��Ҽӵ�
							Manager.v().add(t);
						}
						if(rightDrawer != null) {
							Transition t = new Transition();
							t.setId(IdProvider.v().edgeId());
							t.setSrc(fragNode.getId());
							t.setTgt(rightDrawer.getId());
							t.setLabel("Open right Drawer");
							//ȱ��add�ɣ��Ҽӵ�
							Manager.v().add(t);
						}
					}
				}
			}
		}
		Logger.i(TAG, "=====================================");
	}
	
	private void buildGraph() {
		myFindMissTestPoint();  //���������ô���� 
		Graph graph = Manager.v().buildGraph(); 
		writeResult(graph);   
		writeSQL(graph);
	}
	
	private void writeResult(Graph graph) {
		IOService.v().writeResult(graph);  	// .bat
		IOService.v().writeResultCsv(graph);   // *.csv
		StringBuilder sb = new StringBuilder();
		sb.append("Node size: ").append(graph.getNodeSize());
		sb.append("widget size: ").append(graph.getWidgets().size());
		sb.append("Edge size: ").append(graph.getEdges().size()).append("\n");
		
		StringBuilder sb1 = new StringBuilder();
		sb1.append(sb).append(graph.toString());
		IOService.v().writeResultString(sb1.toString());  //gSTG.txt
		
		System.out.println("-----------------Result-----------------");
		System.out.println(sb.toString());
	}
	private void writeSQL(Graph graph) {
		List<Node> windows =graph.getNodes();
		Set<Widget> widgets=graph.getWidgets();
		List<Edge> edges=graph.getEdges();
		clearDB.v().clearTable(); 
		Iterator<Node> iteratorWindows =windows.iterator();
		while (iteratorWindows.hasNext()) {
			Node node = (Node) iteratorWindows.next();
			WindowDB.v().insertWindow(node);
		}
		Iterator<Widget> iteratorWidget= widgets.iterator();
		while (iteratorWidget.hasNext()) {
			Widget widget = (Widget) iteratorWidget.next();
			WidgetDB.v().insertWidget(widget);
		}
		Iterator<Edge> iteratorEdge=edges.iterator();
		while (iteratorEdge.hasNext()) {
			Edge edge = (Edge) iteratorEdge.next();
			EdgeDB.v().insertEdge(edge);
		}
	}
	
	//��act/frag���ڲ���API ���㵽���У�istest�ı䣬���������TestingPoint.txt�� 
	public void myFindMissTestPoint() {
		Set<StepOneOutput> apiinfo2=AppInfo.v().apiInfo22;
		Iterator<StepOneOutput>  iterator=AppInfo.v().apiLocateInfo.iterator();
		while (iterator.hasNext()) {
			StepOneOutput stepOneOutput = (StepOneOutput) iterator.next();
			if (isStgAPIContainsTheOne(stepOneOutput,apiinfo2)) {
				continue;
			}else if (stepOneOutput.inAct()!="NO") {
				Manager.v().getActNodeByName(stepOneOutput.inAct()).setTest(true);
				StepOneOutput soo=new StepOneOutput();
				soo.CLASS=stepOneOutput.CLASS;
				soo.METHOD=stepOneOutput.METHOD;
				soo.APISIGNATURE=stepOneOutput.APISIGNATURE;
				soo.PERMISSIONS="permission";
				AppInfo.v().apiInfo22.add(soo);
				
				StringBuffer sb = new StringBuffer();
				sb.append("___***start***____" + "\n");
				sb.append("Class :"+stepOneOutput.CLASS + "\n");  //
				sb.append("Method:"+stepOneOutput.METHOD);
				sb.append("\nAPI signature:"+stepOneOutput.APISIGNATURE+ "\n");
				sb.append("permission :\"permission"+"\"\n");
				sb.append("____***end***_____" + "\n");
				IOService.v().writeTestingPoint(sb.toString());
				
				
			} else if (stepOneOutput.inFrag()!="NO") {
				Manager.v().getFragNodeByName(stepOneOutput.inFrag()).setTest(true);
				StepOneOutput soo=new StepOneOutput();
				soo.CLASS=stepOneOutput.CLASS;
				soo.METHOD=stepOneOutput.METHOD;
				soo.APISIGNATURE=stepOneOutput.APISIGNATURE;
				soo.PERMISSIONS="permission";
				AppInfo.v().apiInfo22.add(soo);
				
				StringBuffer sb = new StringBuffer();
				sb.append("___***start***____" + "\n");
				sb.append("Class :"+stepOneOutput.CLASS + "\n");  //
				sb.append("Method:"+stepOneOutput.METHOD);
				sb.append("\nAPI signature:"+stepOneOutput.APISIGNATURE+ "\n");
				sb.append("permission :\"permission"+"\"\n");
				sb.append("____***end***_____" + "\n");
				IOService.v().writeTestingPoint(sb.toString());
			}
				
			
		}
	}
	
	
	
	
	public  boolean isStgAPIContainsTheOne(StepOneOutput apiLocateInfo,Set<StepOneOutput> apiinfo22 ) {
		Iterator<StepOneOutput>  iterator=apiinfo22.iterator();
		while (iterator.hasNext()) {
			StepOneOutput stepOneOutput = (StepOneOutput) iterator.next();
			if (apiLocateInfo.isEqual(stepOneOutput)) {
				return true;   //apiInfo22(STG�Ѿ��ҵ��˸�API�����ÿ�������ˡ�)
			}
		}
		return false;
	}
	
	
	
	
	//���������staticTopOneһ��
	private void findMissingTestingPoint() {  //������Ƿ�����©�Ĳ��Ե㣬�������еĲ��Ե㡣AllClasses--> AllMethods-->AllUnits
		Logger.i(TAG, "Start to find missing testing-point...");
		Set<String> recyclerAdapters = new HashSet<String>();
		Set<String> baseAdapters = new HashSet<String>();
		Set<String> methodsx = new HashSet<String>();  //���ڴ�� APIinfo
		for(SootClass sc : AppParser.v().getAllClasses()) {
			if(ClassService.isRecyclerViewAdapter(sc)) {
				recyclerAdapters.add(sc.getName());
			}else if(ClassService.isAdapter(sc)) {
				baseAdapters.add(sc.getName());
			}
			for(SootMethod sm : sc.getMethods()) {
				Body body = null;
				try {
					body = sm.retrieveActiveBody();
				}catch (RuntimeException e) {
				}
				if(body != null) {
					for(Unit unit : body.getUnits()) {
						Stmt stmt = (Stmt)unit;
						if(stmt.containsInvokeExpr()) {
							SootMethod invokee = stmt.getInvokeExpr().getMethod();
							//��������API��λ���
//							if(MethodService.isImageAPI(invokee)) {
//								Utils.buildTestPointMsg(sm, invokee);																				
//								
//							}
							//������������ܵõ�����API��λ��Ϣ�� �����ϸ�StaticTepOneһ��
							String perString=staticTepOne.v().isTestingPoint(stmt,body);
							if(perString!="00") {
								StringBuffer sBuffer=new StringBuffer();
								sBuffer.append("___***start***____" + "\n");
								sBuffer.append("Class :"+sm.getDeclaringClass().getName() + "\n");  //
								sBuffer.append("Method:"+sm.getSubSignature());
								sBuffer.append("\nAPI signature:"+invokee.getSignature()+ "\n");
								sBuffer.append("permission :\""+perString+"\"\n");
								sBuffer.append("____***end***_____" + "\n");
								methodsx.add(sBuffer.toString());
								
							}
							
						}
					}
				}
			}
		}
		IOService.v().writeAdapterClasses(recyclerAdapters, baseAdapters);
		Logger.i(TAG, "end to find missing testing-point...");
		if(methodsx.isEmpty()) {
        	System.out.println("  Dangerous permission Association APIs haven't used!");
        	
        }else {
        	methodsx.add("\nsize:"+methodsx.size());
        	staticTepOne.v().writeTxt("APILocation22", methodsx);
        }
		
		
	}
	
	
	private void writer(File file, String text, boolean append) throws IOException {
		if(!file.exists() || !file.isFile()){
			file.createNewFile();
		}
		try(FileOutputStream fileOutputStream = new FileOutputStream(file, append);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter)){
            bufferedWriter.append(text).append("\r\n");
		}
	}
	
	
	
	// *****useless code is put here.*****
	
//	public static void runApps() {
//	String mApkDir = "F:\\00AAZZG\\0TestAPK\\Fdroid\\4\\";
//	File dir = new File(mApkDir);
//	if(!dir.exists() || !dir.isDirectory()) {
//		Logger.i(TAG, dir.getAbsolutePath()+" is not a directory");
//		System.exit(0);
//	}
//	
//	File[] apkFiles = dir.listFiles(new FileFilter() {
//		
//		@Override
//		public boolean accept(File arg0) {
//			if(arg0.getAbsolutePath().endsWith(".apk"))
//				return true;
//			return false;
//		}
//	});
//	Main m = new Main();
//	for(File apkFile : apkFiles) {
//		String mApkName = apkFile.getName().replace(".apk", "");
//		System.out.println(apkFile.getName());
//		try {
//			m.analysis_(mApkDir, mApkName);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
////	try {
////		m.writer(totalRes, String.valueOf(size), true);
////	} catch (IOException e) {
////		e.printStackTrace();
////	}
//}
//public void analysis_(String mApkDir, String mApkName) throws Exception {
//	writer(totalRes, "===============================", true);
//	writer(totalRes, "APK: "+mApkName, true);
//	Logger.i(TAG, "===============================");
//	Logger.i(TAG, "DIR: "+mApkDir);
//	Logger.i(TAG, "APK: "+mApkName);
//	Logger.i(TAG, "Start to build gSTG...");
//	AppParser.reset();
//	long startTime = System.currentTimeMillis();
//	AppParser.v().init(mApkDir, mApkName);
//	nodesGenerate();
//	transitionsGenerate();
//	buildGraph();
//	long endTime = System.currentTimeMillis();
//    long executeTime = (endTime - startTime)/1000;
//	Logger.i(TAG, "End, took "+executeTime+"s");
//	Logger.i(TAG, "LOC: "+AppParser.v().loc());
//	Logger.i(TAG, "===============================");
//	writer(totalRes, "End, took "+executeTime+"s", true);
//	writer(totalRes, "LOC: "+AppParser.v().loc()+"\n", true);
//	writer(totalRes, "===============================", true);
//}
}
