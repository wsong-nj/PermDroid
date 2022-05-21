package zzg.staticanalysis;

import zzg.staticguimodel.AppInfo;
import zzg.staticguimodel.StepOneOutput;

import java.awt.RenderingHints.Key;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import soot.G;
import soot.PackManager;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.Body;
import soot.BodyTransformer;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import zzg.staticanalysis.utils.MappingConstants;


public class staticTepOne {
	
	public static String outFolder=Main.apkDir+Main.apkName;  
	public Set<String> methods = new HashSet<String>();//API loacte info
	
	private static staticTepOne instance = null;
	public staticTepOne() {}
	public static staticTepOne v() {
		if(instance == null) {
			synchronized (staticTepOne.class) {
				if(instance == null) {
					instance = new staticTepOne();
				}
			}
		}
		return instance;
	}
	
	//completed AppInfo object 
//	public AppInfo getAppInfo() {
//		AppInfo temp =new AppInfo();  
//		temp=temp.getAppInfo(MappingConstants.apk);//get  pkgname��activity name ��danPermissions
//		temp.getneedMethodsignature(temp);  //danpermissins ->need API
//		return temp;
//	}
	
	
	public static void initialiseSoot(){   
    	G.reset();
    	
    	Options.v().set_src_prec(Options.src_prec_apk);	// Apk first
    	Options.v().set_prepend_classpath(true);
    	 
    	Options.v().set_android_jars(MappingConstants.AndroidPlatforms);
    	Options.v().set_process_dir(Collections.singletonList(MappingConstants.apk));
    	
    	Options.v().set_output_format(Options.output_format_dex );//output set	
    	Options.v().set_output_dir(outFolder);  //������־���APK������ûǩ��
    	
    	Options.v().set_no_bodies_for_excluded(true);	//Do not load bodies for excluded classes
    	Options.v().set_allow_phantom_refs(true);  //Allow unresolved classes; may cause errors
    	Options.v().set_keep_line_number(true);  
    	Options.v().set_no_output_source_file_attribute(true);		
    	Options.v().set_whole_program(true);
    	Options.v().set_process_multiple_dex(true);//Process all DEX files found in APK. 
    	Scene.v().addBasicClass("android.app.Activity", SootClass.SIGNATURES);
	    Scene.v().addBasicClass("android.util.Log", SootClass.SIGNATURES);
	    Scene.v().addBasicClass("java.lang.RuntimeException", SootClass.SIGNATURES);
    	Scene.v().loadNecessaryClasses();
    }
	public static String apkOutFolder() {  
		String path = outFolder ;
		File dir = new File(path);
		if(!dir.exists())
			dir.mkdir();
		return path;
	}
	public boolean getAPILocation() {
		apkOutFolder();
		initialiseSoot();
		
		//���APIλ����Ϣ �� ������--���ڷ���---�漰��API---��API��Ȩ����Ϣ
		AppInfo.v().getAppInfo(Main.apk);  //һ�����Manifest����
        String pkg = AppInfo.v().getPackName();
        String mainActivityName=AppInfo.v().getMainActivityName();
        if(AppInfo.v().getDanPermissoins().size()>0) { 
        	//apk have dangerous permissions, output "dangerousPermissions.txt"
        	writeTxt("dangerousPermissions", AppInfo.v().danPermissoins);
        }else {
        	System.out.println("The application does not have any dangerous permissions,so it don't need to be tested");
        	return false;
        }
        String prefix = getCommonPrefix(mainActivityName,pkg);      //��ǰ׺ȷʵ��ʡһ�����顣������coderд�ľͲ���Ҫ����
        System.out.println("prefix:"+prefix);
        //List<String> signatures =new ArrayList<String>();//
        

		PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {

			protected void internalTransform(final Body b, String phaseName,
					@SuppressWarnings("rawtypes") Map options) {
				SootMethod sm = b.getMethod();
				String className = sm.getDeclaringClass().getName();
				if (!className.contains(prefix)) {  //���˵������ڸ�app��method ����ʦ���Ǹ���������һ����.
					return;
				}

				final PatchingChain<Unit> units = b.getUnits();
				
				// important to use snapshotIterator here
				for (Iterator iter = units.snapshotIterator(); iter.hasNext();) {
					final Unit u = (Unit) iter.next();
					Stmt stmt = (Stmt) u;
					if (stmt.containsInvokeExpr()) {
						InvokeExpr ivExpr=stmt.getInvokeExpr();
						String s = stmt.getInvokeExpr().getMethod().getSignature();   //<android.location.LocationManager: void removeUpdates(android.location.LocationListener)> ����ǩ��
						String pString=isTargetAPI(s);  //����ǣ���API��Ӧ��Ȩ�ޣ����򷵻�"00";
						if (pString!="00") {
							StringBuffer sb = new StringBuffer();
							sb.append("Class :"+sm.getDeclaringClass().getName() + "\n");  //
							sb.append("Method:"+sm.getSubSignature()+ "\n");
							sb.append("API signature:"+s+ "\n");
							sb.append("permission :\""+pString+"\"\n");
							methods.add(sb.toString());
							StepOneOutput soo=new StepOneOutput();
							soo.CLASS=sm.getDeclaringClass().getName();
							soo.METHOD=sm.getSubSignature();
							soo.APISIGNATURE=s;
							soo.PERMISSIONS=pString;
							AppInfo.v().apiLocateInfo.add(soo);
							StringBuffer sbLog = new StringBuffer();
							//   log : ��㷽��ǩ�� @ API����ǩ��
							sbLog.append(pString+":"+sm.getSignature()+":"+s);  
							List<Unit> logUnits=insertLog(sbLog.toString());
							units.insertAfter(logUnits, u);
							
						}else if (s.contains("checkSelfPermission")) {  //�����ͨ�õ�
							/**
							 * 	checkSelfPermission(CONTEXT, Manifest.permission.REQUESTED_PERMISSION)
							 * */
							StringBuffer sb = new StringBuffer();
							
							sb.append("Class :"+sm.getDeclaringClass().getName() + "\n");  //
							sb.append("Method:"+sm.getSubSignature()+ "\n");
							sb.append("API signature:"+s+ "\n");
							String spermission="";     //�õ�stmt�������ĵڶ������������������Ȩ��
							for (int i = 0; i < ivExpr.getArgs().size(); i++) {
								if (ivExpr.getArg(i).getType().toString().equals("java.lang.String")) {
									Value permissionString=ivExpr.getArg(i);
									spermission=permissionString.toString();
									break;
								}
							}
							sb.append("permission :"+ spermission+"\n");
							
							methods.add(sb.toString());
							StepOneOutput soo=new StepOneOutput();
							soo.CLASS=sm.getDeclaringClass().getName();
							soo.METHOD=sm.getSubSignature();
							soo.APISIGNATURE=s;
							soo.PERMISSIONS=spermission;
							soo.PERMISSIONS=soo.PERMISSIONS.replaceAll("\"", " ");
							AppInfo.v().apiLocateInfo.add(soo);
							
							StringBuffer sbLog = new StringBuffer();
							//   log : ��㷽��ǩ�� @ API����ǩ��
							sbLog.append(spermission+":"+sm.getSignature()+":"+s);  
							List<Unit> logUnits=insertLog(sbLog.toString());
							units.insertAfter(logUnits, u);
							
						}else if (s.contains("requestPermissions")) {  //ͨ��
							/**
							 *  requestPermissions(CONTEXT,new String[] { Manifest.permission.REQUESTED_PERMISSION },REQUEST_CODE);
							 * */
							StepOneOutput soo=new StepOneOutput();
							soo.CLASS=sm.getDeclaringClass().getName();
							soo.METHOD=sm.getSubSignature();
							soo.APISIGNATURE=s;
							//soo.PERMISSIONS=null;
							StringBuffer sb = new StringBuffer();
							
							sb.append("Class :"+sm.getDeclaringClass().getName() + "\n");  //java.lang.String[]
							sb.append("Method:"+sm.getSubSignature());
							sb.append("\nAPI signature:"+s+ "\n");
							sb.append("permission :");
							//Value permissionString=ivExpr.getArg(0).getType();     //�õ�stmt�������ĵڶ�������
							Value v=null;
							for (int i = 0; i < ivExpr.getArgs().size(); i++) {
								if (ivExpr.getArg(i).getType().toString().equals("java.lang.String[]")) {
									v=ivExpr.getArg(i);
									System.out.println("Value:1"+v);
									break;
								}
							}
							String vs=v.toString();
							System.out.println("Value2:"+vs);
							
							if (vs.contains("android")) {  //ֱ���ҵ��� �������Ȩ�ޣ�
								sb.append(vs);
								soo.PERMISSIONS=vs;
							}else {      
								//ǰ������ 
								UnitGraph cfg = new BriefUnitGraph(b);
								List<Unit> pres = cfg.getPredsOf(stmt);
								System.out.println("��ǰ����"+pres.size()+"�����");
								for (Unit pre : pres) {
									System.out.println("��䣺" + pre.toString());
									if (pre instanceof AssignStmt) {
										Value ri = ((AssignStmt) pre).getRightOp();
										System.out.println(pre.getClass());
										System.out.println(ri.getClass().getName());
										Value le = ((AssignStmt) pre).getLeftOp();
										System.out.println(le.getClass().getName());
										if (le instanceof ArrayRef) {
											Value baseValue = ((ArrayRef) le).getBase();
											if (baseValue.equivTo(v)) {
												sb.append(ri.toString()+" ");
												soo.PERMISSIONS+=ri.toString()+" ";
												
											}
										}
									}
								}
								//��ǰ����
							}						
							sb.append("\n");
							methods.add(sb.toString());
							
							if (soo.PERMISSIONS!=null) {
								soo.PERMISSIONS=soo.PERMISSIONS.replaceAll("null","");
								soo.PERMISSIONS=soo.PERMISSIONS.replaceAll("\"", "");
								soo.PERMISSIONS=soo.PERMISSIONS.replaceAll(" ", "");
							}
							AppInfo.v().apiLocateInfo.add(soo);
							
							StringBuffer sbLog = new StringBuffer();
							//   log : ��㷽��ǩ�� @ API����ǩ��
							sbLog.append(soo.PERMISSIONS+":"+sm.getSignature()+":"+s);  
							List<Unit> logUnits=insertLog(sbLog.toString());
							units.insertAfter(logUnits, u);
						}else if (s.contains("ensurePermission")) {  //����
							
							StringBuffer sb = new StringBuffer();
							
							sb.append("Class :"+sm.getDeclaringClass().getName() + "\n");  //
							sb.append("Method:"+sm.getSubSignature()+ "\n");
							sb.append("API signature:"+s+ "\n");
							String spermission="no";     //�õ�stmt�������ĵڶ������������������Ȩ��
							for (int i = 0; i < ivExpr.getArgs().size(); i++) {
								if (ivExpr.getArg(i).getType().toString().equals("java.lang.String")) {
									Value permissionString=ivExpr.getArg(i);
									spermission=permissionString.toString();
									break;
								}
							}
							sb.append("permission :"+ spermission+"\n");
							
							methods.add(sb.toString());
							StepOneOutput soo=new StepOneOutput();
							soo.CLASS=sm.getDeclaringClass().getName();
							soo.METHOD=sm.getSubSignature();
							soo.APISIGNATURE=s;
							soo.PERMISSIONS=spermission;
							soo.PERMISSIONS=soo.PERMISSIONS.replaceAll("\"", " ");
							AppInfo.v().apiLocateInfo.add(soo);
							
							StringBuffer sbLog = new StringBuffer();
							//   log : ��㷽��ǩ�� @ API����ǩ��
							sbLog.append(spermission+":"+sm.getSignature()+":"+s);  
							List<Unit> logUnits=insertLog(sbLog.toString());
							units.insertAfter(logUnits, u);
							
						}
					}//if (stmt.containsInvokeExpr()) {
				}
			}

		}));
        PackManager.v().runPacks();
        String unsignedString=outFolder+File.separator+Main.apkName;
        String siginedAPK=outFolder+File.separator+Main.apkName+"_resigned";
        
        File file=new File(unsignedString+".apk");
        
        if(!file.exists()) {
        	PackManager.v().writeOutput();
        	resignApk(unsignedString,siginedAPK);
        }
        if(methods.isEmpty()) {
        	System.out.println("  Dangerous permission Association APIs haven't used!");
        	return false;
        }else {
        	methods.add("\nsize:"+methods.size());
        	writeTxt("APILocation", methods);
        }
        //ͬ���ļ��� 
        
        //String siginAPK=outFolder+File.separator+Main.apkName+"_resigned";
		//outFolder=Main.apkDir+Main.apkName
		
        
		return true;	
	}
	
	public String  isTargetAPI(String SootMethodSignature ) {
		if(SootMethodSignature.contains("checkSelfPermission") || SootMethodSignature.contains("requestPermissions") ||SootMethodSignature.contains("ensurePermission")) {  //����������������
			return "00";
		}
		
		String temp=null;
		for (String key:AppInfo.v().danPermissoins) {
			 String[] apiSignatures= MappingConstants.permissionToMethodSignatures.get(key);
			 for (String api:apiSignatures) {
				if (api.equals(SootMethodSignature)) {
					temp+="android.permission."+key+"  ";//�����֣�һ����break����һ���Ƕ�����
					break;
				}
			}
		}
		
		if (temp!=null) {
			return temp.replaceAll("null", "");
		}else {
			return "00"; 
		}
		
	}
	
	public String  isTestingPoint(Stmt stmt,Body b) {
		//�÷��������ж��Ƿ��ǲ��Ե� ����ʱ���õõ��漰��Ȩ�ޣ���Ҫʱ������һ������permissions����ʾ��API�漰������ЩΣ��Ȩ�ޡ�
		//����Ĳ�����
		//sootmethod Ӧ���� ��stmt->isInvokeEpr ->getInvokeEpr->  ivEpr.getmethod()=sootMethod; 
		//���� ��Stmt stmt = (Stmt) u;SootMethod sootMethod=stmt.getInvokeExpr().getMethod();
		//���̣��ȵõ��÷���ǩ��������÷����� is "checkSelfPermission" or "requestPermissions"  ���� true ��
		InvokeExpr invokeExpr = stmt.getInvokeExpr();
		SootMethod invokee = invokeExpr.getMethod();
		String SootMethodSignature =invokee.getSignature();
		
		if (SootMethodSignature.contains("checkSelfPermission")) {
			String spermission="";     //�õ�stmt�������ĵڶ������������������Ȩ��
			for (int i = 0; i < invokeExpr.getArgs().size(); i++) { 
				if (invokeExpr.getArg(i).getType().toString().equals("java.lang.String")) {
					Value permissionString=invokeExpr.getArg(i);
					spermission=permissionString.toString();
					break;
				}
			}
			if(spermission.length()>=3) {
				return spermission;
			}else {
				return "01";
			}
			
		}else if(SootMethodSignature.contains("requestPermissions")){
			//��ǰ���������ܵõ������Ȩ�ޣ���Ҫ  ��� ���body����˿��Դ��� sootmethod
			/**
			 *  requestPermissions(CONTEXT,new String[] { Manifest.permission.REQUESTED_PERMISSION },REQUEST_CODE);
			 * */
			Value v=null;
			for (int i = 0; i < invokeExpr.getArgs().size(); i++) {
				if (invokeExpr.getArg(i).getType().toString().equals("java.lang.String[]")) {
					v=invokeExpr.getArg(i);
					break;
				}
			}
			String vs=v.toString();
			String pString="";
			if (vs.contains("permission")) {  
				return vs;
			}else {      
				//ǰ������ 
				UnitGraph cfg = new BriefUnitGraph(b);
				List<Unit> pres = cfg.getPredsOf(stmt);
				System.out.println("��ǰ����"+pres.size()+"�����");
				for (Unit pre : pres) {
					System.out.println("��䣺" + pre.toString());
					if (pre instanceof AssignStmt) {
						Value ri = ((AssignStmt) pre).getRightOp();
						System.out.println(pre.getClass());
						System.out.println(ri.getClass().getName());
						Value le = ((AssignStmt) pre).getLeftOp();
						System.out.println(le.getClass().getName());
						if (le instanceof ArrayRef) {
							Value baseValue = ((ArrayRef) le).getBase();
							if (baseValue.equivTo(v)) {
								pString+=ri.toString()+" ";
								
							}
						}
					}
				}
				//��ǰ����
			}						
			
			if(pString.length()>=3) {
				return pString;
			}else {
				return "01";
			}
			
		}else if (SootMethodSignature.contains("ensurePermission")) {  //����
			String spermission="no";     //�õ�stmt�������ĵڶ������������������Ȩ��
			for (int i = 0; i < invokeExpr.getArgs().size(); i++) { 
				if (invokeExpr.getArg(i).getType().toString().equals("java.lang.String")) {
					Value permissionString=invokeExpr.getArg(i);
					spermission=permissionString.toString();
					break;
				}
			}
			if(spermission.length()>=3) {
				return spermission;
			}else {
				return "01";
			}
			
		}
		String temp="";
		for (String key:AppInfo.v().danPermissoins) {
			 String[] apiSignatures= MappingConstants.permissionToMethodSignatures.get(key);
			 for (String api:apiSignatures) {
				if (api.equals(SootMethodSignature)) {
					temp+="android.permission."+key+"  ";//�����֣�һ����break����һ���Ƕ�����
					break;
				}
			}
		}
		
		if (temp.length()>=3) {
			return temp;
		}else {
			return "00"; 
		}
		
	}
	
	//
	public void writeTxt(String outPathName,Set<String> contents) {
		FileWriter out = null;
		
		try {
			File folder = new File(outFolder);
			if(!folder.exists())
				{folder.mkdirs();}
			File hookClasses = new File(outFolder +"\\"+outPathName+ ".txt");
			if (!hookClasses.exists()) {
				hookClasses.createNewFile();}
			else {
				hookClasses.delete();
				hookClasses.createNewFile();
			}
			out = new FileWriter(hookClasses,true);
			Iterator<String> it=contents.iterator();
			while (it.hasNext()) {
				String c=it.next();
				out.append(c+"\r");
			}
			out.close();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	public void writeTxt(String outPathName,List<String> contents) {
		FileWriter out = null;
		
		try {
			File folder = new File(outFolder);  //  locateAPI
			if(!folder.exists())
				{folder.mkdirs();}
			File hookClasses = new File(outFolder +File.separator+outPathName+ ".txt");  //
			if (!hookClasses.exists()) {
				hookClasses.createNewFile();}
			else {
				hookClasses.delete();
				hookClasses.createNewFile();
			}
			out = new FileWriter(hookClasses,true);
			
			for(String c:contents) {  						
					out.append(c+"\r");
			}
			out.close();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//�õ�String��ǰ������
    private static String getCommonPrefix(String mainActivity,String pkg) {
    	if(mainActivity==null || mainActivity.startsWith(".") || mainActivity.startsWith(pkg))
    		return pkg;
//    	System.out.println("main:"+mainActivity);
//    	System.out.println("pkg:"+pkg);
    	String[] partStrings=mainActivity.split("\\.");
    	//System.out.println("size"+partStrings.length);
    	String temString=null;
    	if (partStrings.length>=2) {
			temString=partStrings[0]+"."+partStrings[1];
		}
    	return temString.length()>0?temString:pkg;
    }
	//��װlog
    private static List<Unit> insertLog(String methodName){
		List<Unit> list = new ArrayList<>();
		SootClass logClass=Scene.v().getSootClass("android.util.Log");// 
        SootMethod sootMethod=logClass.getMethod("int w(java.lang.String,java.lang.String)");  
        StaticInvokeExpr staticInvokeExpr=Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(),StringConstant.v("clickTag"),StringConstant.v(methodName));  
        InvokeStmt invokeStmt=Jimple.v().newInvokeStmt(staticInvokeExpr);
        list.add(invokeStmt);
        return list;
	}
  //��׮����ǩ��
    public static boolean resignApk(String apkDir,String  Name) {
		String[] command = new String[]{"./preprocess\\resignApk.bat", apkDir,Name};  //"./preprocess\\resignApk.bat"; ���·�����ļ���  
		return runCmd(command, true);
	}
    public static boolean runCmd(String[] command, boolean isWaitForEnd) {
		StringBuffer sb = new StringBuffer();
		BufferedReader reader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
			reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			
			while(true) {
				String line = reader.readLine();
				if(line == null)
					break;
				sb.append(line);
			}
			if(isWaitForEnd)
				process.waitFor();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				if(reader != null)
					reader.close();
				if(process != null)
					process.destroy();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
		}
		if(sb.toString().length() <= 0) {
			System.out.println(Arrays.asList(command) + " success");
			System.out.println(sb.toString());
			return true;			
		}
		else {
			System.out.println(sb.toString());
			return false;
		}
	}
}
