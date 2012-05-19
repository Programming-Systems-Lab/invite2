package edu.columbia.cs.psl.invivo.runtime.visitor;

import org.objectweb.asm.ClassVisitor;

public abstract class BuddyClassVisitor<T extends ClassVisitor> extends ClassVisitor {

	public BuddyClassVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	private T buddy;
	
	public void setBuddy(Object preVisitor) {
		this.buddy =  (T) preVisitor;
	}
	
	public T getBuddy() {
		return buddy;
	}

	public void setBuddy(ClassVisitor preVisitor) {
		// TODO Auto-generated method stub
		this.buddy = (T) preVisitor;
	}
}
