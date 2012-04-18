package edu.columbia.cs.psl.invivo.runtime;

import java.lang.annotation.Annotation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.invivo.runtime.visitor.BuddyClassVisitor;

public abstract class AbstractConfiguration {
	public String getThreadPrefix()
	{
		return "__invivoChild_";
	}
	public String getInterceptedPrefix()
	{
		return "__original__";
	}
	public String getChildField()
	{
		return "__metamorphicChildCount";
	}
	public String getHasBeenClonedField()
	{
		return "__invivoCloned";
	}
	public abstract Class<? extends AbstractInterceptor> getInterceptorClass();
	
	public abstract Class<? extends Annotation> getAnnotationClass();
	
	public Class<? extends Annotation> getProtectorAnnotation()
	{
		return NotInstrumented.class;
	}
	public String getInterceptorFieldName()
	{
		return "___interceptor__by_mountaindew";
	}

	public String getStaticInterceptorFieldName()
	{
		return "___interceptor__by_mountaindew_static";
	}
	private String cachedInterceptorDescriptor;
	public String getInterceptorDescriptor()
	{
		if(cachedInterceptorDescriptor == null)
			cachedInterceptorDescriptor = Type.getDescriptor(getInterceptorClass());
		return cachedInterceptorDescriptor;
	}
	
	private String cachedAnnotationDescriptor;
	public String getAnnotationDescriptor()
	{
		if(cachedAnnotationDescriptor == null)
			cachedAnnotationDescriptor = Type.getDescriptor(getAnnotationClass());
		return cachedAnnotationDescriptor;
	}
	public ClassVisitor getPreCV(int api, ClassVisitor cv)
	{
		return null;
	}
	public ClassVisitor getAdditionalCV(int api, ClassVisitor cv)
	{
		return null;
	}
}
