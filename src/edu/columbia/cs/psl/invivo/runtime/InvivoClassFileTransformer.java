package edu.columbia.cs.psl.invivo.runtime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.invivo.runtime.visitor.InterceptingClassVisitor;

@NotInstrumented
public class InvivoClassFileTransformer implements ClassFileTransformer {
	private static Logger logger = Logger.getLogger(InvivoClassFileTransformer.class);
	public static ClassNode currentClassNode;

	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		String name = className.replace("/", ".");
		if (!name.startsWith("java")) {
			boolean found = false;
			currentClassNode = new ClassNode();
			ClassReader cncr = new ClassReader(classfileBuffer);
			cncr.accept(currentClassNode, ClassReader.SKIP_CODE);
			for (Object o : currentClassNode.methods) {
				MethodNode mn = (MethodNode) o;
				if (mn.visibleAnnotations != null)
					for (Object oa : mn.visibleAnnotations) {
						AnnotationNode an = (AnnotationNode) oa;
						if (an.desc.equals(InvivoPreMain.config.getAnnotationDescriptor()))
							found = true;
					}
			}
			if (!found) {
				return classfileBuffer;
			}
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			try {
				InterceptingClassVisitor cv;
				cv = new InterceptingClassVisitor(cw);
				cv.setClassName(name);
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
				File f = new File("debug/");
				if (!f.exists())
					f.mkdir();
				FileOutputStream fos = new FileOutputStream("debug/" + name + ".class");
				ByteArrayOutputStream bos = new ByteArrayOutputStream(cw.toByteArray().length);
				bos.write(cw.toByteArray());
				bos.writeTo(fos);
				fos.close();

			} catch (Throwable ex) {
				logger.error("Error generating modified class " + name, ex);
			}
			return cw.toByteArray();
		}
		return classfileBuffer;
	}

}
