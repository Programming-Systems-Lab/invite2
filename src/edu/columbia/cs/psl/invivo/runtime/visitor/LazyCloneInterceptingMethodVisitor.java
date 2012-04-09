package edu.columbia.cs.psl.invivo.runtime.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;
import edu.columbia.cs.psl.invivo.runtime.StaticWrapper;


public class LazyCloneInterceptingMethodVisitor extends AdviceAdapter {


	protected LazyCloneInterceptingMethodVisitor(int api, MethodVisitor mv, int access,
			String name, String desc) {
		super(api, mv, access, name, desc);
	}
	boolean rewrite = false;

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if(desc.equals(InvivoPreMain.config.getAnnotationDescriptor()))
		{
			classVisitor.setShouldRewrite();
			rewrite = true;
		}
		return super.visitAnnotation(desc, visible);
	}
	int x;
	boolean y;
	private void foo()
	{
		if(y || x==0)
			System.out.println(x);
	}
	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		if(!rewrite)
		{
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
		if(opcode == GETFIELD && desc.length() > 1)
		{	
//			super.visitFieldInsn(opcode, owner, name, desc); //Do NOT do any crazy COA stuff yet
			Label lblbForReadThrough = new Label();
			Label lblForNextInsn = new Label();
			dup();
			super.visitFieldInsn(GETFIELD, owner, name+InvivoPreMain.config.getHasBeenClonedField(), Type.BOOLEAN_TYPE.getDescriptor());
			super.visitJumpInsn(IFNE, lblbForReadThrough);
			dup();
			super.visitFieldInsn(GETFIELD, owner, InvivoPreMain.config.getChildField(), Type.INT_TYPE.getDescriptor());
			super.visitJumpInsn(IFEQ, lblbForReadThrough);
			
			dup();
			super.visitFieldInsn(opcode, owner, name, desc);
			loadThis();
			visitMethodInsn(INVOKESTATIC, "edu/columbia/cs/psl/invivo/runtime/COWAInterceptor", "readAndCOAIfNecessary",
					"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
			checkCast(Type.getType(desc));
			visitJumpInsn(GOTO, lblForNextInsn);	

			super.visitLabel(lblbForReadThrough);
			
			super.visitFieldInsn(opcode, owner, name, desc);			
			super.visitLabel(lblForNextInsn);
		}
		else if (opcode == GETSTATIC && !(owner.startsWith("java") || owner.startsWith("sun"))) {
			visitLdcInsn(owner);
			visitLdcInsn(name);
			visitLdcInsn(desc);
			visitMethodInsn(INVOKESTATIC, "edu/columbia/cs/psl/invivo/runtime/StaticWrapper", "getValue",
					"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
			if(Type.getType(desc).getSort() != Type.OBJECT)
				unbox(Type.getType(desc));
			else
				checkCast(Type.getType(desc));
		} else if (opcode == PUTSTATIC) {
			//here should be the value we want to set to
			if(Type.getType(desc).getSort() != Type.OBJECT)
				box(Type.getType(desc));
			visitLdcInsn(owner);
			visitLdcInsn(name);
			visitLdcInsn(desc);
			visitMethodInsn(INVOKESTATIC, "edu/columbia/cs/psl/invivo/runtime/StaticWrapper", "setValue",
					"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V");
		}
		// else do the standard dynamic lookup
		else super.visitFieldInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack, maxLocals);
	}

	private String className;
	public void setClassName(String className) {
		this.className = className;
	}
	private InterceptingClassVisitor classVisitor;
	public void setClassVisitor(
			InterceptingClassVisitor interceptingClassVisitor) {
		classVisitor = interceptingClassVisitor;
	}

}
