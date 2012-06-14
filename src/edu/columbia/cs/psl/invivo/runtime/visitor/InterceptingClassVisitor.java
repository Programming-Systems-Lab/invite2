package edu.columbia.cs.psl.invivo.runtime.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;
import edu.columbia.cs.psl.invivo.runtime.NotInstrumented;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoClassDesc;
@NotInstrumented
public class InterceptingClassVisitor extends ClassVisitor implements Opcodes {
	
	private String className;

	private boolean isAClass = true;
	
	private boolean runIMV = true;
	
	private boolean willRewrite = false;
	
	private InVivoClassDesc cls;
	
	public InterceptingClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
	}
	
	public InterceptingClassVisitor(ClassVisitor cv, InVivoClassDesc cls) {
		super(Opcodes.ASM4, cv);
		this.setCls(cls);
	}
	
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
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if(desc.equals(Type.getDescriptor(InvivoPreMain.config.getProtectorAnnotation())))
			runIMV = false;
		return super.visitAnnotation(desc, visible);
	}
	
	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc,
			String signature, String[] exceptions) {

		if(runIMV && isAClass)
		{
			MethodVisitor mv = cv.visitMethod(acc, name, desc, signature,
					exceptions);
			LazyCloneInterceptingMethodVisitor cloningIMV = new LazyCloneInterceptingMethodVisitor(Opcodes.ASM4, mv, acc, name, desc);
			InterceptingMethodVisitor imv = new InterceptingMethodVisitor(Opcodes.ASM4, cloningIMV, acc, name, desc);
			imv.setClassName(className);
			imv.setClassVisitor(this);
			return imv;
		}
		else
			return 	cv.visitMethod(acc, name, desc, signature,
					exceptions);
	}

	
	//Default to true to make it work for all classes
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
			
		}
	}
	
	public void setClassName(String name) {
		this.className = name;
	}
	
	public String getClassName() {
		return this.className;
	}

	public InVivoClassDesc getCls() {
		return cls;
	}

	public void setCls(InVivoClassDesc cls) {
		this.cls = cls;
	}
}
