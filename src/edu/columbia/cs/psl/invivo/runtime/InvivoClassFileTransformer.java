package edu.columbia.cs.psl.invivo.runtime;


import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import edu.columbia.cs.psl.invivo.runtime.visitor.InterceptingClassVisitor;


public class InvivoClassFileTransformer implements ClassFileTransformer {
	
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		String name = className.replace("/", ".");
		if(!name.startsWith("java"))
		{			
			ClassReader cr = new ClassReader(classfileBuffer);
			  ClassWriter cw = new ClassWriter(cr,
		     ClassWriter.COMPUTE_MAXS |
		ClassWriter.COMPUTE_FRAMES);
			  InterceptingClassVisitor cv = new InterceptingClassVisitor(cw);
			  cv.setClassName(name);
			  cr.accept(cv, ClassReader.EXPAND_FRAMES);
//			  try{
//				  FileOutputStream fos = new FileOutputStream(name+"_debug.class");
//				  ByteArrayOutputStream bos = new ByteArrayOutputStream(cw.toByteArray().length);
//				  bos.write(cw.toByteArray());
//				  bos.writeTo(fos);
//				  fos.close();
//			  }
//			  catch(Exception ex)
//			  {
//				  ex.printStackTrace();
//			  }
			  return cw.toByteArray();
		}
		return classfileBuffer;
	}

}
