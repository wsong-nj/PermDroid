package zzg.staticanalysis.utils;

public class Logger {

	public static void i(String TAG, String msg) {
		System.out.println(TAG + "\t" + msg);
	}
	
	public static void e(String TAG, Exception e) {
		System.err.println(TAG + " " + e);
		System.err.println(TAG + "\t\tat " + e.getStackTrace()[0].toString());
		if(e.getStackTrace().length > 1)
			System.err.println(TAG + "\t\tat " + e.getStackTrace()[1].toString());
		Throwable curEx = e;
		while(curEx.getCause() != null) {
			curEx = curEx.getCause();
			System.err.println(TAG + " Caused by: " + curEx);
			System.err.println(TAG + "\t\tat " + curEx.getStackTrace()[0].toString());
			if(curEx.getStackTrace().length > 1)
				System.err.println(TAG + "\t\tat " + curEx.getStackTrace()[1].toString());
		}
	}
}
