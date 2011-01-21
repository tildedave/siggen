/******************************************************************** 
 * File          : SignatureMethod.java
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.main.Report;
import polyglot.types.MemberInstance;
import polyglot.types.MethodInstance;

public class SignatureMethod extends SignatureProcedure {

	/**
	 * Polyglot underlying element
	 */
	protected MethodInstance methodInstance;
	
	/**
	 * Methods have return types
	 */
	protected SignatureType returnType;
	
	/**
	 * is this method a stub for an abstract superclass?
	 */
	protected boolean isStub;

	public SignatureMethod(SignatureClass containingClass, 
			Collection<SignatureType> parameterTypes,
			Collection<SignatureType> exceptionTypes, MethodInstance methodInstance,
			SignatureType returnType, boolean isStub) {
		super(containingClass, parameterTypes, exceptionTypes);
		this.methodInstance = methodInstance;
		this.returnType = returnType;
		this.isStub = isStub;
	}

	@Override
	public String getElementString(Collection<SignatureImport> importTypes) {
		if (shouldReport(3))
			Report.report(3, "get string for java method " + methodInstance + " in class " + containingClass);

		String modifierString = getModifierString(methodInstance.flags(), false, isStub);
		List<String> arglabels = new LinkedList<String>();
		
		// hacky -- in the future, actually keep a record of what method supertypes are doing
		boolean canParameterizeWithLabel = !(this.methodInstance.name().equals("toString") && (this.methodInstance.formalTypes().size() == 0));

		if (!methodInstance.flags().isStatic())
			arglabels.add(containingClass.getSelfLabelForClass(canParameterizeWithLabel));


		String argString = getArgumentString(importTypes, arglabels, canParameterizeWithLabel);
		String returnLabel = "";
		
		notifyTypeUsed(importTypes, returnType);
		if (!returnType.isVoid())
			// don't need a return label if the method returns void 
			returnLabel = getJoinLabelString(arglabels);

		String returnTypeName = returnType.getName(importTypes, containingClass.parameterize(), canParameterizeWithLabel, true);
		
		String methodString = 
			modifierString + " " + 
			returnTypeName + 
			returnLabel + " " + methodInstance.name() + "(";
		
		methodString += argString;
		methodString += ")";
		
		methodString += getExceptionString(importTypes);
		methodString += ";";

		if (shouldReport(3))
			Report.report(3, "returning " + methodString);
		
		return squish(methodString);

	}

	@Override
	protected String getExceptionString(Collection<SignatureImport> importTypes) {
		// for javacard API ... make sure that these runtime exceptions are actually declared
		// to be thrown by the signature.  not ideal, but the header isn't done correctly
		String superString = super.getExceptionString(importTypes);
		if (methodInstance.name().equals("throwIt") && 
			methodInstance.flags().isStatic() && 
				!exceptionTypes.contains(this.containingClass)) {
			if (!superString.equals(""))
				superString += ", ";
			else 
				superString += "throws ";
			superString += this.containingClass.getName(importTypes, this.containingClass.parameterize(), true, true);
		}

		return superString;
	}

	public MemberInstance getMemberInstance() {
		return methodInstance;
	}
}
