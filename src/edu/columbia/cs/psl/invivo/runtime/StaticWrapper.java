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
	
	public static Object getValue(Object owner, Object name, Object desc){
		System.out.println("Calling get value");
		int threadid = AbstractInterceptor.getThreadChildId();
		Object retvalue = null;

		// TODO handle cases where it does not appear in each of the 3 hashmaps
		if (hm.containsKey(threadid))
		{
			HashMap<String, HashMap<String, Object>> classmap = hm.get(threadid);
			if (classmap.containsKey(owner))
			{
				HashMap<String,Object> fieldmap = classmap.get(owner);
				printFieldMap(fieldmap, (String) owner);
				if (fieldmap.containsKey(name))
				{
					Object value = fieldmap.get(name);
					retvalue = value;

				}
			}
		}
		try
		{
			Class<?> clazz = Class.forName(((String)owner).replace('/', '.'));
			Field f = clazz.getDeclaredField((String)name);
			f.setAccessible(true);
			retvalue = f.get(null);
		}
		catch(Exception ex)
		{
			System.err.println(owner);
			ex.printStackTrace();
		}
		
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
						
						fieldmap.put((String) name, retvalue);
						System.out.println("Put value "+fieldmap.get(name).toString()+" to field "+(String) name+" in thread 0.");
					}
				}
			//return reference to original
			
		}
		
		//given retvalue, deal with pointsto
		if (retvalue != null)
		{
			if (ptm.containsKey(retvalue))
			{
				return ptm.get(retvalue);
			} else{
				return retvalue;
			}
		} else return null; //put a better error condition or exception in here
	}

	public static void setValue(Object value, Object owner, Object name, Object desc){
		
		System.out.println("Set value <"  + value+">" + "<"+owner+">"+"<"+name+">"+"<"+desc+">");
		int threadid = AbstractInterceptor.getThreadChildId();
		if (hm.containsKey(threadid))
		{
			HashMap<String,HashMap<String,Object>> classmap = hm.get(threadid);
			if (classmap.containsKey(owner))
			{
				HashMap<String,Object> fieldmap = classmap.get(owner);
				fieldmap.put((String)name, value);
			} else {
				HashMap<String,Object> fieldmap = new HashMap<String,Object>();
				fieldmap.put((String) name, value);
				classmap.put((String) owner, fieldmap);
			}
		} else {
			HashMap<String,HashMap<String,Object>> classmap = new HashMap<String,HashMap<String,Object>>();
			HashMap<String,Object> fieldmap = new HashMap<String,Object>();
			fieldmap.put((String) name, value);
			classmap.put((String) owner, fieldmap);
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
