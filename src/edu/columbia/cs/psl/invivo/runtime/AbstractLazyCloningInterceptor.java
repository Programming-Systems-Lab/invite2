package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import edu.columbia.cs.psl.invivo.struct.MethodInvocation;

public abstract class AbstractLazyCloningInterceptor extends AbstractInterceptor {

	public AbstractLazyCloningInterceptor(Interceptable intercepted) {
		super(intercepted);
	}
	
	
}
