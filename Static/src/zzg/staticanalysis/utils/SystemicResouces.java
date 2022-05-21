package zzg.staticanalysis.utils;

import java.util.HashMap;
import java.util.Map;

public class SystemicResouces {
	private static final Map<Integer, String> system_string_id_name = new HashMap<Integer, String>();
	private static final Map<String, String> system_string_name_text = new HashMap<String, String>();
	public static String getStringNameById(int id) {
		String name = system_string_id_name.get(id);
		if(name != null) {
			return system_string_name_text.get(name);
		}
		return null;
	}
	public static String getStringByStringName(String name) {
		return system_string_name_text.get(name);
	}
	static {
		system_string_id_name.put(17039360, "cancel");
		system_string_name_text.put("cancel", "Cancel");
		system_string_id_name.put(17039361, "copy");
		system_string_name_text.put("copy", "Copy");
		system_string_id_name.put(17039362, "copyUrl");
		system_string_name_text.put("copyUrl", "Copy URL");
		system_string_id_name.put(17039363, "cut");
		system_string_name_text.put("cut", "Cut");
		system_string_id_name.put(17039364, "defaultVoiceMailAlphaTag");
		system_string_name_text.put("defaultVoiceMailAlphaTag", "Voicemail");
		system_string_id_name.put(17039365, "defaultMsisdnAlphaTag");
		system_string_name_text.put("defaultMsisdnAlphaTag", "MSISDN1");
		system_string_id_name.put(17039366, "emptyPhoneNumber");
		system_string_name_text.put("emptyPhoneNumber", "(No phone number)");
		system_string_id_name.put(17039367, "httpErrorBadUrl");
		system_string_name_text.put("httpErrorBadUrl", "Couldn't open the page because the URL is invalid.");
		system_string_id_name.put(17039368, "httpErrorUnsupportedScheme");
		system_string_name_text.put("httpErrorUnsupportedScheme", "The protocol isn't supported.");
		system_string_id_name.put(17039369, "no");
		system_string_name_text.put("no", "Cancel");
		system_string_id_name.put(17039370, "ok");
		system_string_name_text.put("ok", "OK");
		system_string_id_name.put(17039371, "paste");
		system_string_name_text.put("paste", "Paste");
		system_string_id_name.put(17039372, "search_go");
		system_string_name_text.put("search_go", "Search");
		system_string_id_name.put(17039373, "selectAll");
		system_string_name_text.put("selectAll", "Select all");
		system_string_id_name.put(17039374, "unknownName");
		system_string_name_text.put("unknownName", "Unknown");
		system_string_id_name.put(17039375, "untitled");
		system_string_name_text.put("untitled", "<Untitled>");
		system_string_id_name.put(17039376, "VideoView_error_button");
		system_string_name_text.put("VideoView_error_button", "OK");
		system_string_id_name.put(17039377, "VideoView_error_text_unknown");
		system_string_name_text.put("VideoView_error_text_unknown", "Can't play this video.");
		system_string_id_name.put(17039378, "VideoView_error_title");
		system_string_name_text.put("VideoView_error_title", "Video problem");
		system_string_id_name.put(17039379, "yes");
		system_string_name_text.put("yes", "OK");
		system_string_id_name.put(17039380, "dialog_alert_title");
		system_string_name_text.put("dialog_alert_title", "Attention");
		system_string_id_name.put(17039381, "VideoView_error_text_invalid_progressive_playback");
		system_string_name_text.put("VideoView_error_text_invalid_progressive_playback", "This video isn't valid for streaming to this device.");
		system_string_id_name.put(17039382, "selectTextMode");
		system_string_name_text.put("selectTextMode", "Select text");
		system_string_id_name.put(17039383, "status_bar_notification_info_overflow");
		system_string_name_text.put("status_bar_notification_info_overflow", "999+");
		system_string_id_name.put(17039384, "fingerprint_icon_content_description");
		system_string_name_text.put("fingerprint_icon_content_description", "Fingerprint icon");
	}
	
