package edu.columbia.cs.psl.invivo.runtime.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

import edu.columbia.cs.psl.invivo.runtime.InvivoPreMain;

public class AnnotationBrowsingMethodVisitor extends MethodVisitor {

	private String name;
	private String desc;
	public AnnotationBrowsingMethodVisitor(int api, String name, String desc, MethodVisitor mv) {
		super(api, mv);
		this.name= name;
		this.desc= desc;
	}
	public String getNameDesc() {
		return name+desc;
	}
	private boolean hasAnnotation;
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if(desc.equals(InvivoPreMain.config.getAnnotationDescriptor()))
		{
			hasAnnotation = true;
		}
		return super.visitAnnotation(desc, visible);
	}
	public boolean isHasAnnotation() {
		return hasAnnotation;
	}
}
