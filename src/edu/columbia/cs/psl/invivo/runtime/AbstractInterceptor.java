package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.rits.cloning.Cloner;

import edu.columbia.cs.psl.invivo.struct.MethodInvocation;

@NotInstrumented
public abstract class AbstractInterceptor {
	private Interceptable interceptedObject;
	private static ThreadLocal<Integer> childId = new ThreadLocal<Integer>() {
		protected Integer initialValue() {
			return 0;
		};
	};

	public AbstractInterceptor(Interceptable intercepted) {
		this.interceptedObject = intercepted;
	}

	public Interceptable getInterceptedObject() {
		return interceptedObject;
	}

	protected void setAsChild(Object obj, int childId) {
		try {
			obj.getClass().getField(InvivoPreMain.config.getChildField()).setInt(obj, childId);
			AbstractInterceptor.childId.set(childId);
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated (don't use this for a static call!)", e);
		}
	}

	public static Interceptable getRootCallee() {
		return createdCallees.get(getThreadChildId());
	}

	private static HashMap<Integer, Interceptable> createdCallees = new HashMap<Integer, Interceptable>();

	protected boolean isChild(Object callee) {
		if (callee == null || callee.getClass().equals(Class.class))
			return false;
		try {
			return callee.getClass().getField(InvivoPreMain.config.getChildField()).getInt(callee) > 0;
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated  (don't use this for a static call!)", e);
		}
	}

	protected Thread createRunnerThread(final MethodInvocation inv, boolean isChild) {
		final int id = (isChild ? nextId.getAndIncrement() : 0);
		return new Thread(new ThreadGroup(InvivoPreMain.config.getThreadPrefix() + id), new Runnable() {

			public void run() {
				try {
					createdCallees.put(id, inv.getCallee());
					if (id > 0)
						setAsChild(inv.getCallee(), id);
					inv.setReturnValue(inv.getCallee().runTest(inv.getMethodIdx(), inv.idx, inv.getCallee(), inv.getParams()));
					cleanupChild(id);
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		}, InvivoPreMain.config.getThreadPrefix() + id);
	}

	protected Thread createChildThread(MethodInvocation inv) {
		return createRunnerThread(inv, true);
	}

	/**
	 * Gets the child ID for the thread calling If this thread is directly a
	 * child process, we can simply return the thread ID - quick If it's not,
	 * then that ID will default still at 0 (which would also be the case for
	 * the parent execution) In that case, we crawl up the threadgroups to see
	 * if we're running under a child thread.
	 * 
	 * @return
	 */
	public static int getThreadChildId() {
		int ret = AbstractInterceptor.childId.get();
		if (ret > 0)
			return ret;
		return getThreadChildId(Thread.currentThread().getThreadGroup());
	}

	private static int getThreadChildId(ThreadGroup g) {
		if (g == null || g.getName().equals("main"))
			return 0;
		if (g.getName().startsWith(InvivoPreMain.config.getThreadPrefix()))
			return Integer.valueOf(g.getName().substring(InvivoPreMain.config.getThreadPrefix().length()));
		return getThreadChildId(g.getParent());
	}

	protected int getChildId(Object callee) {
		if (callee == null || callee.getClass().equals(Class.class))
			throw new IllegalArgumentException("The object requested was not intercepted and annotated (don't use this for a static call!)");
		try {
			return callee.getClass().getField(InvivoPreMain.config.getChildField()).getInt(callee);
		} catch (Exception e) {
			throw new IllegalArgumentException("The object requested was not intercepted and annotated (don't use this for a static call!)", e);
		}
	}

	protected void cleanupChild(int childId) {
		StaticWrapper.cleanupChildInvocation(childId);
		createdCallees.remove(childId);

	}

	public static Method getMethod(String methodName, Class<?>[] params, Class<?> clazz) throws NoSuchMethodException {
		return getMethod(methodName, params, clazz, clazz);
	}

	public static Method getMethod(String methodName, Class<?>[] params, Class<?> clazz, Class<?> originalClazz) throws NoSuchMethodException {
		try {
			for (Method m : clazz.getDeclaredMethods()) {
				boolean ok = true;
				if (m.getName().equals(methodName)) {
					Class<?>[] mArgs = m.getParameterTypes();
					if (mArgs.length != params.length)
						break;
					for (int i = 0; i < mArgs.length; i++)
						if (!mArgs[i].isAssignableFrom(params[i]))
							ok = false;

					if (ok) {
						if (!m.isAccessible())
							m.setAccessible(true);
						return m;
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		if (clazz.getSuperclass() != null)
			return getMethod(methodName, params, clazz.getSuperclass(), originalClazz);
		throw new NoSuchMethodException(originalClazz.getCanonicalName() + "." + methodName + "(" + implode(params) + ")");
	}

	private static String implode(Object[] array) {
		StringBuilder ret = new StringBuilder();
		if (array == null || array.length == 0)
			return "";
		for (Object o : array) {
			ret.append(o);
			ret.append(", ");
		}
		return ret.substring(0, ret.length() - 2);
	}

	public static Method getMethod(String methodName, Class<?> clazz) {
		try {
			for (Method m : clazz.getDeclaredMethods()) {
				if (m.getName().equals(methodName)) {
					if (!m.isAccessible())
						m.setAccessible(true);
					return m;
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		if (clazz.getSuperclass() != null)
			return getMethod(methodName, clazz.getSuperclass());
		return null;
	}

	protected Method getMethod(String methodName, String[] types, Class<?> clazz) {
		try {
			for (Method m : clazz.getDeclaredMethods()) {
				boolean ok = true;
				if (m.getName().equals(methodName)) {
					Class<?>[] mArgs = m.getParameterTypes();
					if (mArgs.length != types.length)
						break;
					for (int i = 0; i < mArgs.length; i++)
						if (!mArgs[i].getName().equals(types[i]))
							ok = false;

					if (ok) {
						if (!m.isAccessible())
							m.setAccessible(true);
						return m;
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		if (clazz.getSuperclass() != null)
			return getMethod(methodName, types, clazz.getSuperclass());
		return null;
	}

	protected Method getCurMethod(String methodName, String[] types) {
		return getMethod(methodName, types, interceptedObject.getClass());
	}

	protected static final AtomicInteger nextId = new AtomicInteger(1);

	private static Cloner cloner = new Cloner();

	public static <T> T shallowClone(T obj) {
		T ret = cloner.shallowClone(obj);
		COWAInterceptor.setIsAClonedObject(ret);
		return ret;
	}

	public static <T> T deepClone(T obj) {
		T ret = cloner.deepClone(obj);
		return ret;
	}

	public final void __onExit(Object val, int op, int id) {
		onExit(val, op, id);
	}

	public final int __onEnter(int methodIdx, Object[] params, Interceptable callee) {
		return onEnter(callee, methodIdx, params);
	}

	public abstract int onEnter(Interceptable callee, int methodIdx, Object[] params);

	public abstract void onExit(Object val, int op, int id);

}
