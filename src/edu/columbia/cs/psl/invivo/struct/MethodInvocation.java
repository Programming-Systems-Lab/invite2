package edu.columbia.cs.psl.invivo.struct;

import java.io.Serializable;
import java.lang.reflect.Method;

import edu.columbia.cs.psl.invivo.runtime.NotInstrumented;

@NotInstrumented
public class MethodInvocation  implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2038681047970130055L;
	public Object callee;
	public Method method;
	public Object[] params;
	public Object returnValue;
	public Exception thrownExceptions;
	public Thread thread;
	public MethodInvocation[] children;
	public Method checkMethod;
	public MethodInvocation parent;
	
	@Override
	public String toString() {
		String paramStr = "";
		if(params != null)
		for(Object v : params)
		{
			if(v != null)
			paramStr += v.toString();
		}
		String childStr = "";
		if(children != null)
			for(MethodInvocation i : children)
				childStr += i.toString() +",";
		return "[Invocation on method "+ (method == null ? "null" : method.getName()) + " with params " + paramStr + " returning " + returnValue +" on object " + callee +".  Children: ["+childStr+"] ]";
	}
}