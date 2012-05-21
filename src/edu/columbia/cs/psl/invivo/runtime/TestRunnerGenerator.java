package edu.columbia.cs.psl.invivo.runtime;


import org.objectweb.asm.ClassVisitor;

public abstract class TestRunnerGenerator<T extends ClassVisitor> {
	protected T cv;
	public TestRunnerGenerator(T cv)
	{
		this.cv = cv;
	}
	/**
	 * Generate the test runner and return its name
	 * @return Fully qualified name of the test runner
	 */
	public abstract String generateTestRunner();
	
}