	private static final Map<Integer, String> system_widget_id_name = new HashMap<Integer, String>();
	public static String getWidgetNameById(int id) {
		return system_widget_id_name.get(id);
	}
	static {
		system_widget_id_name.put(16908341, "shareText");
		system_widget_id_name.put(16908328, "startSelectingText");
		system_widget_id_name.put(16908335, "statusBarBackground");
		system_widget_id_name.put(16908329, "stopSelectingText");
		system_widget_id_name.put(16908304, "summary");
		system_widget_id_name.put(16908324, "switchInputMethod");
		system_widget_id_name.put(16908352, "switch_widget");
		system_widget_id_name.put(16908305, "tabcontent");
		system_widget_id_name.put(16908306, "tabhost");
		system_widget_id_name.put(16908307, "tabs");
		system_widget_id_name.put(16908308, "text1");
		system_widget_id_name.put(16908309, "text2");
		system_widget_id_name.put(16908353, "textAssist");
		system_widget_id_name.put(16908310, "title");
		system_widget_id_name.put(16908311, "toggle");
		system_widget_id_name.put(16908338, "undo");
		system_widget_id_name.put(16908312, "widget_frame");
		system_widget_id_name.put(16908318, "inputArea");
		system_widget_id_name.put(16908325, "inputExtractEditText");
		system_widget_id_name.put(16908326, "keyboardView");
		system_widget_id_name.put(16908298, "list");
		system_widget_id_name.put(16908351, "list_container");
		system_widget_id_name.put(16908334, "mask");
		system_widget_id_name.put(16908299, "message");
		system_widget_id_name.put(16908336, "navigationBarBackground");
		system_widget_id_name.put(16908322, "paste");
		system_widget_id_name.put(16908337, "pasteAsPlainText");
		system_widget_id_name.put(16908300, "primary");
		system_widget_id_name.put(16908301, "progress");
		system_widget_id_name.put(16908339, "redo");
		system_widget_id_name.put(16908340, "replaceText");
		system_widget_id_name.put(16908303, "secondaryProgress");
		system_widget_id_name.put(16908319, "selectAll");
		system_widget_id_name.put(16908333, "selectTextMode");
		system_widget_id_name.put(16908302, "selectedIcon");
		system_widget_id_name.put(16908355, "autofill");
		system_widget_id_name.put(16908288, "background");
		system_widget_id_name.put(16908313, "button1");
		system_widget_id_name.put(16908314, "button2");
		system_widget_id_name.put(16908315, "button3");
		system_widget_id_name.put(16908317, "candidatesArea");
		system_widget_id_name.put(16908289, "checkbox");
		system_widget_id_name.put(16908327, "closeButton");
		system_widget_id_name.put(16908290, "content");
		system_widget_id_name.put(16908321, "copy");
		system_widget_id_name.put(16908323, "copyUrl");
		system_widget_id_name.put(16908331, "custom");
		system_widget_id_name.put(16908320, "cut");
		system_widget_id_name.put(16908291, "edit");
		system_widget_id_name.put(16908292, "empty");
		system_widget_id_name.put(16908316, "extractArea");
		system_widget_id_name.put(16908293, "hint");
		system_widget_id_name.put(16908332, "home");
		system_widget_id_name.put(16908294, "icon");
		system_widget_id_name.put(16908295, "icon1");
		system_widget_id_name.put(16908296, "icon2");
		system_widget_id_name.put(16908350, "icon_frame");
		system_widget_id_name.put(16908297, "input");
		system_widget_id_name.put(16908344, "accessibilityActionScrollUp");
		system_widget_id_name.put(16908349, "accessibilityActionSetProgress");
		system_widget_id_name.put(16908342, "accessibilityActionShowOnScreen");
		system_widget_id_name.put(16908356, "accessibilityActionShowTooltip");
		system_widget_id_name.put(16908363, "accessibilitySystemActionBack");
		system_widget_id_name.put(16908364, "accessibilitySystemActionHome");
		system_widget_id_name.put(16908370, "accessibilitySystemActionLockScreen");
		system_widget_id_name.put(16908366, "accessibilitySystemActionNotifications");
		system_widget_id_name.put(16908368, "accessibilitySystemActionPowerDialog");
		system_widget_id_name.put(16908367, "accessibilitySystemActionQuickSettings");
		system_widget_id_name.put(16908365, "accessibilitySystemActionRecents");
		system_widget_id_name.put(16908371, "accessibilitySystemActionTakeScreenshot");
		system_widget_id_name.put(16908369, "accessibilitySystemActionToggleSplitScreen");
		system_widget_id_name.put(16908330, "addToDictionary");
		system_widget_id_name.put(16908348, "accessibilityActionContextClick");
		system_widget_id_name.put(16908357, "accessibilityActionHideTooltip");
		system_widget_id_name.put(16908372, "accessibilityActionImeEnter");
		system_widget_id_name.put(16908354, "accessibilityActionMoveWindow");
		system_widget_id_name.put(16908359, "accessibilityActionPageDown");
		system_widget_id_name.put(16908360, "accessibilityActionPageLeft");
		system_widget_id_name.put(16908361, "accessibilityActionPageRight");
		system_widget_id_name.put(16908358, "accessibilityActionPageUp");
		system_widget_id_name.put(16908362, "accessibilityActionPressAndHold");
		system_widget_id_name.put(16908346, "accessibilityActionScrollDown");
		system_widget_id_name.put(16908345, "accessibilityActionScrollLeft");
		system_widget_id_name.put(16908347, "accessibilityActionScrollRight");
		system_widget_id_name.put(16908343, "accessibilityActionScrollToPosition");
	}
}
