package edu.columbia.cs.psl.invivo.runtime;

import java.util.ArrayList;
import java.util.List;

public class InterceptorExecutorService {

	public static InterceptorExecutorService service = new InterceptorExecutorService();

	private InterceptorExecutorService() {
	}

	private static final int MAX_THREADS = 20;

	private List<Long> tids = new ArrayList<Long>();

	public synchronized void insertThis(long tid) {
		for (Long t : tids) {
			if (t == tid) {
				System.out.println("Identical name found");
				return;
			}
		}
		tids.add(tid);
	}

	public synchronized boolean isRunning(long tid) {
		for (Long t : tids) {
			if (t == tid)
				return true;
		}
		return false;
	}

	public synchronized void remove(Long tid) {
		for (Long t : tids) {
			if (t == tid) {
				tids.remove(t);
				break;
			}
		}
	}
}
