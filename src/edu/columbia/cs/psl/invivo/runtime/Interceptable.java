package edu.columbia.cs.psl.invivo.runtime;

public interface Interceptable {
	public int getNumberOfTests(int methodNum);
	public Object runTest(int methodNum,int ruleIdx, Object owner, Object[] params);
	public boolean runCheck(int methodNum,int ruleIdx, Object originalReturn, Object newReturn, Object[] params);
	public String getRuleDescription(int methodNum,int ruleIdx);
	public String getFullMethodName(int methodNum);

}
