package zzg.staticanalysis.intent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Ignore the data, only match the Action & Categories & DataType
 */
public class IntentFilter {
	
	private final ArrayList<String> mActions;
	private ArrayList<String> mCategories = null;
	private ArrayList<String> mDataTypes = null;
	private boolean mHasPartialTypes = false;
	
	public IntentFilter() {
		mActions = new ArrayList<String>();
	}
	
	public IntentFilter(ArrayList<String> actions, ArrayList<String> categories) {
		mActions = actions;
		mCategories = categories;
	}
	
	///////////// Action //////////////
	public final void addAction(String action) {
		if (!mActions.contains(action)) {
			mActions.add(action.intern());
		}
	}

	public final int countActions() {
		return mActions.size();
	}

	public final String getAction(int index) {
		return mActions.get(index);
	}

	public final boolean hasAction(String action) {
		return action != null && mActions.contains(action);
	}

	public final boolean matchAction(String action) {
		return hasAction(action);
	}

	public final Iterator<String> actionsIterator() {
		return mActions != null ? mActions.iterator() : null;
	}
	
	///////////// Data Type //////////////
	public final void addDataType(String type) {
		final int slashpos = type.indexOf('/');
		final int typelen = type.length();
		if (slashpos > 0 && typelen >= slashpos+2) {
			if (mDataTypes == null) mDataTypes = new ArrayList<String>();
			if (typelen == slashpos+2 && type.charAt(slashpos+1) == '*') {
				String str = type.substring(0, slashpos);
				if (!mDataTypes.contains(str)) {
					mDataTypes.add(str.intern());
				}
				mHasPartialTypes = true;
			} else {
				if (!mDataTypes.contains(type)) {
					mDataTypes.add(type.intern());
				}
			}
			return;
		}
	}
	
	public final boolean hasDataType(String type) {
		return mDataTypes != null && findMimeType(type);
	}

	public final boolean hasExactDataType(String type) {
		return mDataTypes != null && mDataTypes.contains(type);
	}

	public final int countDataTypes() {
		return mDataTypes != null ? mDataTypes.size() : 0;
	}

	public final String getDataType(int index) {
		return mDataTypes.get(index);
	}

	public final Iterator<String> typesIterator() {
		return mDataTypes != null ? mDataTypes.iterator() : null;
	}
	
	public final boolean matchDataType(String type) {
		final ArrayList<String> types = mDataTypes;
		
		if (types == null) {
			if (type == null)
				return true;
			else
				return false;
		}else {
			if (!findMimeType(type)) {
				return false;
			}else
				return true;
		}
	}
	
	///////////// Category //////////////
	public final void addCategory(String category) {
		if (mCategories == null) 
			mCategories = new ArrayList<String>();
		if (!mCategories.contains(category)) {
			mCategories.add(category.intern());
		}
	}

	public final int countCategories() {
		return mCategories != null ? mCategories.size() : 0;
	}

	public final String getCategory(int index) {
		return mCategories.get(index);
	}

	public final boolean hasCategory(String category) {
		return mCategories != null && mCategories.contains(category);
	}

	public final Iterator<String> categoriesIterator() {
		return mCategories != null ? mCategories.iterator() : null;
	}

	public final String matchCategories(Set<String> categories) {
		if (categories == null) {
			return null;
		}
		Iterator<String> it = categories.iterator();
		if (mCategories == null) {
			return it.hasNext() ? it.next() : null;
		}
		while (it.hasNext()) {
			final String category = it.next();
			if (!mCategories.contains(category)) {
				return category;
			}
		}
		return null;
	}
	
	
	public final boolean match(String action, String type, Set<String> categories) {
		if (action != null && !matchAction(action)) {
			return false;
		}
		boolean dataMatch = matchDataType(type);
		if (!dataMatch) {
			return false;
		}
		String categoryMismatch = matchCategories(categories);
		if (categoryMismatch != null) {
			return false;
		}
		return true;
	}
	
	private final boolean findMimeType(String type) {
		final ArrayList<String> t = mDataTypes;
		if (type == null) {
			return false;
		}
		if (t.contains(type)) {
			return true;
		}
		// Deal with an Intent wanting to match every type in the IntentFilter.
		final int typeLength = type.length();
		if (typeLength == 3 && type.equals("*/*")) {
			return !t.isEmpty();
		}
		// Deal with this IntentFilter wanting to match every Intent type.
		if (mHasPartialTypes && t.contains("*")) {
			return true;
		}
		final int slashpos = type.indexOf('/');
		if (slashpos > 0) {
			if (mHasPartialTypes && t.contains(type.substring(0, slashpos))) {
				return true;
			}
			if (typeLength == slashpos+2 && type.charAt(slashpos+1) == '*') {
				// Need to look through all types for one that matches our base...
				final int numTypes = t.size();
				for (int i = 0; i < numTypes; i++) {
					final String v = t.get(i);
					if (type.regionMatches(0, v, 0, slashpos+1)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
