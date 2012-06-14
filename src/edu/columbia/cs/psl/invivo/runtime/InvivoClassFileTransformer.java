package edu.columbia.cs.psl.invivo.runtime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import edu.columbia.cs.psl.invivo.runtime.visitor.BuddyClassVisitor;
import edu.columbia.cs.psl.invivo.runtime.visitor.InterceptingClassVisitor;

@NotInstrumented
public class InvivoClassFileTransformer implements ClassFileTransformer {
	private static Logger	logger	= Logger.getLogger(InvivoClassFileTransformer.class);

	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		String name = className.replace("/", ".");
		if (!name.startsWith("java")) {
			ClassVisitor preVisitor = InvivoPreMain.config.getPreCV(Opcodes.ASM4, null);
			if (preVisitor != null) {
				try {
					ClassReader initialVisitor = new ClassReader(classfileBuffer);
					initialVisitor.accept(preVisitor, 0);
				} catch (Exception ex) {
					logger.error("Error running initial visitor for class " + name, ex);
				}
			}

			TestRunnerGenerator<ClassVisitor> generator = InvivoPreMain.config.getTestRunnerGenerator(preVisitor);
			
			// TODO Now put the test generator in the ClassInterceptingVisitor and use it in the onEnter method of the MethodInterceptingVisitor
			// TODO Once we are done with that, call the test method after loading up the replacement variables.
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			try {
				InterceptingClassVisitor cv;
				if (generator != null) {
					cv = new InterceptingClassVisitor(cw, generator.getClsDesc());
				} else
					cv = new InterceptingClassVisitor(cw);
				cv.setClassName(name);
				ClassVisitor secondaryVistor = InvivoPreMain.config.getAdditionalCV(Opcodes.ASM4, cv);
				if (secondaryVistor != null) {
					if (secondaryVistor instanceof BuddyClassVisitor<?>)
						((BuddyClassVisitor<?>) secondaryVistor).setBuddy(preVisitor);
					cr.accept(secondaryVistor, ClassReader.EXPAND_FRAMES);
				} else {
					cr.accept(cv, ClassReader.EXPAND_FRAMES);
				}
				File f = new File("debug/");
				if (!f.exists())
					f.mkdir();
				FileOutputStream fos = new FileOutputStream("debug/" + name + ".class");
				ByteArrayOutputStream bos = new ByteArrayOutputStream(cw.toByteArray().length);
				bos.write(cw.toByteArray());
				bos.writeTo(fos);
				fos.close();

			} catch (Exception ex) {
				logger.error("Error generating modified class " + name, ex);
			}
			return cw.toByteArray();
		}
		return classfileBuffer;
	}

}
