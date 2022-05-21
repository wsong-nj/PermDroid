package zzg.staticanalysis.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import soot.SootClass;

public class ExcludePackage {
	private static final String TAG = "[Exclude]";
	
	private static ExcludePackage instance;
	public static ExcludePackage v() {
		if(instance == null) {
			synchronized (ExcludePackage.class) {
				if(instance == null) {
					instance = new ExcludePackage();
				}
			}
		}
		return instance;
	}
	private ExcludePackage() {
		File excludefile = new File("./ExcludePackage.txt");
		if(excludefile.exists() && excludefile.isFile()) {
			try {
				InputStreamReader is = new InputStreamReader(new FileInputStream(excludefile));
				BufferedReader reader = new BufferedReader(is);
				String line = null;
				while((line = reader.readLine()) != null) {
					if(!line.equals("")) {
						excludes.add(line);
					}
				}
				reader.close();
			} catch (FileNotFoundException e) {
				Logger.e(TAG, e);
			} catch (IOException e) {
				Logger.e(TAG, e);
			}
		}
	}
	public Set<String> excludes = new HashSet<String>();
	

	public boolean isExclude(SootClass sc) {
		for(String s : excludes) {
			if(sc.getName().startsWith(s))
				return true;
		}
		return false;
	}
}
