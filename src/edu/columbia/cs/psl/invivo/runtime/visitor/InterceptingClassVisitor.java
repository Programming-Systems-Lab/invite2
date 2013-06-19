package edu.columbia.cs.psl.invivo.runtime.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.invivo.runtime.AbstractInterceptor;
import edu.columbia.cs.psl.invivo.runtime.InvivoClassFileTransformer;
import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;
import edu.columbia.cs.psl.invivo.runtime.NotInstrumented;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoAsmEval;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoClassDesc;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoIdentifierDesc;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoMethodDesc;
import edu.columbia.psl.invivoexpreval.asmeval.InVivoVariableReplacement;

@NotInstrumented
public class InterceptingClassVisitor extends ClassVisitor implements Opcodes {

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
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		if ((access & Opcodes.ACC_INTERFACE) != 0)
			isAClass = false;
		clsDesc.setClassName(name);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		if (desc.length() > 1)
			super.visitField(Opcodes.ACC_PUBLIC,
					name + InvivoPreMain.config.getHasBeenClonedField(),
					Type.BOOLEAN_TYPE.getDescriptor(), null, false);
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
		if (desc.equals(Type.getDescriptor(InvivoPreMain.config
				.getProtectorAnnotation())))
			runIMV = false;
		return super.visitAnnotation(desc, visible);
	}

	@Override
	public MethodVisitor visitMethod(int acc, String name, String desc,
			String signature, String[] exceptions) {

		if (runIMV && isAClass) {
			MethodVisitor mv = cv.visitMethod(acc, name, desc, signature,
					exceptions);
			InterceptingMethodVisitor imv = new InterceptingMethodVisitor(
					Opcodes.ASM4, mv, acc, name, desc);
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

	private HashMap<MethodNode,ArrayList<LocalVariableNode>> testedMethods = new HashMap<MethodNode,ArrayList<LocalVariableNode>>();

	private MethodNode findRealMethodNode(MethodNode in)
	{
		for(Object o: InvivoClassFileTransformer.currentClassNode.methods)
		{
			MethodNode mn = (MethodNode) o;
			if(mn.name.equals(in.name) && mn.desc.equals(in.desc))
				return mn;
		}
		return null;
	}
	public void addTestedMethod(MethodNode mn, ArrayList<LocalVariableNode> lvs) {
		testedMethods.put(findRealMethodNode(mn),lvs);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		if (willRewrite) {
			FieldNode fn = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PRIVATE,
					InvivoPreMain.config.getInterceptorFieldName(),
					Type.getDescriptor(InvivoPreMain.config
							.getInterceptorClass()), null, null);
			fn.accept(cv);

			FieldNode fn3 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PRIVATE
					+ Opcodes.ACC_STATIC,
					InvivoPreMain.config.getStaticInterceptorFieldName(),
					Type.getDescriptor(InvivoPreMain.config
							.getInterceptorClass()), null, null);
			fn3.accept(cv);
		}
		if (isAClass) {
			FieldNode fn2 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PUBLIC,
					InvivoPreMain.config.getChildField(),
					Type.INT_TYPE.getDescriptor(), null, 0);
			fn2.accept(cv);
			FieldNode fn4 = new FieldNode(Opcodes.ASM4, Opcodes.ACC_PUBLIC,
					InvivoPreMain.config.getHasBeenClonedField(),
					Type.BOOLEAN_TYPE.getDescriptor(), null, false);
			fn4.accept(cv);
		}
		try {
			for (MethodNode mn : testedMethods.keySet()) {
				int nTests = 0;
				for(Object o : mn.visibleAnnotations)
				{
					AnnotationNode an = (AnnotationNode) o;
					if(an.desc.equals("Ledu/columbia/cs/psl/metamorphic/runtime/annotation/Metamorphic;"))
					{
						List<AnnotationNode> rules = (List) an.values.get(1);
						for(AnnotationNode a1 : rules)
						{
							String test = null;
							String check = null;
							String checkMethod = "==";
							for(int i = 0; i < a1.values.size(); i++)
							{
								if(a1.values.get(i).equals("test"))
									test = (String) a1.values.get(i+1);
								else if(a1.values.get(i).equals("check"))
									check = (String) a1.values.get(i+1);
								else if(a1.values.get(i).equals("checkMethod"))
									checkMethod = (String) a1.values.get(i+1);
								i++;
							}
							System.out.println("Test: <"+test+"> check: <" + check+"> method:<"+checkMethod+">");
							{
								MethodVisitor mv = this.visitMethod(mn.access, mn.name
										+ "_test"+nTests, mn.desc, null, null);
								GeneratorAdapter gmv = new GeneratorAdapter(mv, mn.access,
										mn.name + "_test"+nTests, mn.desc);

								gmv.visitCode();
								Label start = new Label();
								gmv.visitLabel(start);

								
								InVivoAsmEval eval = new InVivoAsmEval();
								Pattern p = Pattern.compile("\\\\([^(]+)\\(");
								Matcher m = p.matcher(test);
								test = m.replaceAll("new edu.columbia.cs.psl.metamorphic.inputProcessor.impl.$1().apply((java.lang.Object)");
								System.out.println(test);
								eval.emitMetamorphicTest(test,gmv, mn.access, mn.name+ "_test"+nTests, mn.desc,clsDesc,testedMethods.get(mn));
								Label end = new Label();
								gmv.visitLabel(end);

								gmv.returnValue();
								gmv.visitMaxs(0, 0);
								for(LocalVariableNode lv : testedMethods.get(mn))
									if(!lv.name.equals("this"))
									gmv.visitLocalVariable(lv.name, lv.desc, null, start, end, lv.index);

								gmv.visitEnd();
								
							}
							{
								String returnType = Type.getReturnType(mn.desc).getDescriptor();
								String desc = "("+returnType+returnType;
								for(Type t : Type.getArgumentTypes(mn.desc))
								{
									desc += t.getDescriptor();
								}
								desc += ")Z";
								MethodVisitor mv = this.visitMethod(mn.access, mn.name
										+ "_check"+nTests, desc, null, null);
								GeneratorAdapter gmv = new GeneratorAdapter(mv, mn.access,
										mn.name + "_check"+nTests, desc);

								gmv.visitCode();
								Label start = new Label();
								gmv.visitLabel(start);
								InVivoAsmEval eval = new InVivoAsmEval();
								check = formatRuleCheck(check,checkMethod,Type.getReturnType(mn.desc));
								System.out.println(check);
								ArrayList<LocalVariableNode> checkArgs = new ArrayList<>();
								int offset = 0;
								if((mn.access  & Opcodes.ACC_STATIC) == 0)
								{
									checkArgs.add(new LocalVariableNode("this", className.replace(".", "/"), null, null, null, 0));
									offset++;
								}
								checkArgs.add(new LocalVariableNode("orig", returnType, null, null, null, 0+offset));
								checkArgs.add(new LocalVariableNode("metamorphic", returnType, null, null, null, 1+offset));
								for(LocalVariableNode ln : testedMethods.get(mn))
								{
									if(!ln.name.equals("this"))
									checkArgs.add(new LocalVariableNode(ln.name, ln.desc, ln.signature, ln.start, ln.end, ln.index+2));
								}
								eval.emitMetamorphicTest(check,gmv, mn.access, mn.name+ "_check"+nTests, desc,clsDesc,checkArgs);
								Label end = new Label();
								gmv.visitLabel(end);
								gmv.returnValue();
								gmv.visitMaxs(0, 0);
								for(LocalVariableNode lv : checkArgs)
									if(!lv.name.equals("this"))
									gmv.visitLocalVariable(lv.name, lv.desc, null, start, end, lv.index);
								gmv.visitEnd();
								
							}
							nTests++;
						}
					}

				}
				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	private static String getBoxedType(Type type) {
        if(type.getDescriptor().equals("I"))
            return "java/lang/Integer";
        else if(type.getDescriptor().equals("B"))
            return "java/lang/Byte";
        else if(type.getDescriptor().equals("Z"))
            return "java/lang/Boolean";
        else if(type.getDescriptor().equals("S"))
            return "java/lang/Short";
        else if(type.getDescriptor().equals("C"))
            return "java/lang/Char";
        else if(type.getDescriptor().equals("F"))
            return "java/lang/Float";
        else if(type.getDescriptor().equals("J"))
            return "java/lang/Long";
        else if(type.getDescriptor().equals("D"))
            return "java/lang/Double";
        return null;
    }

	private String formatRuleCheck(String checkStr, String checkMethod, Type returnType) throws Exception {

		Pattern p = Pattern.compile("\\\\([^(]+)\\(");
		Matcher m = p.matcher(checkStr);
		boolean returnIsPrimitive = returnType.getDescriptor().length() == 1 ;
		if(m.find())
		{
			checkStr = (returnIsPrimitive ? getBoxedType(returnType) + ".valueOf((" + getBoxedType(returnType) + ")" : "" ) + 
					m.replaceAll("new edu.columbia.cs.psl.metamorphic.inputProcessor.impl.$1().apply((Object) " );
			if(returnIsPrimitive)
				checkStr =  checkStr + ")";
			m.reset(checkStr);
		}
		
		checkStr = checkStr.replace("\\result", "orig");
		if (checkMethod.equals("==") || checkMethod.equals(">=") || checkMethod.equals("<=") || checkMethod.equals("<")
				|| checkMethod.equals(">") || checkMethod.equals("!=")) {
			if (returnIsPrimitive)
				if(returnType.getSort() == Type.DOUBLE && checkMethod.equals("=="))
					checkStr = "edu.columbia.cs.psl.metamorphic.outputRelation.impl.ApproximatelyEqualTo.applies(metamorphic,"+ checkStr + ",0.000001)";
				else
					checkStr = "metamorphic " + checkMethod + " " + checkStr;
			else if(returnType.getSort() == Type.ARRAY)
			{
				if (checkMethod.equals("!="))
					checkStr = "! java.util.Arrays.equals(metamorphic, (" + returnType.toString() + ")" + checkStr + ")";
				else if (checkMethod.equals("=="))
					checkStr = "java.util.Arrays.equals(metamorphic,(" + returnType.toString() + ")" + checkStr + ")";
				else {
						checkStr = "metamorphic.compareTo(" + checkStr + ") " + checkMethod + " 0";
				}
			}
			else {
				if (checkMethod.equals("!="))
					checkStr = "! metamorphic.equals(" + checkStr + ")";
				else if (checkMethod.equals("=="))
					checkStr = "metamorphic.equals(" + checkStr + ")";
				else {
						checkStr = "metamorphic.compareTo(" + checkStr + ") " + checkMethod + " 0";
				}
			}
		}
		
		return checkStr;
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
