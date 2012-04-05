package edu.columbia.cs.psl.invivo.runtime;

import java.util.HashMap;

import com.rits.cloning.Cloner;

public class StaticWrapper {

//	public static HashMap<Integer, HashMap<String, HashMap<String, Object>>> hm = new HashMap<Integer, HashMap<String,;
//	public static HashMap<String, Object> hm = new HashMap<String, Object>();
//	Cloner c = new Cloner();
//	c.
	
/*	public static void initializeHash()
	{
		
	//	HashMap<String, HashMap<String,HashMap<String,Object>>> h2 = new HashMap<String, HashMap<String, HashMap<String, Object>>>();
	//	HashMap<String,HashMap<String,Object>> h3 = new HashMap<String, HashMap<String, Object>>();
	//	HashMap<String, Object> h4 = new HashMap<String, Object>();
	//	h4.put("hi", "world");
	//	h3.put("world", h4);
	//	h2.put("foo", h3);
	//	hm.put(1, h2);
	//	System.out.println(hm.get(1));
		
		
	} */
	
	/*
	 * 	clean out old threads that are dead. 
	 * Take a threadid as a parameter. 
	 * Jon will call it whenever a thread dies.
	 */
	public static void cleanupThread(int threadid)
	{
		return;
	}
	
	public static Object getValue(Object owner, Object name, Object desc){
		System.out.println("Calling get value");
	//	int threadid = AbstractInterceptor.getThreadChildId();
	//	System.out.println("Initializing HashMap");
	//	initializeHash();
	//	String hashkey = threadid + owner.toString()+name.toString()+desc.toString();
	//	System.out.println(hashkey);
//		hm.put(key, value);

		return new Integer(100);
	}

	public static void setValue(Object value, Object owner, Object name, Object desc){
		System.out.println("Set value <"  + value+">" + "<"+owner+">"+"<"+name+">"+"<"+desc+">");
		int threadid = 0;
		if (AbstractInterceptor.getThreadChildId() != 0)
		{
			threadid = AbstractInterceptor.getThreadChildId();
		}
	//	String hashkey = threadid + owner.toString()+name.toString()+desc.toString();
		System.out.println(hm.isEmpty());
	//	hm.put(hashkey, value);
	//	System.out.println(hm.toString());
		return;
	}
	public static String foo;
	public void foo(String in)
	{
		//StaticWrapper.getValue("Me", "you", "blah");
		StaticWrapper.foo = "abcd";
//		StaticWrapper.setValue("abc", "Me", "you", "blah");
	}
}
