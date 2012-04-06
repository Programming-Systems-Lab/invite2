
package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;


import com.rits.cloning.Cloner;

import edu.columbia.cs.psl.invivo.struct.MethodInvocation;

@NotInstrumented
public abstract class AbstractInterceptor {
	private Object interceptedObject;
	private static ThreadLocal<Integer> childId = new ThreadLocal<Integer>()
			{
			protected Integer initialValue() {return 0;};
			};
	
	public AbstractInterceptor(Object intercepted)
	{
		this.interceptedObject = intercepted;
	}
	
	public Object getInterceptedObject() {
		return interceptedObject;
	}
	public abstract int onEnter(Object callee, Method method, Object[] params);
	
	public abstract void onExit(Object val, int op, int id);

	protected void setAsChild(Object obj, int childId)
	{
		try {
			obj.getClass().getField(InvivoPreMain.config.getChildField()).setInt(obj, childId);
			AbstractInterceptor.childId.set(childId);
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated (don't use this for a static call!)",e);
		} 
	}
	protected boolean isChild(Object callee)
	{
		if(callee == null || callee.getClass().equals(Class.class))
			return false;
		try {
			return callee.getClass().getField(InvivoPreMain.config.getChildField()).getInt(callee) > 0;
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated  (don't use this for a static call!)",e);
		}
	}
	/**
	 * Gets the child ID for the thread calling
	 * If this thread is directly a child process, we can simply return
	 * the thread ID - quick
	 * If it's not, then that ID will default still at 0 (which would also be the case for the parent execution)
	 * In that case, we crawl up the threadgroups to see if we're running under a child thread.
	 * @return
	 */
	public static int getThreadChildId()
	{
		int ret = AbstractInterceptor.childId.get();
		if(ret > 0)
			return ret;
		return getThreadChildId(Thread.currentThread().getThreadGroup());
	}
	private static int getThreadChildId(ThreadGroup g)
	{
		if(g == null || g.getName().equals("main"))
			return 0;
		if(g.getName().startsWith(InvivoPreMain.config.getThreadPrefix()))
			return Integer.valueOf(g.getName().substring(InvivoPreMain.config.getThreadPrefix().length()));
		return getThreadChildId(g.getParent());
	}
	protected int getChildId(Object callee)
	{
		if(callee == null || callee.getClass().equals(Class.class))
			throw new IllegalArgumentException("The object requested was not intercepted and annotated (don't use this for a static call!)");
		try {
			return callee.getClass().getField(InvivoPreMain.config.getChildField()).getInt(callee);
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated (don't use this for a static call!)",e);
		}
	}
	protected void cleanupChild(int childId)
	{
		StaticWrapper.cleanupChildInvocation(childId);
		createdCallees.remove(childId);
	}
	public final void __onExit(Object val, int op, int id)
	{
		onExit(val, op, id);
	}
	public final int __onEnter(String methodName, String[] types, Object[] params, Object callee)
	{
		return onEnter(callee, getCurMethod(methodName,types), params);
	}
	protected Method getMethod(String methodName,Class<?>[] params, Class<?> clazz) throws NoSuchMethodException
	{
		return getMethod(methodName, params, clazz, clazz);
	}
	protected Method getMethod(String methodName, Class<?>[] params, Class<?> clazz, Class<?> originalClazz) throws NoSuchMethodException
	{
		try {
			for(Method m : clazz.getDeclaredMethods())
			{
				boolean ok = true;
				if(m.getName().equals(methodName))
				{
					Class<?>[] mArgs = m.getParameterTypes();
					if(mArgs.length != params.length)
						break;
					for(int i = 0;i<mArgs.length;i++)
						if(!mArgs[i].isAssignableFrom(params[i]))
							ok = false;

					if(ok)
					{
						if(!m.isAccessible())
							m.setAccessible(true);
						return m;
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		if(clazz.getSuperclass() != null)
			return getMethod(methodName, params, clazz.getSuperclass(),originalClazz);
		throw new NoSuchMethodException(originalClazz.getCanonicalName() +"."+methodName + "("+ implode(params) + ")");
	}
	private String implode(Object[] array)
	{
		StringBuilder ret = new StringBuilder();
		if(array == null || array.length == 0)
			return "";
		for(Object o : array)
		{
			ret.append(o);
			ret.append(", ");
		}
		return ret.substring(0, ret.length()-2);
	}
	private Method getMethod(String methodName, String[] types, Class<?> clazz)
	{
		try {
			for(Method m : clazz.getDeclaredMethods())
			{
				boolean ok = true;
				if(m.getName().equals(methodName))
				{
					Class<?>[] mArgs = m.getParameterTypes();
					if(mArgs.length != types.length)
						break;
					for(int i = 0;i<mArgs.length;i++)
						if(!mArgs[i].getName().equals(types[i]))
							ok = false;

					if(ok)
					{
						if(!m.isAccessible())
							m.setAccessible(true);
						return m;
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		if(clazz.getSuperclass() != null)
			return getMethod(methodName, types, clazz.getSuperclass());
		return null;
	}
	private Method getCurMethod(String methodName,String[] types)
	{
		return getMethod(methodName,types,interceptedObject.getClass());
	}
	private Cloner cloner = new Cloner();
	private static final AtomicInteger nextId = new AtomicInteger(1);

	/**
	 * Deep clone an object. Currently just delegates out but we may need to do more here manually,
	 * hence use this abstraction please.
	 * @param obj
	 * @return
	 */
	protected <T> T deepClone(T obj)
	{
		return cloner.deepClone(obj);
	}
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
					createdCallees.put(id, inv.callee);
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
}
