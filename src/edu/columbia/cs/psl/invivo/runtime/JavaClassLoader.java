package edu.columbia.cs.psl.invivo.runtime;

public class JavaClassLoader extends ClassLoader {

	public static Class<?> loadClass(byte args[], String name) throws Exception {
		JavaClassLoader _classLoader = new JavaClassLoader();
		byte[] rawBytes = new byte[args.length];
		
		for (int index = 0; index < rawBytes.length; index++)
			rawBytes[index] = (byte) args[index];
		
		Class<?> regeneratedClass = _classLoader.defineClass(name, rawBytes,
				0, rawBytes.length);
		
		return regeneratedClass;
	}
}
