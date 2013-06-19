package edu.columbia.cs.psl.invivo.runtime;

import java.util.HashSet;

public class InterceptorExecutorService {

	public static InterceptorExecutorService service = new InterceptorExecutorService();

	private InterceptorExecutorService() {
	}

	private static final int MAX_THREADS = 20;

	private HashSet<Long> tids = new HashSet<Long>();

	public synchronized void insertThis(long tid) {
		tids.add(tid);
	}

	public synchronized boolean isRunning(long tid) {
		return tids.contains(tid);
	}

	public synchronized void remove(Long tid) {
		tids.remove(tid);
	}
}
