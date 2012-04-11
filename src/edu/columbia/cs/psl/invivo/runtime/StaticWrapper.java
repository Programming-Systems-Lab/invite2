package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import edu.columbia.cs.psl.invivo.runtime.AbstractInterceptor;

public class StaticWrapper {
	public static HashMap<Integer, HashMap<String, HashMap<String, Object>>> hm = new HashMap<Integer, HashMap<String, HashMap<String, Object>>>();
	public static HashMap<Object,Object> ptm = new HashMap<Object, Object>();
	
	/*
	 * clean out old threads that are dead. 
	 * Take a threadid as a parameter. 
	 * Jon will call it whenever a thread dies.
	 */
	public static void cleanupChildInvocation(int threadid)
	{
		return;
	}
	
	public static Object getValue(String owner, String name, Object curValue, boolean useLazyClone){
		System.out.println("Calling get value");
		int threadid = AbstractInterceptor.getThreadChildId();
		Object retValue = null;

		// TODO handle cases where it does not appear in each of the 3 hashmaps
		if (hm.containsKey(threadid))
		{
			HashMap<String, HashMap<String, Object>> classmap = hm.get(threadid);
			if (classmap.containsKey(owner))
			{
				HashMap<String,Object> fieldmap = classmap.get(owner);
				printFieldMap(fieldmap, owner);
				if (fieldmap.containsKey(name))
				{
					Object value = fieldmap.get(name);
					retValue = value;

				}
			}
		}
		retValue = curValue;
		
		if (threadid == 0)
		{
			
			//make a copy of value, store in hashmap
				if (hm.containsKey(0)) 
				{
					//TODO if hm !contain 0
					HashMap<String,HashMap<String,Object>> classmap = hm.get(0);
					if (classmap.containsKey(owner))
					{
						//TODO if classmap !contain owner
						HashMap<String,Object> fieldmap = classmap.get(owner);
						
						fieldmap.put(name, (useLazyClone ? AbstractInterceptor.shallowClone(retValue) : AbstractInterceptor.deepClone(retValue)));
						System.out.println("Put value "+fieldmap.get(name).toString()+" to field "+ name+" in thread 0.");
					}
				}
			//return reference to original
			
		}
		
		//given retvalue, deal with pointst
		if (retValue != null)
		{
			if (ptm.containsKey(retValue))
			{
				return ptm.get(retValue);
			} else{
				return retValue;
			}
		} else return null; //put a better error condition or exception in here
	}

	public static void setValue(Object value, String owner, String name, String desc){
		
		System.out.println("Set value <"  + value+">" + "<"+owner+">"+"<"+name+">"+"<"+desc+">");
		int threadid = AbstractInterceptor.getThreadChildId();
		if (hm.containsKey(threadid))
		{
			HashMap<String,HashMap<String,Object>> classmap = hm.get(threadid);
			if (classmap.containsKey(owner))
			{
				HashMap<String,Object> fieldmap = classmap.get(owner);
				fieldmap.put(name, value);
			} else {
				HashMap<String,Object> fieldmap = new HashMap<String,Object>();
				fieldmap.put(name, value);
				classmap.put(owner, fieldmap);
			}
		} else {
			HashMap<String,HashMap<String,Object>> classmap = new HashMap<String,HashMap<String,Object>>();
			HashMap<String,Object> fieldmap = new HashMap<String,Object>();
			fieldmap.put(name, value);
			classmap.put(owner, fieldmap);
			hm.put(threadid, classmap);
		}
		System.out.println(hm.isEmpty());
		return;
	}
	
	//TODO implement this method
	public static void addToPointsToMap(Object origObj, Object newObj, int childID)
	{
		
	}
	
	public static String foo;
	public void foo(String in)
	{
		//StaticWrapper.getValue("Me", "you", "blah");
		StaticWrapper.foo = "abcd";
//		StaticWrapper.setValue("abc", "Me", "you", "blah");
	}
	
	//pretty printer
	
	public static void printFieldMap(HashMap<String, Object> fm, String name)
	{
		Set<Entry<String, Object>> fields = fm.entrySet();
		Iterator it = fields.iterator();
		while (it.hasNext())
		{
			Entry<String, Object> el = (Entry<String,Object>) it.next();
			System.out.println("\t\t"+AbstractInterceptor.getThreadChildId() + " " +name.toUpperCase() + "."+ el.getKey()+ ":  " + (String) el.getValue());
		}
	}
}
