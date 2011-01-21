/******************************************************************** 
 * File          : PolyglotToJavaConverter.java
 * Project       : siggen
 * Description   : Convert Polyglot types to Java types
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:31:09 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */

package polyglot.ext.siggen.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import jif.types.LabeledType;
import polyglot.main.Report;
import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.FieldInstance;
import polyglot.types.MemberInstance;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.PrimitiveType;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;

@Deprecated
public class PolyglotToJavaConverter {

	ClassLoader cl;
	
	public PolyglotToJavaConverter() {
		cl = ClassLoader.getSystemClassLoader();
	}
	
	public Class classForParsedClassType(ParsedClassType ct) {
		// if ct is a nested class, first get the container, then 
		// find its java class, then search through its declared classes
		// for ct's Class object
		if (ct.isNested() && ct.container() instanceof ParsedClassType) {
			Class containerClass = classForParsedClassType((ParsedClassType) ct.container());
			for (Class nestedClass : containerClass.getDeclaredClasses()) {
				if (nestedClass.getSimpleName().equals(ct.name()))
					return nestedClass;
			}
			return null;
			
		}
		else {
			// otherwise, ct is a top-level class and should be 
			// accessible with the ClassLoader
			Class c;

			try {
				c = cl.loadClass(ct.fullName());
				return c;
			}
			catch (ClassNotFoundException e) {
				return null;
			}
		}
	}
	
	public Field fieldForFieldInstance(FieldInstance fi) {
		Class c = classForParsedClassType((ParsedClassType) fi.container());
		if (c != null) {
			try {
				return c.getDeclaredField(fi.name());
			}
			catch (NoSuchFieldException e) { }
		}
		
		return null;
	}
	
	public Method methodForMethodInstance(MethodInstance mi) {
		Class c = classForParsedClassType((ParsedClassType) mi.container());
		if (c != null) {
			List<Class> parameterTypes = classListForTypeList(mi.formalTypes());
			Class[] parameterArray = parameterTypes.toArray((Class[]) Array.newInstance(c.getClass(), 0));
			if (parameterTypes != null)
				try {
					return c.getDeclaredMethod(mi.name(), parameterArray);
				} catch (NoSuchMethodException e) { 
					throw new InternalCompilerError("could not find " + mi + " in " + c + ": " + e);
				}
		}
		
		return null; 
	}


	public Constructor constructorForConstructorInstance(ConstructorInstance ci) {
		
		Class c = classForParsedClassType((ParsedClassType) ci.container());
		if (c != null) {
			List<Class> parameterTypes = classListForTypeList(ci.formalTypes());
			Class[] parameterArray = parameterTypes.toArray((Class[]) Array.newInstance(c.getClass(), 0));
			if (parameterTypes != null)
				try {
					return c.getDeclaredConstructor((Class[]) parameterArray);
				} catch (NoSuchMethodException e) { }
		}
		
		return null; 
	}
	
	public List<Member> memberListForMemberInstanceList(Collection<MemberInstance> members) {
		List<Member> returnList = new LinkedList<Member>();
		for(MemberInstance mi : members) {
			if (shouldReport(1))
				Report.report(1, "---> polyglot: " + mi);
			Member m = memberForMemberInstance(mi);
			if (shouldReport(1))
				Report.report(1, "<--- java: " + m);
			if (m != null)
				returnList.add(m);
		}
		
		return returnList;
	}

	private boolean shouldReport(int i) {
		return Report.should_report("convert", 1);
	}

	private Member memberForMemberInstance(MemberInstance mi) {
		if (mi instanceof FieldInstance) {
			return fieldForFieldInstance((FieldInstance) mi);
		}
		else if (mi instanceof MethodInstance) {
			return methodForMethodInstance((MethodInstance) mi);
		}
		else if (mi instanceof ConstructorInstance) {
			return constructorForConstructorInstance((ConstructorInstance) mi);
		}
		else if (mi instanceof ClassType) {
			if (shouldReport(2))
				Report.report(2, "WARNING: inner class " + mi + " being converted to null");
			return null;	
		}
		
		throw new InternalCompilerError("cannot convert " + mi + " (" + mi.getClass() + ")");
	}

	private List<Class> classListForTypeList(List<Type> list) {
		List<Class> returnList = new LinkedList<Class>();
		for(Type t : list) {
			Class c = classForType(t);
			returnList.add(c);
		}
		return returnList;

	}
	
	public Class classForType(Type t) {
		if (t instanceof PrimitiveType) {
			PrimitiveType pt = (PrimitiveType) t;
			if (pt.kind() == PrimitiveType.BOOLEAN)
				return Boolean.TYPE;
			if (pt.kind() == PrimitiveType.BYTE)
				return Byte.TYPE;
			if (pt.kind() == PrimitiveType.CHAR)
				return Character.TYPE;
			if (pt.kind() == PrimitiveType.DOUBLE)
				return Double.TYPE;
			if (pt.kind() == PrimitiveType.FLOAT)
				return Float.TYPE;
			if (pt.kind() == PrimitiveType.INT)
				return Integer.TYPE;
			if (pt.kind() == PrimitiveType.LONG)
				return Long.TYPE;
			if (pt.kind() == PrimitiveType.SHORT)
				return Short.TYPE;
			if (pt.kind() == PrimitiveType.VOID)
				return Void.TYPE;

			throw new InternalCompilerError("could not find class for " + t.toString());
		}
		if (t instanceof ParsedClassType)
			return classForParsedClassType((ParsedClassType) t);
		if (t instanceof LabeledType)
			return classForType(((LabeledType) t).typePart());
		if (t instanceof ArrayType) {
			ArrayType at = (ArrayType) t;
			// this is ugly, but there's no way 
			Class c = classForType(at.base());
			if (c != null) {
				Class arrayC;
				arrayC = Array.newInstance(c, 0).getClass();
				return arrayC;
			}
		}
		
		Class c;
		try {
			c = cl.loadClass(t.toString());
			return c;
		} catch (ClassNotFoundException e) {
			throw new InternalCompilerError("could not convert polyglot type " + t + " into a java type");
		}
	}

	public Collection<Class> classListForParsedClassList(Collection<ParsedClassType> undeclaredPolyglotClasses) {
		Collection<Class> returnList = new LinkedList<Class>();
		for(ParsedClassType ct : undeclaredPolyglotClasses) {
			returnList.add(classForParsedClassType(ct));
		}
		
		return returnList;
	}
}
