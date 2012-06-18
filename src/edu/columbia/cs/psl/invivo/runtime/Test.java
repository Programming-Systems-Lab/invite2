package edu.columbia.cs.psl.invivo.runtime;

public class Test {

	public static void main(String args[]) throws InterruptedException {
		InterceptorExecutorService.service.remove(Thread.currentThread().getId());
	}
}
