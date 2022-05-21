package zzg.staticanalysis.utils;

import zzg.staticanalysis.Main;
import java.util.HashMap;
import java.util.Map;
/**
 * public static String androidPlatformLocation = "F:\\Elipse\\android-sdk-windows\\platforms";
	public static String apkDir = "G:\\APK\\socialApps\\";
	public static String apkName = "goodweather_13";
 * */

public class MappingConstants {
	public static String outFolder = ".\\sootOutput\\";
	public static final String AndroidPlatforms = Main.androidPlatformLocation;
	public static final String apkFolder = Main.apkDir;
	public static String apkName = Main.apkName;
	public static String Manifestinfo = "Manifestinfo";
	public static String apk = apkFolder+apkName+".apk";
	public static String[] reqOrCheckPermission= {
			"checkSelfPermission",
			"requestPermissions"
	};
	
	public MappingConstants() {
		
	}
	/**
	 * API-premission Mapping;
	 *   first��8 dangerous  permission-groups ！！mapping！！> 16 dangerous permissions ,but group SENSOR hasn't API mapping ,so it is ignored;
	 *   second ��16 dangerous permissions   ！！mapping！！> API merthodSignatures, some dangerous permissions hasn't API mappings,so they are ignored.
	 */ 
	public static final String[] needPermissions = {"RECORD_AUDIO", "READ_EXTERNAL_STORAGE","WRITE_EXTERNAL_STORAGE",
			"CAMERA","READ_CALENDAR", "WRITE_CALENDAR","WRITE_CONTACTS", "READ_CONTACTS", "GET_ACCOUNTS",
			"READ_CALL_LOG", "READ_PHONE_STATE",  "USE_SIP",  "ADD_VOICEMAIL","ACCESS_FINE_LOCATION", 
			"ACCESS_COARSE_LOCATION","SEND_SMS"  };
	//map 1
	public static Map<String,String[]> groupsToPermissions = new HashMap<String,String[]>();
	// dangerous permissions
	public static final String[] MicroPhone = {	"RECORD_AUDIO" };
	public static final String[] Storage = {"READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"};
	public static final String[] Camera = {	"CAMERA"};
	public static final String[] Calendar = {"READ_CALENDAR", "WRITE_CALENDAR" };
	public static final String[] Contacts = {"WRITE_CONTACTS", "READ_CONTACTS", "GET_ACCOUNTS" };
	public static final String[] Phone = {"READ_CALL_LOG", "READ_PHONE_STATE",  "USE_SIP",  "ADD_VOICEMAIL" };
	public static final String[] Location = {"ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION" };
	public static final String[] Sms = {"SEND_SMS"};
	//map 2
	public static Map<String,String[]>  permissionToMethodSignatures =new HashMap<String,String[]>();
	//methodSignatures     
	// 1-6
	public static final String[] RECORD_AUDIO = {   
			"<android.speech.SpeechRecognizer: void stopListening()>", 
			"<android.speech.SpeechRecognizer: void setRecognitionListener(android.speech.RecognitionListener)>",
			"<android.speech.SpeechRecognizer: void cancel()>",
			"<android.media.AudioRecord: void <init>(int,int,int,int,int)>",
			"<android.speech.SpeechRecognizer: void startListening(android.content.Intent)>",
			"<android.media.MediaRecorder: void setAudioSource(int)>"
	};
	//2-7
	public static final String[]  READ_EXTERNAL_STORAGE= {
			"<android.view.inputmethod.InputMethodManager: void showInputMethodAndSubtypeEnabler(java.lang.String)>",
			"<android.telephony.gsm.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
			"<android.telephony.gsm.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.gsm.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
			"<android.telephony.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>"
	};
	//2-3
	public static final String[] WRITE_EXTERNAL_STORAGE= {
			"<android.app.DownloadManager: android.net.Uri getUriForDownloadedFile(long)>",
			"<android.app.DownloadManager: long enqueue(android.app.DownloadManager$Request)>",
			"<android.app.DownloadManager: long addCompletedDownload(java.lang.String,java.lang.String,boolean,java.lang.String,java.lang.String,long,boolean)>"
	};
	//3-3
	public static final String[] CAMERA= {
			"<android.media.MediaRecorder: void setVideoSource(int)>",
			"<android.hardware.Camera: android.hardware.Camera open()>",
			"<android.hardware.Camera: void native_setup(java.lang.Object)>"
	};
	//4-6
	public static final String[] READ_CALENDAR= {
			"<android.provider.CalendarContract$Instances: android.database.Cursor query(android.content.ContentResolver,java.lang.String[],long,long)>",
			"<android.provider.CalendarContract$CalendarAlerts: android.net.Uri insert(android.content.ContentResolver,long,long,long,long,int)>",
			"<android.provider.CalendarContract$EventDays: android.database.Cursor query(android.content.ContentResolver,int,int,java.lang.String[])>",
			"<android.provider.CalendarContract$Instances: android.database.Cursor query(android.content.ContentResolver,java.lang.String[],long,long,java.lang.String)>",
			"<android.provider.CalendarContract$Attendees: android.database.Cursor query(android.content.ContentResolver,long,java.lang.String[])>",
			"<android.provider.CalendarContract$Reminders: android.database.Cursor query(android.content.ContentResolver,long,java.lang.String[])>"
	};
	//4-2
	public static final String[] WRITE_CALENDAR= {
			"<com.android.calendar.AgendaWindowAdapter: android.net.Uri buildQueryUri(int,int)>",
			"<android.provider.CalendarContract$CalendarAlerts: android.net.Uri insert(android.content.ContentResolver,long,long,long,long,int)>"
	};
	//5-29
	public static final String[]  WRITE_CONTACTS= {
			"<com.android.contacts.ContactsListActivity: android.net.Uri getContactUri(int)>",
			"<com.android.contacts.ContactsListActivity: android.net.Uri getUriToQuery()>",
			"<com.android.contacts.ContactsListActivity: android.net.Uri getSelectedUri(int)>",
			"<android.provider.Contacts$People: void markAsContacted(android.content.ContentResolver,long)>",
			"<android.provider.Contacts$ContactMethods: void addPostalLocation(android.content.Context,long,double,double)>",
			"<android.widget.QuickContactBadge: void assignContactFromPhone(java.lang.String,boolean)>",
			"<android.provider.ContactsContract$ProfileSyncState: byte[] get(android.content.ContentProviderClient,android.accounts.Account)>",
			"<android.provider.Contacts$People: android.database.Cursor queryGroups(android.content.ContentResolver,long)>",
			"<android.provider.ContactsContract$ProfileSyncState: android.content.ContentProviderOperation newSetOperation(android.accounts.Account,byte[])>",
			"<android.provider.ContactsContract$SyncState: void set(android.content.ContentProviderClient,android.accounts.Account,byte[])>",
			"<android.provider.ContactsContract$RawContacts: android.net.Uri getContactLookupUri(android.content.ContentResolver,android.net.Uri)>",
			"<android.provider.Contacts$People: android.net.Uri createPersonInMyContactsGroup(android.content.ContentResolver,android.content.ContentValues)>",
			"<android.provider.Contacts$People: android.net.Uri addToGroup(android.content.ContentResolver,long,java.lang.String)>",
			"<android.provider.Contacts$People: android.net.Uri addToMyContactsGroup(android.content.ContentResolver,long)>",
			"<android.provider.ContactsContract$SyncState: android.util.Pair getWithUri(android.content.ContentProviderClient,android.accounts.Account)>",
			"<android.provider.Contacts$Settings: void setSetting(android.content.ContentResolver,java.lang.String,java.lang.String,java.lang.String)>",
			"<android.provider.ContactsContract$SyncState: android.content.ContentProviderOperation newSetOperation(android.accounts.Account,byte[])>",
			"<android.provider.ContactsContract$Contacts: android.net.Uri getLookupUri(android.content.ContentResolver,android.net.Uri)>",
			"<android.provider.ContactsContract$Contacts: void markAsContacted(android.content.ContentResolver,long)>",
			"<android.provider.ContactsContract$ProfileSyncState: android.util.Pair getWithUri(android.content.ContentProviderClient,android.accounts.Account)>",
			"<android.widget.QuickContactBadge: void onClick(android.view.View)>",
			"<android.provider.ContactsContract$SyncState: byte[] get(android.content.ContentProviderClient,android.accounts.Account)>",
			"<android.widget.QuickContactBadge: void assignContactFromEmail(java.lang.String,boolean)>",
			"<android.provider.ContactsContract$Contacts: android.net.Uri getLookupUri(long,java.lang.String)>",
			"<android.provider.ContactsContract$ProfileSyncState: void set(android.content.ContentProviderClient,android.accounts.Account,byte[])>",
			"<android.provider.Contacts$Settings: java.lang.String getSetting(android.content.ContentResolver,java.lang.String,java.lang.String)>",
			"<android.provider.Contacts$People: android.net.Uri addToGroup(android.content.ContentResolver,long,long)>",
			"<android.provider.ContactsContract$Directory: void notifyDirectoryChange(android.content.ContentResolver)>",
			"<android.provider.ContactsContract$Data: android.net.Uri getContactLookupUri(android.content.ContentResolver,android.net.Uri)>"
	};
	//5-24
	public static final String[] GET_ACCOUNTS= {
			"<android.provider.Browser: void deleteFromHistory(android.content.ContentResolver,java.lang.String)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture confirmCredentials(android.accounts.Account,android.os.Bundle,android.app.Activity,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture editProperties(java.lang.String,android.app.Activity,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture getAuthToken(android.accounts.Account,java.lang.String,android.os.Bundle,boolean,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.provider.Browser: void updateVisitedHistory(android.content.ContentResolver,java.lang.String,boolean)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture getAuthToken(android.accounts.Account,java.lang.String,boolean,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture getAuthTokenByFeatures(java.lang.String,java.lang.String,java.lang.String[],android.app.Activity,android.os.Bundle,android.os.Bundle,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture addAccount(java.lang.String,java.lang.String,java.lang.String[],android.os.Bundle,android.app.Activity,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.provider.Browser: void truncateHistory(android.content.ContentResolver)>",
			"<android.provider.Browser: void clearSearches(android.content.ContentResolver)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture getAccountsByTypeAndFeatures(java.lang.String,java.lang.String[],android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: void addOnAccountsUpdatedListener(android.accounts.OnAccountsUpdateListener,android.os.Handler,boolean)>",
			"<android.provider.Browser: void deleteHistoryTimeFrame(android.content.ContentResolver,long,long)>",
			"<android.app.KeyguardManager: void exitKeyguardSecurely(android.app.KeyguardManager$OnKeyguardExitResult)>",
			"<android.provider.Browser: void addSearchUrl(android.content.ContentResolver,java.lang.String)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture removeAccount(android.accounts.Account,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture getAuthToken(android.accounts.Account,java.lang.String,android.os.Bundle,android.app.Activity,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: android.accounts.Account[] getAccountsByType(java.lang.String)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture getAuthTokenLabel(java.lang.String,java.lang.String,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture updateCredentials(android.accounts.Account,java.lang.String,android.os.Bundle,android.app.Activity,android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.provider.Browser: void clearHistory(android.content.ContentResolver)>",
			"<android.accounts.AccountManager: android.accounts.AccountManagerFuture hasFeatures(android.accounts.Account,java.lang.String[],android.accounts.AccountManagerCallback,android.os.Handler)>",
			"<android.accounts.AccountManager: android.accounts.Account[] getAccounts()>",
			"<android.accounts.AccountManager: java.lang.String blockingGetAuthToken(android.accounts.Account,java.lang.String,boolean)>"
		
	};
	//5-26
	public static final String[] READ_CONTACTS= {
			"<android.provider.Contacts$People: void markAsContacted(android.content.ContentResolver,long)>",
			"<android.provider.Contacts$ContactMethods: void addPostalLocation(android.content.Context,long,double,double)>",
			"<android.widget.QuickContactBadge: void assignContactFromPhone(java.lang.String,boolean)>",
			"<android.provider.ContactsContract$ProfileSyncState: byte[] get(android.content.ContentProviderClient,android.accounts.Account)>",
			"<android.provider.ContactsContract$ProfileSyncState: android.content.ContentProviderOperation newSetOperation(android.accounts.Account,byte[])>",
			"<android.provider.Contacts$People: android.database.Cursor queryGroups(android.content.ContentResolver,long)>",
			"<android.provider.ContactsContract$SyncState: void set(android.content.ContentProviderClient,android.accounts.Account,byte[])>",
			"<android.provider.ContactsContract$RawContacts: android.net.Uri getContactLookupUri(android.content.ContentResolver,android.net.Uri)>",
			"<android.provider.Contacts$People: android.net.Uri createPersonInMyContactsGroup(android.content.ContentResolver,android.content.ContentValues)>",
			"<android.provider.Contacts$People: android.net.Uri addToGroup(android.content.ContentResolver,long,java.lang.String)>",
			"<android.provider.Contacts$People: android.net.Uri addToMyContactsGroup(android.content.ContentResolver,long)>",
			"<android.provider.Contacts$Settings: void setSetting(android.content.ContentResolver,java.lang.String,java.lang.String,java.lang.String)>",
			"<android.provider.ContactsContract$SyncState: android.content.ContentProviderOperation newSetOperation(android.accounts.Account,byte[])>",
			"<android.provider.ContactsContract$SyncState: android.util.Pair getWithUri(android.content.ContentProviderClient,android.accounts.Account)>",
			"<android.provider.ContactsContract$Contacts: android.net.Uri getLookupUri(android.content.ContentResolver,android.net.Uri)>",
			"<android.provider.ContactsContract$Contacts: void markAsContacted(android.content.ContentResolver,long)>",
			"<android.provider.ContactsContract$ProfileSyncState: android.util.Pair getWithUri(android.content.ContentProviderClient,android.accounts.Account)>",
			"<android.widget.QuickContactBadge: void onClick(android.view.View)>",
			"<android.provider.ContactsContract$SyncState: byte[] get(android.content.ContentProviderClient,android.accounts.Account)>",
			"<android.widget.QuickContactBadge: void assignContactFromEmail(java.lang.String,boolean)>",
			"<android.provider.ContactsContract$Contacts: android.net.Uri getLookupUri(long,java.lang.String)>",
			"<android.provider.ContactsContract$ProfileSyncState: void set(android.content.ContentProviderClient,android.accounts.Account,byte[])>",
			"<android.provider.ContactsContract$Directory: void notifyDirectoryChange(android.content.ContentResolver)>",
			"<android.provider.Contacts$People: android.net.Uri addToGroup(android.content.ContentResolver,long,long)>",
			"<android.provider.Contacts$Settings: java.lang.String getSetting(android.content.ContentResolver,java.lang.String,java.lang.String)>",
			"<android.provider.ContactsContract$Data: android.net.Uri getContactLookupUri(android.content.ContentResolver,android.net.Uri)>"

	};
	//6-1
	public static final String[] READ_CALL_LOG= {
			"<android.provider.CallLog$Calls: java.lang.String getLastOutgoingCall(android.content.Context)>"
	};
	//6-9
	public static final String[] READ_PHONE_STATE= {
			"<android.telephony.TelephonyManager: java.lang.String getSubscriberId()>",
			"<android.telephony.TelephonyManager: java.lang.String getDeviceSoftwareVersion()>",
			"<android.telephony.TelephonyManager: void listen(android.telephony.PhoneStateListener,int)>",
			"<android.telephony.TelephonyManager: java.lang.String getLine1Number()>",
			"<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()>",
			"<android.net.ConnectivityManager: int startUsingNetworkFeature(int,java.lang.String)>",
			"<android.telephony.TelephonyManager: java.lang.String getVoiceMailAlphaTag()>",
			"<android.telephony.TelephonyManager: java.lang.String getVoiceMailNumber()>",
			"<android.telephony.TelephonyManager: java.lang.String getDeviceId()>"
	};
	//6-13
	public static final String[] USE_SIP= {
			"<android.net.sip.SipManager: android.net.sip.SipSession getSessionFor(android.content.Intent)>",
			"<android.net.sip.SipManager: android.net.sip.SipAudioCall takeAudioCall(android.content.Intent,android.net.sip.SipAudioCall$Listener)>",
			"<android.net.sip.SipManager: void open(android.net.sip.SipProfile,android.app.PendingIntent,android.net.sip.SipRegistrationListener)>",
			"<android.net.sip.SipManager: android.net.sip.SipAudioCall makeAudioCall(android.net.sip.SipProfile,android.net.sip.SipProfile,android.net.sip.SipAudioCall$Listener,int)>",
			"<android.net.sip.SipManager: void register(android.net.sip.SipProfile,int,android.net.sip.SipRegistrationListener)>",
			"<android.net.sip.SipManager: boolean isOpened(java.lang.String)>",
			"<android.net.sip.SipManager: boolean isRegistered(java.lang.String)>",
			"<android.net.sip.SipManager: void open(android.net.sip.SipProfile)>",
			"<android.net.sip.SipManager: void unregister(android.net.sip.SipProfile,android.net.sip.SipRegistrationListener)>",
			"<android.net.sip.SipManager: android.net.sip.SipAudioCall makeAudioCall(java.lang.String,java.lang.String,android.net.sip.SipAudioCall$Listener,int)>",
			"<android.net.sip.SipManager: void close(java.lang.String)>",
			"<android.net.sip.SipManager: void setRegistrationListener(java.lang.String,android.net.sip.SipRegistrationListener)>",
			"<android.net.sip.SipManager: android.net.sip.SipSession createSipSession(android.net.sip.SipProfile,android.net.sip.SipSession$Listener)>"
	};
	//6-3
	public static final String[] ADD_VOICEMAIL= {
			"<android.provider.CallLog$Calls: java.lang.String getLastOutgoingCall(android.content.Context)>",
			"<android.provider.VoicemailContract$Status: android.net.Uri buildSourceUri(java.lang.String)>",
			"<android.provider.VoicemailContract$Voicemails: android.net.Uri buildSourceUri(java.lang.String)>"
	};
	//7-22
	public static final String[] ACCESS_FINE_LOCATION= {
			"<android.telephony.TelephonyManager: java.util.List getAllCellInfo()>",
			"<android.location.LocationManager: void requestLocationUpdates(long,float,android.location.Criteria,android.location.LocationListener,android.os.Looper)>",
			"<android.location.LocationManager: java.util.List getProviders(android.location.Criteria,boolean)>",
			"<android.location.LocationManager: void requestSingleUpdate(android.location.Criteria,android.app.PendingIntent)>",
			"<android.location.LocationManager: android.location.LocationProvider getProvider(java.lang.String)>",
			"<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>",
			"<android.location.LocationManager: boolean isProviderEnabled(java.lang.String)>",
			"<android.location.LocationManager: void addProximityAlert(double,double,float,long,android.app.PendingIntent)>",
			"<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)>",
			"<android.location.LocationManager: java.lang.String getBestProvider(android.location.Criteria,boolean)>",
			"<android.telephony.TelephonyManager: java.util.List getNeighboringCellInfo()>",
			"<android.telephony.TelephonyManager: android.telephony.CellLocation getCellLocation()>",
			"<android.location.LocationManager: java.util.List getProviders(boolean)>",
			"<android.location.LocationManager: void requestLocationUpdates(long,float,android.location.Criteria,android.app.PendingIntent)>",
			"<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)>",
			"<android.location.LocationManager: boolean sendExtraCommand(java.lang.String,java.lang.String,android.os.Bundle)>",
			"<android.location.LocationManager: boolean addNmeaListener(android.location.GpsStatus$NmeaListener)>",
			"<android.location.LocationManager: void requestSingleUpdate(java.lang.String,android.location.LocationListener,android.os.Looper)>",
			"<android.location.LocationManager: void requestSingleUpdate(android.location.Criteria,android.location.LocationListener,android.os.Looper)>",
			"<android.location.LocationManager: boolean addGpsStatusListener(android.location.GpsStatus$Listener)>",
			"<android.location.LocationManager: void requestSingleUpdate(java.lang.String,android.app.PendingIntent)>",
			"<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)>"

	};
	//7-26
	public static final String[] ACCESS_COARSE_LOCATION= {
			"<android.location.LocationManager: boolean addNmeaListener(android.location.OnNmeaMessageListener)>",
			"<android.location.LocationManager: boolean addNmeaListener(android.location.OnNmeaMessageListener,android.os.Handler)>",
			"<android.location.LocationManager: boolean registerGnssStatusCallback(android.location.GnssStatus$Callback)>",
			"<android.location.LocationManager: boolean registerGnssStatusCallback(android.location.GnssStatus$Callback,android.os.Handler)>",
			"<android.location.LocationManager: void removeUpdates(android.app.PendingIntent)>",
			"<android.location.LocationManager: void removeUpdates(android.location.LocationListener)>",
			"<android.location.LocationManager: void requestLocationUpdates(long,float,android.location.Criteria,android.location.LocationListener,android.os.Looper)>",
			"<android.location.LocationManager: java.util.List getProviders(android.location.Criteria,boolean)>",
			"<android.location.LocationManager: void requestSingleUpdate(android.location.Criteria,android.app.PendingIntent)>",
			"<android.location.LocationManager: android.location.LocationProvider getProvider(java.lang.String)>",
			"<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>",
			"<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>",
			"<android.location.LocationManager: boolean isProviderEnabled(java.lang.String)>",
			"<android.location.LocationManager: void addProximityAlert(double,double,float,long,android.app.PendingIntent)>",
			"<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)>",
			"<android.location.LocationManager: java.lang.String getBestProvider(android.location.Criteria,boolean)>",
			"<android.telephony.TelephonyManager: java.util.List getNeighboringCellInfo()>",
			"<android.telephony.TelephonyManager: android.telephony.CellLocation getCellLocation()>",
			"<android.telephony.TelephonyManager: void listen(android.telephony.PhoneStateListener,int)>",
			"<android.location.LocationManager: java.util.List getProviders(boolean)>",
			"<android.location.LocationManager: void requestLocationUpdates(long,float,android.location.Criteria,android.app.PendingIntent)>",
			"<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)>",
			"<android.location.LocationManager: boolean sendExtraCommand(java.lang.String,java.lang.String,android.os.Bundle)>",
			"<android.location.LocationManager: void requestSingleUpdate(java.lang.String,android.location.LocationListener,android.os.Looper)>",
			"<android.location.LocationManager: void requestSingleUpdate(android.location.Criteria,android.location.LocationListener,android.os.Looper)>",
			"<android.location.LocationManager: void requestSingleUpdate(java.lang.String,android.app.PendingIntent)>",
			"<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)>"
	};
	//8-7
	public static final String[] SEND_SMS= {
			"<android.telephony.SmsManager: void sendMultimediaMessage(android.content.Context,android.net.Uri,java.lang.String,android.os.Bundle,android.app.PendingIntent)>",
			"<android.telephony.gsm.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
			"<android.telephony.gsm.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
			"<android.telephony.gsm.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
			"<android.telephony.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>"
	};
	//initialization  map 1   
	public static void initMap1() {
		groupsToPermissions.put("MICROPHONE", MicroPhone);
		groupsToPermissions.put("STORAGE", Storage);
		groupsToPermissions.put("CAMERA", Camera);
		groupsToPermissions.put("CALENDAR", Calendar);
		groupsToPermissions.put("CONTACTS", Contacts);
		groupsToPermissions.put("PHONE", Phone);
		groupsToPermissions.put("LOCATION", Location);
		groupsToPermissions.put("SMS", Sms);
		
	}
	//initialization  map 2    
	public static void initMap2() {
		permissionToMethodSignatures.put("RECORD_AUDIO", RECORD_AUDIO);
		permissionToMethodSignatures.put("READ_EXTERNAL_STORAGE", READ_EXTERNAL_STORAGE);
		permissionToMethodSignatures.put("WRITE_EXTERNAL_STORAGE", WRITE_EXTERNAL_STORAGE);
		permissionToMethodSignatures.put("CAMERA", CAMERA);
		permissionToMethodSignatures.put("READ_CALENDAR", READ_CALENDAR);
		permissionToMethodSignatures.put("WRITE_CALENDAR", WRITE_CALENDAR);
		permissionToMethodSignatures.put("WRITE_CONTACTS", WRITE_CONTACTS);
		permissionToMethodSignatures.put("GET_ACCOUNTS", GET_ACCOUNTS);
		permissionToMethodSignatures.put("READ_CONTACTS", READ_CONTACTS);
		permissionToMethodSignatures.put("READ_CALL_LOG", READ_CALL_LOG);
		permissionToMethodSignatures.put("READ_PHONE_STATE", READ_PHONE_STATE);
		permissionToMethodSignatures.put("USE_SIP", USE_SIP);
		permissionToMethodSignatures.put("ADD_VOICEMAIL", ADD_VOICEMAIL);
		permissionToMethodSignatures.put("ACCESS_FINE_LOCATION", ACCESS_FINE_LOCATION);
		permissionToMethodSignatures.put("ACCESS_COARSE_LOCATION", ACCESS_COARSE_LOCATION);
		permissionToMethodSignatures.put("SEND_SMS", SEND_SMS);	
	};
	
}