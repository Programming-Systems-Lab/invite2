package edu.columbia.cs.psl.invivo.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.directory.InvalidAttributesException;

import org.apache.log4j.Logger;

public class StaticWrapper {
	public static HashMap<Integer, HashMap<String, HashMap<String, Object>>> hm = new HashMap<Integer, HashMap<String, HashMap<String, Object>>>();
	private static Logger logger = Logger.getLogger(StaticWrapper.class);

	/*
	 * clean out old threads that are dead. Take a threadid as a parameter. Jon
	 * will call it whenever a thread dies.
	 */
	public static void cleanupChildInvocation(int threadid) {
		if (hm.containsKey(threadid)) {
			hm.remove(threadid);
		}
		return;
	}

	public static Object getValue(String owner, String name, Object curValue,
			boolean useLazyClone) throws InvalidAttributesException, SecurityException, NoSuchFieldException, ClassNotFoundException {
		System.out.println("Calling get value");
		int threadid = AbstractInterceptor.getThreadChildId();
		Object retValue = null;

		if (hm.containsKey(threadid)) {
			HashMap<String, HashMap<String, Object>> classmap = hm
					.get(threadid);
			if (classmap.containsKey(owner)) {
				HashMap<String, Object> fieldmap = classmap.get(owner);
				printFieldMap(fieldmap, owner);
				if (fieldmap.containsKey(name)) {
					Object value = fieldmap.get(name);
					retValue = value;
				} else {
					// thread is in hm and class is in classmap but field is not in fieldmap
					Object classValue = Class.forName(owner).getField(name);
					fieldmap.put(name, classValue);
					retValue = classValue;
				}
			} else {
				// thread is in hm but class is not in classmap
				Class clazz = Class.forName(owner);
				HashMap<String, Object> fieldmap = new HashMap<String,Object>();
				Object classValue = clazz.getField(name);
				fieldmap.put(name, classValue);
				classmap.put(clazz.getSimpleName(), fieldmap);
				retValue = classValue;
			}
		} else {
			// thread is not in hm
			HashMap<String, HashMap<String,Object>> classmap = new HashMap<String, HashMap<String, Object>>();
			HashMap<String, Object> fieldmap = new HashMap<String,Object>();
			Class clazz = Class.forName(owner);
			Object classValue = clazz.getField(name);
			fieldmap.put(name, classValue);
			classmap.put(owner, fieldmap);
			hm.put(threadid, classmap);
			// also need to add pointsto map for this new thread
			//TODO  does the key need to be an object too or will a string suffice?
			HashMap<String, Object> ptm = new HashMap<String,Object>();
			classmap.put("pointsto", ptm);
			retValue = classValue;	
		}
		retValue = curValue;

		if (threadid == 0) {

			// make a copy of value, store in hashmap
			if (hm.containsKey(0)) {
				// TODO if hm !contain 0
				HashMap<String, HashMap<String, Object>> classmap = hm.get(0);
				if (classmap.containsKey(owner)) {
					// TODO if classmap !contain owner
					HashMap<String, Object> fieldmap = classmap.get(owner);

					fieldmap.put(
							name,
							(useLazyClone ? AbstractInterceptor
									.shallowClone(retValue)
									: AbstractInterceptor.deepClone(retValue)));
					logger.debug("Put value " + fieldmap.get(name).toString()
							+ " to field " + name + " in thread 0.");
				}
			}
			// return reference to original

		}

		// given retvalue, deal with pointsto
		if (retValue != null)

		{
			HashMap<String,HashMap<String,Object>> ptm = hm.get(threadid);
			if (ptm.containsKey(retValue)) {
				return ptm.get(retValue);
			} else {
				return retValue;
			}
		}
		else
			throw new InvalidAttributesException();

	}

	public static void setValue(Object value, String owner, String name,
			String desc) {

		System.out.println("Set value <" + value + ">" + "<" + owner + ">"
				+ "<" + name + ">" + "<" + desc + ">");
		int threadid = AbstractInterceptor.getThreadChildId();
		if (hm.containsKey(threadid)) {
			HashMap<String, HashMap<String, Object>> classmap = hm
					.get(threadid);
			if (classmap.containsKey(owner)) {
				HashMap<String, Object> fieldmap = classmap.get(owner);
				fieldmap.put(name, value);
			} else {
				HashMap<String, Object> fieldmap = new HashMap<String, Object>();
				fieldmap.put(name, value);
				classmap.put(owner, fieldmap);
			}
		} else {
			HashMap<String, HashMap<String, Object>> classmap = new HashMap<String, HashMap<String, Object>>();
			HashMap<String, Object> fieldmap = new HashMap<String, Object>();
			fieldmap.put(name, value);
			classmap.put(owner, fieldmap);
			hm.put(threadid, classmap);
		}
		System.out.println(hm.isEmpty());
		return;
	}

	// TODO implement this method
	public static void addToPointsToMap(Object origObj, Object newObj,
			int childID) {

	}

	public static String foo;

	public void foo(String in) {
		// StaticWrapper.getValue("Me", "you", "blah");
		StaticWrapper.foo = "abcd";
		// StaticWrapper.setValue("abc", "Me", "you", "blah");
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
