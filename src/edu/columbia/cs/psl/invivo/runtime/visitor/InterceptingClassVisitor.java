package edu.columbia.cs.psl.invivo.runtime.visitor;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;
import edu.columbia.cs.psl.invivo.runtime.NotInstrumented;
@NotInstrumented
public class InterceptingClassVisitor extends ClassVisitor implements Opcodes {
	private Logger logger = Logger.getLogger(InterceptingClassVisitor.class);
	private String className;

	public InterceptingClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
	}
	private boolean isAClass = true;
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		if((access & Opcodes.ACC_INTERFACE) != 0)
			isAClass = false;
	}
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if(desc.length() > 1)
			super.visitField(Opcodes.ACC_PUBLIC, name+InvivoPreMain.config.getHasBeenClonedField(), Type.BOOLEAN_TYPE.getDescriptor(), null, false);
		
		return super.visitField(access, name, desc, signature, value);
	}
	private boolean runIMV = true;
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if(desc.equals(Type.getDescriptor(InvivoPreMain.config.getProtectorAnnotation())))
			runIMV = false;
		return super.visitAnnotation(desc, visible);
	}
	
	private boolean renaming = false;
	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc,
			String signature, String[] exceptions) {
		if(methodsWithAnnotations.contains(name+desc))
			renaming=true;
		if(renaming)
		{
			//Add a wrapper for the original method now.
			MethodVisitor mv = cv.visitMethod(acc, name , desc, signature,
					exceptions);
			GeneratorAdapter gv = new GeneratorAdapter(mv, acc, name, desc);
			generateSpringboard(gv, name, Type.getArgumentTypes(desc),Type.getReturnType(desc).getDescriptor(),desc);
			mv.visitEnd();
		}
		if(runIMV && isAClass)
		{
			MethodVisitor mv = cv.visitMethod(acc, (renaming ? InvivoPreMain.config.getInterceptedPrefix()+name : name), desc, signature,
					exceptions);
			LazyCloneInterceptingMethodVisitor imv = new LazyCloneInterceptingMethodVisitor(Opcodes.ASM4, mv, acc, (renaming ? InvivoPreMain.config.getInterceptedPrefix()+name : name), desc);
			imv.setClassName(className);
			imv.setClassVisitor(this);
			return imv;
		}
		else
			return 	cv.visitMethod(acc, name, desc, signature,
					exceptions);
	}
	
	private void generateSpringboard(GeneratorAdapter mv,String name,Type[] argumentTypes,String returnType,String desc)
	{
		Label the_method = new Label();
		Label exec_normal = new Label();
		mv.loadThis();
		mv.visitFieldInsn(GETFIELD, className.replace(".", "/"),InvivoPreMain.config.getChildField(),Type.INT_TYPE.getDescriptor());
		mv.visitJumpInsn(IFNE, exec_normal);
		
		mv.visitIntInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className.replace(".", "/"), InvivoPreMain.config.getInterceptorFieldName(), InvivoPreMain.config.getInterceptorDescriptor());
		mv.visitJumpInsn(IFNONNULL, the_method);
		mv.visitIntInsn(ALOAD, 0);
		mv.visitTypeInsn(NEW, InvivoPreMain.config.getInterceptorClass().getName().replace(".", "/"));
		mv.visitInsn(DUP);
		mv.visitIntInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, InvivoPreMain.config.getInterceptorClass().getName().replace(".", "/"), "<init>", "(Ljava/lang/Object;)V");
		mv.visitFieldInsn(PUTFIELD, className.replace(".", "/"), InvivoPreMain.config.getInterceptorFieldName(), InvivoPreMain.config.getInterceptorDescriptor());

		mv.visitLabel(the_method);
//		refIdForInterceptor = newLocal(Type.INT_TYPE);
		mv.visitIntInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className.replace(".", "/"), InvivoPreMain.config.getInterceptorFieldName(), InvivoPreMain.config.getInterceptorDescriptor());
		mv.visitLdcInsn(name);

		mv.push(argumentTypes.length);
		mv.newArray(Type.getType(String.class));
		for (int i = 0; i < argumentTypes.length; i++) {
			mv.visitInsn(DUP);
			mv.push(i);
		    if(argumentTypes[i].getSort() != Type.OBJECT && argumentTypes[i].getSort() != Type.ARRAY)
		    	mv.visitLdcInsn(argumentTypes[i].getClassName());
		    else
		    	mv.visitLdcInsn(argumentTypes[i].getInternalName().replace("/", "."));
		    mv.box(Type.getType(String.class));
		    mv.arrayStore(Type.getType(String.class));
		}
		
		mv.loadArgArray();
		mv.loadThis();
		mv.invokeVirtual(Type.getType(InvivoPreMain.config.getInterceptorClass()), Method.getMethod("java.lang.Object __onCall (java.lang.String, java.lang.String[], java.lang.Object[], java.lang.Object)"));
		if(returnType.length() > 1)
			mv.visitTypeInsn(Opcodes.CHECKCAST, returnType);
		else
			mv.unbox(Type.getType(returnType));

		mv.returnValue();
		
		mv.visitLabel(exec_normal);
		mv.loadThis();
		mv.loadArgs();
		mv.visitMethodInsn(INVOKEVIRTUAL, className.replace(".", "/"), name, desc);
		mv.returnValue();

		mv.visitMaxs(0, 0);
	}
	
	//Default to true to make it work for all classes
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
		}
		if(isAClass)
		{
			FieldNode fn2 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PUBLIC,
					InvivoPreMain.config.getChildField(),
					Type.INT_TYPE.getDescriptor(), null, 0); 
			fn2.accept(cv);
			FieldNode fn4 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PUBLIC,
					InvivoPreMain.config.getHasBeenClonedField(),
					Type.BOOLEAN_TYPE.getDescriptor(), null, false);
			fn4.accept(cv);
			
//			logger.info("Actually rewrote class: " + className);
		}
	}

	public void setClassName(String name) {
		this.className = name;
	}
	private HashSet<String> methodsWithAnnotations;
	public void setMethodsWithAnnotations(HashSet<String> methodsWithAnnotations) {
		this.methodsWithAnnotations= methodsWithAnnotations;
	}
}
