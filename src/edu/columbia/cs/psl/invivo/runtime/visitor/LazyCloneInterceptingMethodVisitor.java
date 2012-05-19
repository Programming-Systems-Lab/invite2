package edu.columbia.cs.psl.invivo.runtime.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;


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
			rewrite = true;
		}
		return super.visitAnnotation(desc, visible);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		if(!rewrite)
		{
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
		if(opcode == GETFIELD && desc.length() > 1 && !name.equals(InvivoPreMain.config.getInterceptorFieldName()))
		{	
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
		else super.visitFieldInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack, maxLocals);
	}

	

}
