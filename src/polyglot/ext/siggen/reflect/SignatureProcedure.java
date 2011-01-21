/******************************************************************** 
 * File          : SignatureProcedure.java
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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class SignatureProcedure extends SignatureElement implements SignatureMember {
	
	/**
	 * Signature Contents underlying elements
	 */
	protected Collection<SignatureType> parameterTypes;
	protected Collection<SignatureType> exceptionTypes;
	
	public SignatureProcedure(SignatureClass containingClass, Collection<SignatureType> parameterTypes,
			Collection<SignatureType> exceptionTypes) {
		super(containingClass);
		this.parameterTypes = parameterTypes;
		this.exceptionTypes = exceptionTypes;
	}

	protected String getExceptionString(Collection<SignatureImport> importTypes) {
		String exceptionString = "";
		if (exceptionTypes.size() > 0) {
			exceptionString += " throws ";
			Iterator<SignatureType> itExcTypes = exceptionTypes.iterator();
			while(itExcTypes.hasNext()) {
				SignatureType c = itExcTypes.next();
				exceptionString += c.getName(importTypes, this.containingClass.parameterize(), true, true);

				notifyTypeUsed(importTypes, c);
				
				if(itExcTypes.hasNext()) {
					exceptionString += ", ";
				}
			}
		}
		return exceptionString;
	}

	protected String getArgumentString(Collection<SignatureImport> importTypes, List<String> argLabels, boolean canParameterizeWithLabel) {
		String argString = "";
		Collection<SignatureType> argTypes = parameterTypes;
		Iterator<SignatureType> itArgTypes = argTypes.iterator();
		
		while(itArgTypes.hasNext()) {
			SignatureType sc = itArgTypes.next();
			argString += sc.getName(importTypes, containingClass.parameterize(), canParameterizeWithLabel, true);
			if (containingClass.parameterize() && canParameterizeWithLabel) {
				argString += "{L}";
			}int var = containingClass.getAndIncrementVariableCounter();
			argString += " v" + var;

			if (argLabels != null) {
				argLabels.add("v" + var);
			}
			

			notifyTypeUsed(importTypes,sc);

			if (itArgTypes.hasNext())
				argString += ", ";
		}
		return argString;
	}

	protected String getJoinLabelString(List<String> arglabels) {
		String str = "";
		Iterator<String> itArgLabels = arglabels.iterator();
		if (itArgLabels.hasNext()) {
			String first = itArgLabels.next();
			str += first;
			while(itArgLabels.hasNext()) {
				String argLabel = itArgLabels.next();
				str += "; " + argLabel;
			}
		}
		
		return "{" + str + "}";
	}
}
