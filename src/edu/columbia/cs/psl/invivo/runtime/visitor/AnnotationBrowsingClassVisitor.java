package edu.columbia.cs.psl.invivo.runtime.visitor;

import java.util.HashSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AnnotationBrowsingClassVisitor extends ClassVisitor {

	public AnnotationBrowsingClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
	}
	private HashSet<AnnotationBrowsingMethodVisitor> mvs = new HashSet<AnnotationBrowsingMethodVisitor>();
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		AnnotationBrowsingMethodVisitor mv = new AnnotationBrowsingMethodVisitor(Opcodes.ASM4, name,desc,super.visitMethod(access, name, desc, signature, exceptions));
		mvs.add(mv);
		return mv;
	}
	public HashSet<String> getMethodsWithAnnotation() {
		return methodsWithAnnotation;
	}
	private HashSet<String> methodsWithAnnotation = new HashSet<String>();
	@Override
	public void visitEnd() {
		super.visitEnd();
		for(AnnotationBrowsingMethodVisitor mv : mvs)
		{
			if(mv.isHasAnnotation())
			{
				methodsWithAnnotation.add(mv.getNameDesc());
			}
		}
	}
}
