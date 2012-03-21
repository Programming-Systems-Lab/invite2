package edu.columbia.cs.psl.invivo.runtime;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;


public class InvivoPreMain {
	public static AbstractConfiguration config;
public static void premain(String args, Instrumentation inst, AbstractConfiguration config) {
		InvivoPreMain.config = config;
		ClassFileTransformer transformer = 
								new InvivoClassFileTransformer();
		inst.addTransformer(transformer);
	}
}
