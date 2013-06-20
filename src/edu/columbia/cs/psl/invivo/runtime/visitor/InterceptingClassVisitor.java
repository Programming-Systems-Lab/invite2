package edu.columbia.cs.psl.invivo.runtime.visitor;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.invivo.runtime.InvivoClassFileTransformer;
import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;
import edu.columbia.cs.psl.invivo.runtime.NotInstrumented;
import edu.columbia.cs.psl.invivo.runtime.TestRunnerGenerator;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoClassDesc;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoIdentifierDesc;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoMethodDesc;

@NotInstrumented
public class InterceptingClassVisitor extends ClassVisitor implements Opcodes {
	private static final Logger logger = Logger.getLogger(InterceptingClassVisitor.class);

	private String className;

	private boolean isAClass = true;

	private boolean runIMV = true;

	private boolean willRewrite = false;

	private InVivoClassDesc clsDesc = new InVivoClassDesc();

	public InterceptingClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
	}

	public InterceptingClassVisitor(ClassVisitor cv, InVivoClassDesc cls) {
		super(Opcodes.ASM4, cv);
		this.setCls(cls);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

		if ((access & Opcodes.ACC_INTERFACE) != 0)
			isAClass = false;
		clsDesc.setClassName(name);
		if (isAClass) {
			String[] tmp = new String[interfaces.length + 1];
			System.arraycopy(interfaces, 0, tmp, 0, interfaces.length);
			interfaces = tmp;
			interfaces[interfaces.length - 1] = "edu/columbia/cs/psl/invivo/runtime/Interceptable";
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (desc.length() > 1)
			super.visitField(Opcodes.ACC_PUBLIC, name + InvivoPreMain.config.getHasBeenClonedField(), Type.BOOLEAN_TYPE.getDescriptor(), null, false);
		InVivoIdentifierDesc d = new InVivoIdentifierDesc();
		d.setAccess(access);
		d.setClassName(className);
		d.setSignature(signature);
		d.setDesc(desc);
		d.setName(name);
		clsDesc.getClassFields().add(d);
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(Type.getDescriptor(InvivoPreMain.config.getProtectorAnnotation())))
			runIMV = false;
		return super.visitAnnotation(desc, visible);
	}

	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc, String signature, String[] exceptions) {

		if (runIMV && isAClass) {
			MethodVisitor mv = cv.visitMethod(acc, name, desc, signature, exceptions);
			InterceptingMethodVisitor imv = new InterceptingMethodVisitor(Opcodes.ASM4, mv, acc, name, desc);
			// InterceptingMethodVisitor imv = new
			// InterceptingMethodVisitor(Opcodes.ASM4, cloningIMV, acc, name,
			// desc);
			imv.setClassName(className);
			imv.setClassVisitor(this);
			InVivoMethodDesc d = new InVivoMethodDesc();
			d.setMethodAcc(acc);
			d.setMethodDesc(desc);
			d.setMethodName(name);
			clsDesc.addMethod(d, null);
			//			if (this.clsDesc != null) {
			//				Entry<InVivoMethodDesc, List<InVivoVariableReplacement>> mDesc = clsDesc
			//						.getClassMethod(name, desc);
			//				if (mDesc != null)
			//					imv.setmDesc(mDesc);
			//			}
			return imv;
		} else
			return cv.visitMethod(acc, name, desc, signature, exceptions);
	}

	// Default to true to make it work for all classes
	public void setShouldRewrite() {
		willRewrite = true;
	}

	private HashMap<MethodNode, ArrayList<LocalVariableNode>> testedMethods = new HashMap<MethodNode, ArrayList<LocalVariableNode>>();

	private MethodNode findRealMethodNode(MethodNode in) {
		for (Object o : InvivoClassFileTransformer.currentClassNode.methods) {
			MethodNode mn = (MethodNode) o;
			if (mn.name.equals(in.name) && mn.desc.equals(in.desc))
				return mn;
		}
		return null;
	}

	private ArrayList<MethodNode> mnsInOrder = new ArrayList<>();

	public InVivoClassDesc getClsDesc() {
		return clsDesc;
	}

	public ArrayList<MethodNode> getMnsInOrder() {
		return mnsInOrder;
	}

	public HashMap<MethodNode, ArrayList<LocalVariableNode>> getTestedMethods() {
		return testedMethods;
	}

	public int getInterceptorIdx(MethodNode mn) {
		MethodNode realMN = findRealMethodNode(mn);
		mnsInOrder.add(realMN);
		return mnsInOrder.size() - 1;

	}

	public void addTestedMethod(MethodNode mn, ArrayList<LocalVariableNode> lvs) {
		MethodNode realMN = findRealMethodNode(mn);
		testedMethods.put(realMN, lvs);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		if (willRewrite) {
			FieldNode fn = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PRIVATE, InvivoPreMain.config.getInterceptorFieldName(), Type.getDescriptor(InvivoPreMain.config.getInterceptorClass()), null, null);
			fn.accept(cv);

			FieldNode fn3 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, InvivoPreMain.config.getStaticInterceptorFieldName(), Type.getDescriptor(InvivoPreMain.config
					.getInterceptorClass()), null, null);
			fn3.accept(cv);
		}
		if (isAClass) {
			FieldNode fn2 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PUBLIC, InvivoPreMain.config.getChildField(), Type.INT_TYPE.getDescriptor(), null, 0);
			fn2.accept(cv);
			FieldNode fn4 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PUBLIC, InvivoPreMain.config.getHasBeenClonedField(), Type.BOOLEAN_TYPE.getDescriptor(), null, false);
			fn4.accept(cv);
		}
		TestRunnerGenerator tr = InvivoPreMain.config.getTestRunnerGenerator(this);
		if (tr != null)
			tr.visitEnd();
	}

	public void setClassName(String name) {
		this.className = name;
	}

	public String getClassName() {
		return this.className;
	}

	public InVivoClassDesc getCls() {
		return clsDesc;
	}

	public void setCls(InVivoClassDesc cls) {
		this.clsDesc = cls;
	}
}
