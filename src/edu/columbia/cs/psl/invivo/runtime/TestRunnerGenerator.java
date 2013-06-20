package edu.columbia.cs.psl.invivo.runtime;


import edu.columbia.cs.psl.invivo.runtime.visitor.InterceptingClassVisitor;


public abstract class TestRunnerGenerator{
	
	protected InterceptingClassVisitor cv;
	public TestRunnerGenerator(InterceptingClassVisitor cv)
	{
		this.cv = cv;
	}
	public abstract void visitEnd();
}
