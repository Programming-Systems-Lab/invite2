package edu.columbia.cs.psl.invivo.runtime.visitor;

import org.objectweb.asm.ClassVisitor;

public abstract class BuddyClassVisitor<T extends ClassVisitor> extends ClassVisitor {

	public BuddyClassVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	private T buddy;
	@SuppressWarnings("unchecked")
	public void setBuddy(Object buddy) {
		this.buddy = (T) buddy;
	}
	public T getBuddy() {
		return buddy;
	}
}
