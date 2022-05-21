package zzg.staticanalysis.intent;

import java.util.ArrayList;
import java.util.Set;

public class Activity {
	public final String mClassName;
	public final ArrayList<IntentFilter> mIntents;
	
	public Activity(String actName) {
		this.mClassName = actName;
		this.mIntents = new ArrayList<IntentFilter>();
	}
	
	public Activity(String actName, ArrayList<IntentFilter> intents) {
		this.mClassName = actName;
		this.mIntents = intents;
	}
	
	public void addIntent(IntentFilter intent) {
		mIntents.add(intent);
	}
	
	public boolean match(String action, String type, Set<String> categories) {
		for(IntentFilter intent : mIntents) {
			if(intent.match(action, type, categories)) {
				return true;
			}
		}
		return false;
	}
}
