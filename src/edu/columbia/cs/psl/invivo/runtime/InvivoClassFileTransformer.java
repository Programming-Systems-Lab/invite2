package edu.columbia.cs.psl.invivo.runtime;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import edu.columbia.cs.psl.invivo.runtime.visitor.BuddyClassVisitor;
import edu.columbia.cs.psl.invivo.runtime.visitor.InterceptingClassVisitor;

@NotInstrumented
public class InvivoClassFileTransformer implements ClassFileTransformer {

	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {
		String name = className.replace("/", ".");
		if (!name.startsWith("java")) {
			ClassVisitor preVisitor = InvivoPreMain.config.getPreCV(Opcodes.ASM4, null);
			if (preVisitor != null) {
				ClassReader initialVisitor = new ClassReader(classfileBuffer);
				initialVisitor.accept(preVisitor,0);
			}

			ClassReader cr = new ClassReader(classfileBuffer);
			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			try {
			InterceptingClassVisitor cv = new InterceptingClassVisitor(cw);
			cv.setClassName(name);
			ClassVisitor secondaryVistor = InvivoPreMain.config.getAdditionalCV(Opcodes.ASM4, cv);
			if(secondaryVistor != null)
			{
				if(secondaryVistor instanceof BuddyClassVisitor<?>)
					((BuddyClassVisitor<?>) secondaryVistor).setBuddy(preVisitor);
				cr.accept(secondaryVistor, ClassReader.EXPAND_FRAMES);
			}
			else
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
			
				FileOutputStream fos = new FileOutputStream("debug/" + name + ".class");
				ByteArrayOutputStream bos = new ByteArrayOutputStream(cw.toByteArray().length);
				bos.write(cw.toByteArray());
				bos.writeTo(fos);
				fos.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return cw.toByteArray();
		}
		return classfileBuffer;
	}

}
