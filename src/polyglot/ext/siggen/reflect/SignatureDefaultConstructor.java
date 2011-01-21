/******************************************************************** 
 * File          : SignatureDefaultConstructor.java
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
import java.util.LinkedList;

import polyglot.main.Report;
import polyglot.types.ConstructorInstance;

public class SignatureDefaultConstructor extends SignatureConstructor {

	public SignatureDefaultConstructor(SignatureClass containingClass, ConstructorInstance polyglotConstructor) {
		super(containingClass, new LinkedList(), new LinkedList(), polyglotConstructor);
	}

	@Override
	public String getElementString(Collection<SignatureImport> importTypes) {
		// no default public constructor: include our own
		// don't want to parameterize c
		String defaultConstructorString = "public " + containingClass.getShortNameForClass() + "() { }";
		if (shouldReport(4))
			Report.report(4, "adding default public constructor in class " + containingClass);

		return defaultConstructorString;
	}

}
