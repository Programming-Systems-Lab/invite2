package edu.columbia.cs.psl.invivo.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.rits.cloning.Cloner;

/**
 * 
 * @author jon
 * 
 */
@NotInstrumented
public class COWAInterceptor {
	private static Logger logger = Logger.getLogger(COWAInterceptor.class);
	private static Cloner deepCloner = new Cloner();

	public static Object readAndCOAIfNecessary(Object theOwner, Object theFieldValue, Object callee) {
		System.out.println("You called read");
		System.out.println(theOwner + "; " + callee + "; " + theFieldValue);

		Object r = doCopy(AbstractInterceptor.getRootCallee(), theFieldValue);
		int childNum = AbstractInterceptor.getThreadChildId();
		
		synchronized (pointsTo) {
			if(!pointsTo.containsKey(childNum))
				pointsTo.put(childNum, new IdentityHashMap<Object, Object>());
		}
		IdentityHashMap<Object, Object> myPointsTo = pointsTo.get(childNum);
		
		final List<Field> fields = allFields(r.getClass());
		for (final Field field : fields) {
			final int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers)) {
				try {
					final Object fieldObject = field.get(r);
					synchronized (myPointsTo) {
						if(myPointsTo.containsKey(fieldObject))
							field.set(r, myPointsTo.get(fieldObject));						
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		
		myPointsTo.put(theFieldValue, r);
		return r;
	}

	private static void setIsAClonedObject(Object obj) {
		try {
			obj.getClass().getField(InvivoPreMain.config.getHasBeenClonedField()).setBoolean(obj, true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static boolean isAClonedObject(Object obj) {
		try {
			return obj.getClass().getField(InvivoPreMain.config.getHasBeenClonedField()).getBoolean(obj);
		} catch (Exception ex) {
			return false;
		}
	}

	public static void setAndCOWIfNecessary(Object theOwner, Object value, Object callee, String owner, String name, String desc) {

	}

	/**
	 * Will perform a minimal deep clone of the object graph started at root
	 * such that leaf is cloned.
	 * 
	 * @param root
	 * @param leaf
	 */
	public static Object doCopy(Object root, Object leaf) {
		Object newLeaf = deepCloner.shallowClone(leaf);

		setIsAClonedObject(leaf);
		traverseGraphAndReplace(root, leaf, newLeaf);
		return newLeaf;
	}

	private static List<Field> allFields(final Class<?> c) {
		List<Field> l = fieldsCache.get(c);
		if (l == null) {
			l = new LinkedList<Field>();
			final Field[] fields = c.getDeclaredFields();
			addAll(l, fields);
			Class<?> sc = c;
			while ((sc = sc.getSuperclass()) != Object.class && sc != null) {
				addAll(l, sc.getDeclaredFields());
			}
			fieldsCache.putIfAbsent(c, l);
		}
		return l;
	}
	private final static ConcurrentHashMap<Integer,IdentityHashMap<Object, Object>> pointsTo = new ConcurrentHashMap<Integer, IdentityHashMap<Object,Object>>();
	private final static ConcurrentHashMap<Class<?>, List<Field>> fieldsCache = new ConcurrentHashMap<Class<?>, List<Field>>();

	private static void addAll(final List<Field> l, final Field[] fields) {
		for (final Field field : fields) {
			if (!field.isAccessible())
				field.setAccessible(true);
			l.add(field);
		}
	}

	private static void traverseGraphAndReplace(Object root, Object leaf, Object newLeaf) {
		if (root == null)
			return;
		if (!isAClonedObject(root))
			return;
		Class<?> clz = root.getClass();
		if (clz.isArray()) {
			final int length = Array.getLength(root);
			for (int i = 0; i < length; i++) {
				final Object v = Array.get(root, i);
				if (isAClonedObject(v)) {
					if (v == leaf)
						Array.set(root, i, newLeaf);
					else
						traverseGraphAndReplace(v, leaf, newLeaf);
				}
			}
		}
		final List<Field> fields = allFields(clz);
		for (final Field field : fields) {
			final int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers)) {
				try {
					final Object fieldObject = field.get(root);
					if (isAClonedObject(fieldObject)) {
						if (fieldObject == leaf)
							field.set(root, newLeaf);
						else
							traverseGraphAndReplace(fieldObject, leaf, newLeaf);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
