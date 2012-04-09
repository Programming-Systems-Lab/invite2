package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.rits.cloning.Cloner;

import edu.columbia.cs.psl.invivo.struct.MethodInvocation;

public abstract class AbstractLazyCloningInterceptor extends AbstractInterceptor {

	public AbstractLazyCloningInterceptor(Object intercepted) {
		super(intercepted);
	}
	public static Object getRootCallee()
	{
		return createdCallees.get(getThreadChildId());
	}
	private static HashMap<Integer, Object> createdCallees = new HashMap<Integer, Object>();
	protected Thread createRunnerThread(final MethodInvocation inv, boolean isChild)
	{
			final int id = (isChild ? nextId.getAndIncrement() : 0);
		return new Thread(new ThreadGroup(InvivoPreMain.config.getThreadPrefix()+id),new Runnable() {		
			
			public void run() {
				try {
					createdCallees.put(id, inv.callee);
					if(id > 0)
					setAsChild(inv.callee,id);
					inv.returnValue = inv.method.invoke(inv.callee, inv.params);
					cleanupChild(id);
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		},InvivoPreMain.config.getThreadPrefix()+id);
	}
	protected Thread createChildThread(MethodInvocation inv)
	{
		return createRunnerThread(inv, true);
	}
	
	public final Object __onCall(String methodName, String[] types, Object[] params, Object callee)
	{
		return onCall(callee, getCurMethod(InvivoPreMain.config.getInterceptedPrefix()+methodName,types), params);
	}
	
	public abstract Object onCall(Object callee, Method curMethod, Object[] params);
	
	@Override
	protected void cleanupChild(int childId) {
		super.cleanupChild(childId);
		createdCallees.remove(childId);
	}
	
	private Cloner cloner = new Cloner();
	protected <T> T shallowClone(T obj)
	{
		T ret = cloner.shallowClone(obj);
		COWAInterceptor.setIsAClonedObject(ret);
		return ret;
	}
	protected <T> T deepClone(T obj)
	{
		T ret = cloner.deepClone(obj);
		return ret;
	}
}
