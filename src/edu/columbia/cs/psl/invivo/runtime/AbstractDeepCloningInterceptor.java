package edu.columbia.cs.psl.invivo.runtime;


public abstract class AbstractDeepCloningInterceptor  extends AbstractInterceptor {

	public AbstractDeepCloningInterceptor(Interceptable intercepted) {
		super(intercepted);
	}
	
//	public static Interceptable getRootCallee()
//	{
//		return createdCallees.get(getThreadChildId());
//	}
//	private static HashMap<Integer, Object> createdCallees = new HashMap<Integer, Object>();
//	protected Thread createChildThread(final MethodInvocation inv)
//	{
//		final int id = nextId.getAndIncrement();
//		return new Thread(new ThreadGroup(InvivoPreMain.config.getThreadPrefix()+id),new Runnable() {		
//			
//			public void run() {
//				try {
//					setAsChild(inv.getCallee(),id);
//					inv.setReturnValue(inv.getMethod().invoke(inv.getCallee(), inv.getParams()));
//					cleanupChild(id);
//				} catch (SecurityException e) {
//					e.printStackTrace();
//				} catch (IllegalArgumentException e) {
//					e.printStackTrace();
//				} catch (IllegalAccessException e) {
//					e.printStackTrace();
//				} catch (InvocationTargetException e) {
//					e.printStackTrace();
//				}
//			}
//		},InvivoPreMain.config.getThreadPrefix()+id);
//	}
}
