package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import com.rits.cloning.Cloner;

import edu.columbia.cs.psl.invivo.struct.MethodInvocation;

public abstract class AbstractDeepCloningInterceptor  extends AbstractInterceptor {

	public AbstractDeepCloningInterceptor(Object intercepted) {
		super(intercepted);
	}
	public final void __onExit(Object val, int op, int id)
	{
		onExit(val, op, id);
	}
	public final int __onEnter(String methodName, String[] types, Object[] params, Object callee)
	{
		return onEnter(callee, getCurMethod(methodName,types), params);
	}
	public abstract int onEnter(Object callee, Method method, Object[] params);
	
	public abstract void onExit(Object val, int op, int id);
	public static Object getRootCallee()
	{
		return createdCallees.get(getThreadChildId());
	}
	private static HashMap<Integer, Object> createdCallees = new HashMap<Integer, Object>();
	protected Thread createChildThread(final MethodInvocation inv)
	{
		final int id = nextId.getAndIncrement();
		return new Thread(new ThreadGroup(InvivoPreMain.config.getThreadPrefix()+id),new Runnable() {		
			
			public void run() {
				try {
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
	private Cloner cloner = new Cloner();
	protected <T> T deepClone(T obj)
	{
		T ret = cloner.deepClone(obj);
		COWAInterceptor.setIsAClonedObject(ret);
		return ret;
	}
}
