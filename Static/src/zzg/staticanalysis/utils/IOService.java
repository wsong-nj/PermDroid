package zzg.staticanalysis.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jasmin.sym;
import zzg.staticanalysis.AppParser;
import zzg.staticguimodel.Edge;
import zzg.staticguimodel.Graph;
import zzg.staticguimodel.Node;
import zzg.staticguimodel.Widget;

public class IOService {
	private static final String TAG = "[IOService]";
	
	public static void reset() {
		instance = null;
	}
	
	private static IOService instance = null;
	private IOService() {}
	public static IOService v() {
		if(instance == null) {
			synchronized (IOService.class) {
				if(instance == null) {
					instance = new IOService();
				}
			}
		}
		return instance;
	}
	
	public void init(String apkDir, String apkName) {
		outputDir = new File(apkDir + apkName);
		if(!outputDir.exists() || !outputDir.isDirectory()) {
			outputDir.mkdirs();
		}
		decompileFile = new File(outputDir, "decompile");
		if(!decompileFile.exists() || !decompileFile.isDirectory()) {
			decompileFile.mkdirs();
		}
		staticResultFile = new File(outputDir, "static");
		if(!staticResultFile.exists() || !staticResultFile.isDirectory()) {
			staticResultFile.mkdirs();
		}
		pkgFile = new File(staticResultFile, "Package.txt");
		testingPonit = new File(staticResultFile, "TestingPonit.txt");
		if(testingPonit.exists() || testingPonit.isDirectory()) {   //每次初始化时，删除已存在的testingPoint文件
			testingPonit.delete();
		}
		result = new File(staticResultFile, "GUIModel_"+AppParser.v().getPkg()+".dat");
		recyclerViewAdapter = new File(staticResultFile, "RecyclerViewAdapter.txt");
		baseAdapter = new File(staticResultFile, "BaseAdapter.txt");
	}
	
	
	private File outputDir;
	private File decompileFile;
	private File staticResultFile;
	
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
	
	private File pkgFile;
	private File testingPonit;
	private File result;
	private File recyclerViewAdapter, baseAdapter;
	
	
	public void writePkg(String pkg) {
		try {
			writer(pkgFile, pkg, false);
		} catch (IOException e) {
			Logger.e(TAG, e);
		}
	}
	
	private List<String> wroteTestingPoint = new ArrayList<>();
	public void writeTestingPoint(String text) {
		if(wroteTestingPoint.contains(text)) {
			return;
		}
		try {
			testingPonit=new File(staticResultFile, "TestingPonit.txt");
			writer(testingPonit, text, true);  //append - if true, then bytes will be writtento the end of the file rather than the beginning
            wroteTestingPoint.add(text); 
		} catch (IOException e) {
			Logger.e(TAG, e);
		}
	}
	
	public void writeResult(Graph graph) {
		try(OutputStream os = new FileOutputStream(result);
			ObjectOutputStream oos = new ObjectOutputStream(os)){
			oos.writeObject(graph);
			Logger.i(TAG, "write graph successful: "+result.getAbsolutePath());
		} catch (FileNotFoundException e) {
			Logger.e(TAG, new RuntimeException("write graph fail"));
			Logger.e(TAG, e);
		} catch (IOException e) {
			Logger.e(TAG, new RuntimeException("write graph fail"));
			Logger.e(TAG, e);
		}
	}
	
	public void writeResultCsv(Graph graph) {
		File nodesFile = new File(staticResultFile, "Nodes.csv");   //节点CSV
		try(FileOutputStream out = new FileOutputStream(nodesFile);
			OutputStreamWriter osw = new OutputStreamWriter(out);
			BufferedWriter bw = new BufferedWriter(osw)){
			
			bw.write("id,name,test,type,fragmentsNameSize,contextMenuID,optionsMenuID,leftDrawerID,rightDrawerID,Widgets\n");   //一次一行
			for(Node n : graph.getNodes()) {
				if(n.getId()==0)
					{continue;}
				bw.write(n.toCsv());
			}
		} catch (FileNotFoundException e) {
			Logger.e(TAG, new RuntimeException("write nodes csv fail"));
		} catch (IOException e) {
			Logger.e(TAG, new RuntimeException("write nodes csv fail"));
		}
		File edgesFile = new File(staticResultFile, "Edges.csv");  //边CSV
		try(FileOutputStream out = new FileOutputStream(edgesFile);
			OutputStreamWriter osw = new OutputStreamWriter(out);
			BufferedWriter bw = new BufferedWriter(osw)){
			
			bw.write("id,label,srcID,tgtID,widget\n");
			for(Edge e : graph.getEdges()) {
				bw.write(e.toCsv());
			}
		} catch (FileNotFoundException e) {
			Logger.e(TAG, new RuntimeException("write edges csv fail"));
		} catch (IOException e) {
			Logger.e(TAG, new RuntimeException("write edges csv fail"));
		}
		//写widgets
		File widgetsFile =new File(staticResultFile, "widgets.csv");
		try (FileOutputStream out = new FileOutputStream(widgetsFile);
				OutputStreamWriter osw = new OutputStreamWriter(out);
				BufferedWriter bw = new BufferedWriter(osw)) {
			//// id,type,resName,resID,text,eventMethod,eventHandler,eventType,test,dependSizes
			bw.write("id,type,resName,resID,text,eventMethod,eventHandler,eventType,test,dependSizes,isStatic\n");  
			for (Widget w : graph.getWidgets()) {   //往图里面再加入一个控件集合，搜集所有控件集合
				bw.write(w.newtoCSV());
			}
		} catch (FileNotFoundException e) {
			Logger.e(TAG, new RuntimeException("write widgets csv fail"));
		} catch (IOException e) {
			Logger.e(TAG, new RuntimeException("write widgets csv fail"));
		}
	}
	
