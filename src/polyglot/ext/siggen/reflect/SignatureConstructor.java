/******************************************************************** 
 * File          : SignatureConstructor.java
 * Project       : siggen
 * Description   : <DESCRIPTION>
 * Author(s)     : dhking
 *  
 * Created       : Feb 7, 2008 10:27:55 AM
 *  Copyright (c) 2007-2008 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */
package polyglot.ext.siggen.reflect;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import polyglot.main.Report;
import polyglot.types.ConstructorInstance;
import polyglot.types.MemberInstance;

public class SignatureConstructor extends SignatureProcedure {

	protected ConstructorInstance polyglotConstructor;
	
	public SignatureConstructor(SignatureClass containingClass,
			Collection<SignatureType> parameterTypes,
			Collection<SignatureType> exceptionTypes,
			ConstructorInstance polyglotConstructor) {
		super(containingClass, parameterTypes, exceptionTypes);
		this.polyglotConstructor = polyglotConstructor;
	}

	@Override
	public String getElementString(Collection<SignatureImport> importTypes) {
		//String constructorString = Modifier.toString(javaConstructor.getModifiers()) +  " " + javaConstructor.getDeclaringClass().getSimpleName();
		String constructorString = getModifierString(polyglotConstructor.flags(), true, false) + " " + containingClass.getShortNameForClass();
		
		if (shouldReport(3))
			Report.report(3, "get string for java constructor " + polyglotConstructor + " in class " + this.containingClass);
		
		constructorString += "(" + getArgumentString(importTypes, null, true) + ")"; 
		constructorString += getExceptionString(importTypes);
		// every constructor is empty
		constructorString += " { }";

		return (constructorString);
	}

	public MemberInstance getMemberInstance() {
		return polyglotConstructor;
	}
}
