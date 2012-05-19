package edu.columbia.cs.psl.invivo.struct;

import java.io.Serializable;
import java.lang.reflect.Method;

import edu.columbia.cs.psl.invivo.runtime.NotInstrumented;

/**
 * The primary MethodInvocation data structure.
 * 
 */

@NotInstrumented
public class MethodInvocation implements Serializable
{
	private static final long serialVersionUID = -2038681047970130055L;
	
	private Object callee;
	
	private Method method;
	
	private Object[] params;
	
	private Object returnValue;
	
	private Exception thrownExceptions;
	
	private Thread thread;
	
	private MethodInvocation[] children;
	
	private Method checkMethod;
	
	private MethodInvocation parent;
		
	@Override
	public String toString() {
		StringBuilder paramStr = new StringBuilder();
		
		if(getParams() != null)
			for(Object v : getParams()) {
				if(v != null)
					paramStr.append(v.toString());
			}
			
		StringBuilder childStr = new StringBuilder();
		if(getChildren() != null) {
			for(MethodInvocation i : getChildren())
				childStr.append(i.toString() + ",");
		}
		
		return "[Invocation on method "+ (method == null ? "null" : method.getName()) + " with params " + paramStr.toString() + " returning " + getReturnValue() +" on object " + callee +".  Children: ["+childStr.toString()+"] ]";
	}


	public Exception getThrownExceptions() {
		return thrownExceptions;
	}


	public void setThrownExceptions(Exception thrownExceptions) {
		this.thrownExceptions = thrownExceptions;
	}


	public Thread getThread() {
		return thread;
	}


	public void setThread(Thread thread) {
		this.thread = thread;
	}


	public Method getCheckMethod() {
		return checkMethod;
	}


	public void setCheckMethod(Method checkMethod) {
		this.checkMethod = checkMethod;
	}


	public MethodInvocation getParent() {
		return parent;
	}

	public void setParent(MethodInvocation parent) {
		this.parent = parent;
	}
	
	public Object getCallee() {
		return this.callee;
	}
	
	public void setCallee(Object c) {
		this.callee = c;
	}
	
	public Method getMethod() {
		return this.method;
	}
	
	public void setMethod(Method m) {
		this.method = m;
	}

	public Object[] getParams() {
		return params;
	}
	
	public void setParams(Object[] params) {
		this.params = params;
	}

	public Object getReturnValue() {
		return returnValue;
	}

	public void setReturnValue(Object returnValue) {
		this.returnValue = returnValue;
	}

	public MethodInvocation[] getChildren() {
		return children;
	}

	public void setChildren(MethodInvocation[] children) {
		this.children = children;
	}	
}
