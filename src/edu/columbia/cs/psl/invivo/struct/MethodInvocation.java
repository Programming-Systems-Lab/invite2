package edu.columbia.cs.psl.invivo.struct;

import java.io.Serializable;
import java.lang.reflect.Method;

import edu.columbia.cs.psl.invivo.runtime.Interceptable;
import edu.columbia.cs.psl.invivo.runtime.NotInstrumented;

/**
 * The primary MethodInvocation data structure.
 * 
 */

@NotInstrumented
public class MethodInvocation implements Serializable
{
	private static final long serialVersionUID = -2038681047970130055L;
	
	public Interceptable callee;
		
	public String methodName;
	
	public Object[] params;
	
	public Object returnValue;
	
	public Exception thrownExceptions;
	
	public Thread thread;
	
	public MethodInvocation[] children;
		
	public MethodInvocation parent;

	public int idx;

	protected int methodIdx;

	public Method method;

	public int getMethodIdx() {
		return methodIdx;
	}
	public void setMethodIdx(int methodIdx) {
		this.methodIdx = methodIdx;
	}
	@Override
	public String toString() {
		StringBuilder paramStr = new StringBuilder();
		
		if(getParams() != null)
			for(Object v : getParams()) {
				if(v != null)
					paramStr.append(v.toString()+",");
			}
			
		StringBuilder childStr = new StringBuilder();
		if(getChildren() != null) {
			for(MethodInvocation i : getChildren())
				childStr.append(i.toString() + ",");
		}
		
		return "[Invocation on method  with params (" + paramStr.substring(0, paramStr.length()-1)+ ") returning " + getReturnValue() +" on object " + callee +".  Children: ["+childStr.toString()+"] ]";
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

	public MethodInvocation getParent() {
		return parent;
	}

	public void setParent(MethodInvocation parent) {
		this.parent = parent;
	}
	
	public Interceptable getCallee() {
		return this.callee;
	}
	
	public void setCallee(Interceptable c) {
		this.callee = c;
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
