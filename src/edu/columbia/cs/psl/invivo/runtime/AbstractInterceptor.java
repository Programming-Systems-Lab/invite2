
package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
	
	protected void setChild(Object obj, boolean val)
	{
		try {
			obj.getClass().getField(InvivoPreMain.config.getChildField()).setBoolean(obj, val);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void setAsChild(Object obj)
	{
		try {
			obj.getClass().getField(InvivoPreMain.config.getChildField()).setBoolean(obj, true);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	protected boolean isChild(Object callee)
	{
		if(callee == null || callee.getClass().equals(Class.class))
			return false;
		try {
			return callee.getClass().getField(InvivoPreMain.config.getChildField()).getBoolean(callee);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
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
	protected Thread createChildThread(final MethodInvocation inv)
	{
		return new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Object clone = cloner.deepClone(inv.parent.callee);
					inv.params[inv.parent.params.length] = clone;
					setAsChild(clone);
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
		});
	}
}
