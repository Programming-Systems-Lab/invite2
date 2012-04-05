package edu.columbia.cs.psl.invivo.runtime.visitor;

import org.apache.log4j.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;
import edu.columbia.cs.psl.invivo.runtime.NotInstrumented;
@NotInstrumented
public class InterceptingClassVisitor extends ClassVisitor {
	private Logger logger = Logger.getLogger(InterceptingClassVisitor.class);
	private String className;

	public InterceptingClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
	}

	private boolean runIMV = true;
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if(desc.equals(Type.getDescriptor(InvivoPreMain.config.getProtectorAnnotation())))
			runIMV = false;
		return super.visitAnnotation(desc, visible);
	}
	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc,
			String signature, String[] exceptions) {
		MethodVisitor mv = cv.visitMethod(acc, name, desc, signature,
				exceptions);
		if(runIMV)
		{
		InterceptingMethodVisitor imv = new InterceptingMethodVisitor(Opcodes.ASM4, mv, acc, name, desc);
		imv.setClassName(className);
		imv.setClassVisitor(this);
		return imv;
		}
		else
			return mv;
	}
	private boolean willRewrite = false;
	public void setShouldRewrite()
	{
		willRewrite = true;
	}
	@Override
	public void visitEnd() {
		super.visitEnd();
		if(willRewrite)
		{
			FieldNode fn = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PRIVATE,
					InvivoPreMain.config.getInterceptorFieldName(),
					Type.getDescriptor(InvivoPreMain.config.getInterceptorClass()), null, null);
			fn.accept(cv);
			
			
			FieldNode fn3 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
					InvivoPreMain.config.getStaticInterceptorFieldName(),
					Type.getDescriptor(InvivoPreMain.config.getInterceptorClass()), null, null);
			fn3.accept(cv);
			
			
			FieldNode fn2 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PUBLIC,
					InvivoPreMain.config.getChildField(),
					Type.INT_TYPE.getDescriptor(), null, 0); //TODO: abstract the interceptor type
			fn2.accept(cv);
			logger.info("Actually rewrote class: " + className);
		}
	}

	public void setClassName(String name) {
		this.className = name;
	}
}
