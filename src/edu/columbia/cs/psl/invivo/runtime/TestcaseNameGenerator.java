package edu.columbia.cs.psl.invivo.runtime;

import java.util.List;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

public interface TestcaseNameGenerator {
	public String getTestCaseClassName(String className, MethodNode mn);
}
