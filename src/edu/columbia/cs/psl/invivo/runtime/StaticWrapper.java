package edu.columbia.cs.psl.invivo.runtime;

public class StaticWrapper {

	public static Object getValue(Object owner, Object name, Object desc){
		System.out.println("Calling get value");

		return new Integer(100);
	}

	public static void setValue(Object value, Object owner, Object name, Object desc){
		System.out.println("Set value <"  + value+">" + "<"+owner+">"+"<"+name+">"+"<"+desc+">");
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
