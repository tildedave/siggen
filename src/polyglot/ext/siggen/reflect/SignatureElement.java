/******************************************************************** 
 * File          : SignatureElement.java
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

import polyglot.main.Report;
import polyglot.types.Flags;
import polyglot.types.MemberInstance;
import polyglot.types.ParsedClassType;

public abstract class SignatureElement {

	protected SignatureClass containingClass;
	
	public SignatureElement(SignatureClass containingClass) {
		this.containingClass = containingClass;
	}
	
	public abstract String getElementString(Collection<SignatureImport> importTypes);
	
	/** Utility Methods */
	
	protected String squish(String s) {
		String returnString = s.replaceAll("  ", " ");
		if (returnString.startsWith(" "))
			returnString = returnString.substring(1);
		return returnString;
	}
	
	protected String getModifierString(Flags flags, boolean isConstructor, boolean isStub) {
		ParsedClassType c = containingClass.getPolyglotClass();
		boolean isInterface = c.flags().isInterface();
		Flags methodFlags = flags;
		if (isInterface || isStub) {
			methodFlags = methodFlags.clearAbstract();
		}
		if (!isInterface || isStub) {
			methodFlags = methodFlags.Native();
		}
		if (isConstructor || methodFlags.isAbstract() || isInterface)
			methodFlags = methodFlags.clearNative();
		
		methodFlags = methodFlags.clearTransient().clearVolatile().clearFinal();
		
		String modifierString = squish(methodFlags.toString());
		
		return modifierString;
	}

	protected boolean shouldReport(int level) {
		return Report.should_report("siggen", level);
	}

	protected void notifyTypeUsed(Collection<SignatureImport> imports, SignatureType c) {
		if (c.isNotPrimitiveType())
			imports.add(new SignatureImport(c));
	}
}