	public void writeResultString(String str) {
		File file = new File(staticResultFile, "gSTG.txt");
		try {
			writer(file, str, false);
		} catch (IOException e) {
			Logger.e(TAG, e);
		}
	}
	
	public void writeResources(Map<String, String> wId_wName, Map<String, String> lId_lName,
			Map<String, String> xId_xName, Map<String, String> mId_mName, Map<String, String> nId_nName) {
		File resFile = new File(staticResultFile, "Resources.csv");
		try(FileOutputStream out = new FileOutputStream(resFile);
			OutputStreamWriter osw = new OutputStreamWriter(out);
			BufferedWriter bw = new BufferedWriter(osw)){
			
			bw.write("Id,Name\n");
			bw.write("1,Widget\n");
			writeMap(wId_wName, bw);
			bw.write("2,LayoutFileName\n");
			writeMap(lId_lName, bw);
			bw.write("3,XmlFileName\n");
			writeMap(xId_xName, bw);
			bw.write("4,MenuFileName\n");
			writeMap(mId_mName, bw);
			bw.write("5,NavigationFileName\n");
			writeMap(nId_nName, bw);
		} catch (FileNotFoundException e) {
			Logger.e(TAG, new RuntimeException("write resources csv fail"));
		} catch (IOException e) {
			Logger.e(TAG, new RuntimeException("write resources csv fail"));
		}
	}
	
	private void writeMap(Map<String, String> map, BufferedWriter bw) throws IOException {
		for(String s : map.keySet()) {
			bw.write(s + "," + map.get(s) + "\n");
		}
	}
	
	public void writeString(Map<String, String> sId_sName, Map<String, String> name_text) {
		File strings = new File(staticResultFile, "strings.csv");
		try(FileOutputStream out = new FileOutputStream(strings);
			OutputStreamWriter osw = new OutputStreamWriter(out);
			BufferedWriter bw = new BufferedWriter(osw)){
			bw.write("size,"+sId_sName.size() + "," + name_text.size() + "\n");
			bw.write("Id,Name,Value\n");
			for(String id : sId_sName.keySet()) {
				String name = sId_sName.get(id);
				String value = name_text.get(name);
				bw.write(id + "," + name + "," + (value == null ? "" : value) + "\n");
			}
			
		} catch (FileNotFoundException e) {
			Logger.e(TAG, new RuntimeException("write graph str fail"));
		} catch (IOException e) {
			Logger.e(TAG, new RuntimeException("write graph str fail"));
		}
	}
	
	public void writeAdapterClasses(Set<String> recyclerAdapters, Set<String> baseAdapters) {
		writeSet(baseAdapters, baseAdapter);
		writeSet(recyclerAdapters, recyclerViewAdapter);
	}
	
	private void writeSet(Set<String> set, File file) {
		StringBuilder sb = new StringBuilder();
		for(String s : set) {
			sb.append(s);
			sb.append("\n");
		}
		try {
			writer(file, sb.toString(), false);
		} catch (IOException e) {
			Logger.e(TAG, e);
		}
	}
	
	
	
	
	
	//push result to device
	public void pushResult() {
		String adbpush = "adb push ", targetFile = " /storage/emulated/0/00_IDBDroid";
		Process p = null;
		try {
			push(p, adbpush + result.getAbsolutePath() + targetFile);
			push(p, adbpush + pkgFile.getAbsolutePath() + targetFile);
			push(p, adbpush + testingPonit.getAbsolutePath() + targetFile);
			push(p, adbpush + recyclerViewAdapter.getAbsolutePath() + targetFile);
			push(p, adbpush + baseAdapter.getAbsolutePath() + targetFile);
		}catch (Exception e) {
			Logger.e(TAG, e);
		}
	}
	
	private void push(Process p, String cmd) throws Exception {
		p = Runtime.getRuntime().exec(cmd);
		InputStream is = p.getInputStream();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try(InputStreamReader isr = new InputStreamReader(is);
					BufferedReader br = new BufferedReader(isr)){
					String line = null;
					while ((line = br.readLine()) != null) {
						Logger.i(TAG, line);
					}
				} catch (IOException e){
					Logger.e(TAG, e);
				}
			}
		}).start();
	}
}
