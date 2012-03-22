
package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

import com.rits.cloning.Cloner;

import edu.columbia.cs.psl.invivo.struct.MethodInvocation;

public abstract class AbstractInterceptor {
	private Object interceptedObject;
	
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
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated, or is null (don't use this for a static call!)");
		} 
	}
	protected boolean isChild(Object callee)
	{
		if(callee == null || callee.getClass().equals(Class.class))
			return false;
		try {
			return callee.getClass().getField(InvivoPreMain.config.getChildField()).getInt(callee) > 0;
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated  (don't use this for a static call!)");
		}
	}
	public static int getThreadChildId()
	{
		if(Thread.currentThread().getName().startsWith(InvivoPreMain.config.getThreadPrefix()))
		{
			return Integer.parseInt(Thread.currentThread().getName().replace(InvivoPreMain.config.getThreadPrefix(), ""));
		}
		throw new IllegalStateException("Not in a child thread");
	}
	protected int getChildId(Object callee)
	{
		if(callee == null || callee.getClass().equals(Class.class))
			throw new IllegalArgumentException("The object requested was not intercepted and annotated (don't use this for a static call!)");
		try {
			return callee.getClass().getField(InvivoPreMain.config.getChildField()).getInt(callee);
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated (don't use this for a static call!)");
		}
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
	protected Cloner cloner = new Cloner();
	private Integer childId = 1;
	
	protected Thread createChildThread(final MethodInvocation inv)
	{
		final int id;
		synchronized (childId) {
			id = childId;
			childId++;
		}
		return new Thread(new Runnable() {		
			@Override
			public void run() {
				try {
					inv.callee = cloner.deepClone(inv.parent.callee);
					inv.params[inv.parent.params.length] = inv.callee;
					setAsChild(inv.callee,id);
					inv.returnValue = inv.method.invoke(null, inv.params);
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
