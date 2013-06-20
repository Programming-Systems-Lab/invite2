package edu.columbia.cs.psl.invivo.runtime.visitor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.invivo.runtime.AbstractInterceptor;
import edu.columbia.cs.psl.invivo.runtime.Interceptable;
import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoAsmEval;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoMethodDesc;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoVariableReplacement;

public class InterceptingMethodVisitor extends AdviceAdapter {
	private String name;

	private Entry<InVivoMethodDesc, List<InVivoVariableReplacement>> mDesc;

	private Type[] argumentTypes;

	private int access;

	private int lastArgIdx = 0;
	
	MethodNode thisMN = new MethodNode();
	protected InterceptingMethodVisitor(int api, MethodVisitor mv, int access,
			String name, String desc) {
		super(api, mv, access, name, desc);
		this.name = name;
		this.access = access;
		this.argumentTypes = Type.getArgumentTypes(desc);
		thisMN.name = name;
		thisMN.access = access;
		thisMN.desc = desc;
		thisMN.visibleAnnotations = new LinkedList<AnnotationNode>();
		lastArgIdx = Type.getArgumentTypes(desc).length;
		if((access & Opcodes.ACC_STATIC) != 0) lastArgIdx++;
	}

	boolean rewrite = false;

	int interceptorIdx = 0;
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(InvivoPreMain.config.getAnnotationDescriptor())) {
			classVisitor.setShouldRewrite();
			rewrite = true;
			interceptorIdx = classVisitor.getInterceptorIdx(thisMN);
		}
		AnnotationNode an = new AnnotationNode(desc);
		thisMN.visibleAnnotations.add(an);
		return super.visitAnnotation(desc, visible);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		if (!rewrite) {
			super.visitFieldInsn(opcode, owner, name, desc);
			return;
		}
		if (opcode == GETSTATIC
				&& !(owner.startsWith("java") || owner.startsWith("sun"))) {
			visitLdcInsn(owner);
			visitLdcInsn(name);
			super.visitFieldInsn(opcode, owner, name, desc);
			visitLdcInsn(false);
			visitMethodInsn(INVOKESTATIC,
					"edu/columbia/cs/psl/invivo/runtime/StaticWrapper",
					"getValue",
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Z)Ljava/lang/Object;");
			if (Type.getType(desc).getSort() != Type.OBJECT)
				unbox(Type.getType(desc));
			else
				checkCast(Type.getType(desc));
		} else if (opcode == PUTSTATIC) {
			// here should be the value we want to set to
			if (Type.getType(desc).getSort() != Type.OBJECT)
				box(Type.getType(desc));
			visitLdcInsn(owner);
			visitLdcInsn(name);
			visitLdcInsn(desc);
			visitMethodInsn(INVOKESTATIC,
					"edu/columbia/cs/psl/invivo/runtime/StaticWrapper",
					"setValue",
					"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		}
		// else do the standard dynamic lookup
		else
			super.visitFieldInsn(opcode, owner, name, desc);
	}
	private ArrayList<LocalVariableNode> lvs = new ArrayList<>();
	@Override
	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		super.visitLocalVariable(name, desc, signature, start, end, index);
		if(index <= lastArgIdx)
			lvs.add(new LocalVariableNode(name, desc, signature, null, null, index));
	}
	private String getInterceptorName()
	{
		return InvivoPreMain.config.getInterceptorFieldName();//+ "_"+name+interceptorIdx;
	}
	private String getInterceptorInternalName()
	{
		return getInterceptorDescriptor().substring(1,getInterceptorDescriptor().length()-1);//className.replace(".", "/")+"$invivo"+this.name+this.interceptorIdx;
	}
	private String getInterceptorDescriptor()
	{
		return InvivoPreMain.config.getInterceptorDescriptor();
	}
	private void onStaticMethodEnter() {
		Label the_method = new Label();

		super.visitFieldInsn(GETSTATIC, className.replace(".", "/"),
				getInterceptorName(),
				getInterceptorDescriptor());
		super.visitJumpInsn(IFNONNULL, the_method);

		// Initialize a new interceptor with the class as the intercepted object
		visitTypeInsn(NEW, getInterceptorInternalName());
		dup();
		visitInsn(ACONST_NULL);
		visitMethodInsn(INVOKESPECIAL, getInterceptorInternalName(), "<init>",
				"("+Type.getDescriptor(Interceptable.class)+")V");
		super.visitFieldInsn(PUTSTATIC, className.replace(".", "/"),
				getInterceptorName(),
				getInterceptorDescriptor());

		visitLabel(the_method);

		refIdForInterceptor = newLocal(Type.INT_TYPE);

		super.visitFieldInsn(GETSTATIC, className.replace(".", "/"),
				getInterceptorName(),
				getInterceptorDescriptor());
//		visitLdcInsn(name);
		push(interceptorIdx);
		
		loadArgArray();
		visitInsn(ACONST_NULL);
		invokeVirtual(
				Type.getType(getInterceptorDescriptor()),
				Method.getMethod("int __onEnter (int, java.lang.Object[], edu.columbia.cs.psl.invivo.runtime.Interceptable)"));
		storeLocal(refIdForInterceptor);
		super.onMethodEnter();
	}

	Label mainMethodBody = new Label();
	Label continueExec = new Label();
	Label exceptionHandle = new Label();
	
	private void onMemberMethodEnter() {
		Label the_method = new Label();

//		evalReplacements();
		//TODO uncomment the above for the junit conversion stuff

		visitIntInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, className.replace(".", "/"),
				getInterceptorName(),
				getInterceptorDescriptor());
		super.visitJumpInsn(IFNONNULL, the_method);
		visitIntInsn(ALOAD, 0);
		visitTypeInsn(NEW, getInterceptorInternalName());
		dup();
		loadThis();
		visitMethodInsn(INVOKESPECIAL, getInterceptorInternalName(), "<init>",
				"("+Type.getDescriptor(Interceptable.class)+")V");
		super.visitFieldInsn(PUTFIELD, className.replace(".", "/"),
				getInterceptorName(),
				getInterceptorDescriptor());

		visitLabel(the_method);

		refIdForInterceptor = newLocal(Type.INT_TYPE);

		visitIntInsn(ALOAD, 0);
		super.visitFieldInsn(GETFIELD, className.replace(".", "/"),
				getInterceptorName(),
				getInterceptorDescriptor());
		push(interceptorIdx);
		loadArgArray();
		loadThis();
		invokeVirtual(
				Type.getType(AbstractInterceptor.class),
				Method.getMethod("int __onEnter (int, java.lang.Object[], edu.columbia.cs.psl.invivo.runtime.Interceptable)"));
		storeLocal(refIdForInterceptor);
		
		/*mv.visitTryCatchBlock(this.mainMethodBody, 
				this.continueExec, 
				this.exceptionHandle, 
				"java/lang/Exception");
		
		mv.visitLabel(this.mainMethodBody);*/
		
		super.onMethodEnter();
	}

	private void evalReplacements() {
		InVivoAsmEval eval = new InVivoAsmEval();
		eval.emitInterceptingInvivoInsn(this.getmDesc(), this.mv);
	}

	int refIdForInterceptor;

	@Override
	protected void onMethodEnter() {
		// This is from the old way of having the main thread continue
		super.onMethodEnter();
		if (!rewrite)
			return;
		if ((access & Opcodes.ACC_STATIC) != 0)
			onStaticMethodEnter();
		else
			onMemberMethodEnter();

	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack, maxLocals);
	}

	public void onMethodExit(int opcode) {
		super.onMethodExit(opcode);
		
		/*mv.visitLabel(continueExec);
		Label methodExit = new Label();
		mv.visitJumpInsn(GOTO, methodExit);
		
		mv.visitLabel(exceptionHandle);
		mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Exception"});
		mv.visitVarInsn(ASTORE, 1);
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		mv.visitLdcInsn("Exception caught!");
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
		
		mv.visitLabel(methodExit);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		*/
		if (!rewrite)
			return;

		visitFieldInsn(
				GETSTATIC,
				"edu/columbia/cs/psl/invivo/runtime/InterceptorExecutorService",
				"service",
				"Ledu/columbia/cs/psl/invivo/runtime/InterceptorExecutorService;");
		visitMethodInsn(INVOKESTATIC, 
				"java/lang/Thread", 
				"currentThread",
				"()Ljava/lang/Thread;");
		visitMethodInsn(INVOKEVIRTUAL, 
				"java/lang/Thread", 
				"getId", 
				"()J");
		visitMethodInsn(INVOKESTATIC, 
				"java/lang/Long", 
				"valueOf",
				"(J)Ljava/lang/Long;");
		visitMethodInsn(
				INVOKEVIRTUAL,
				"edu/columbia/cs/psl/invivo/runtime/InterceptorExecutorService",
				"remove", 
				"(Ljava/lang/Long;)V");

		if (opcode == RETURN) {
			visitInsn(ACONST_NULL);
		} else if (opcode == ARETURN || opcode == ATHROW) {
			dup();
		} else {
			if (opcode == LRETURN || opcode == DRETURN) {
				dup2();
			} else {
				dup();
			}
			box(Type.getReturnType(this.methodDesc));
		}
		if ((access & Opcodes.ACC_STATIC) != 0) {
			super.visitFieldInsn(GETSTATIC, className.replace(".", "/"),
					getInterceptorName(),
					getInterceptorDescriptor());
		} else {
			visitIntInsn(ALOAD, 0);
			super.visitFieldInsn(GETFIELD, className.replace(".", "/"),
					getInterceptorName(),
					getInterceptorDescriptor());
		}
		swap();
		visitIntInsn(SIPUSH, opcode);
		loadLocal(refIdForInterceptor);
		visitMethodInsn(INVOKEVIRTUAL, getInterceptorInternalName(), "__onExit",
				"(Ljava/lang/Object;II)V");
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

	public Entry<InVivoMethodDesc, List<InVivoVariableReplacement>> getmDesc() {
		return mDesc;
	}

	public void setmDesc(
			Entry<InVivoMethodDesc, List<InVivoVariableReplacement>> mDesc) {
		this.mDesc = mDesc;
	}
	@Override
	public void visitEnd() {
		super.visitEnd();
		if(rewrite)
			this.classVisitor.addTestedMethod(thisMN,lvs);
	}
}
