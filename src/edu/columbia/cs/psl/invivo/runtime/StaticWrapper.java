package edu.columbia.cs.psl.invivo.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.directory.InvalidAttributesException;

import org.apache.log4j.Logger;

public class StaticWrapper {
	public static HashMap<Integer, HashMap<String, HashMap<String, Object>>> hm = new HashMap<Integer, HashMap<String, HashMap<String, Object>>>();
	public static HashMap<Integer, HashMap<Object, Object>> pointsTo = new HashMap<Integer, HashMap<Object, Object>>();
	private static Logger logger = Logger.getLogger(StaticWrapper.class);

	/*
	 * clean out old threads that are dead. Take a childid as a parameter. Jon
	 * will call it whenever a thread dies.
	 */
	public static void cleanupChildInvocation(int childId) {
		hm.remove(childId);
		pointsTo.remove(childId);
	}

	public static Object getValue(String owner, String name, Object curValue,
			boolean useLazyClone) throws InvalidAttributesException,
			SecurityException, NoSuchFieldException, ClassNotFoundException {
		System.out.println("Calling get value");
		int childId = AbstractInterceptor.getThreadChildId();
		Object retValue = null;

		if (childId > 0) {
			
			if(!hm.containsKey(childId))
				hm.put(childId, new HashMap<String, HashMap<String,Object>>());
			if(!hm.get(childId).containsKey(owner))
				hm.get(childId).put(owner, new HashMap<String, Object>());
			
			HashMap<String, Object> fieldmap = hm.get(childId).get(owner);
					if (!fieldmap.containsKey(name)) {
						//fieldmap does not contain this field
						Object toUse = null;
						if (hm.containsKey(0) &&
								hm.get(0).containsKey(owner)
								&& hm.get(0).get(owner).containsKey(name))
							toUse = hm.get(0).get(owner).get(name);
						else
							toUse = curValue;
						fieldmap.put(name, (useLazyClone ? AbstractInterceptor
								.shallowClone(toUse) : AbstractInterceptor
								.deepClone(toUse)));
					}
					retValue = fieldmap.get(name);
		} else if (childId == 0) {
			retValue = curValue;
			
			// make a copy of value, store in hashmap
			if (!hm.containsKey(0))
				hm.put(0, new HashMap<String, HashMap<String, Object>>());
			if (!hm.get(0).containsKey(owner))
				hm.get(0).put(owner, new HashMap<String, Object>());
			if (!hm.get(0).get(owner).containsKey(name))
				hm.get(0).get(owner).put(name, (useLazyClone ? 
						AbstractInterceptor.shallowClone(retValue) : 
							AbstractInterceptor.deepClone(retValue)));
		}

		// given retvalue, deal with pointsto
		HashMap<Object, Object> ptm = pointsTo.get(childId);
		if (ptm != null && ptm.containsKey(retValue)) {
			return ptm.get(retValue);
		} else {
			return retValue;
		}
	}

	public static void setValue(Object value, String owner, String name,
			String desc) {

		int childId = AbstractInterceptor.getThreadChildId();
		if (!hm.containsKey(childId))
			hm.put(childId, new HashMap<String, HashMap<String,Object>>());
		if(!hm.get(childId).containsKey(owner))
			hm.get(childId).put(owner, new HashMap<String, Object>());
		hm.get(childId).get(owner).put(name, value);
		return;
		
	}

	public static void addToPointsToMap(Object origObj, Object newObj,
			int childID) {
		if (!pointsTo.containsKey(childID))
			pointsTo.put(childID, new HashMap<Object, Object>());
		pointsTo.get(childID).put(origObj, newObj);
	}


	// pretty printer
	public static void printFieldMap(HashMap<String, Object> fm, String name) {
		Set<Entry<String, Object>> fields = fm.entrySet();
		Iterator<Entry<String, Object>> it = fields.iterator();
		while (it.hasNext()) {
			Entry<String, Object> el = it.next();
			System.out.println("\t\t" + AbstractInterceptor.getThreadChildId()
					+ " " + name.toUpperCase() + "." + el.getKey() + ":  "
					+ (String) el.getValue());
		}
	}
}
