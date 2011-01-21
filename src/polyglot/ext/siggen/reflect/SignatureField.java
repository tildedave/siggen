/******************************************************************** 
 * File          : SignatureField.java
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
import polyglot.types.FieldInstance;
import polyglot.types.MemberInstance;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;

public class SignatureField extends SignatureElement implements SignatureMember {

	/** 
	 * Polyglot underlying element
	 */
	protected FieldInstance polyglotField;
	
	/**
	 * Fields have a type
	 */
	protected SignatureType fieldType;
	
	public SignatureField(SignatureClass containingClass, 
			FieldInstance polyglotField, SignatureType fieldType) {
		super(containingClass);
		this.polyglotField = polyglotField;
		this.fieldType = fieldType;
	}

	@Override
	public String getElementString(Collection<SignatureImport> importTypes) {
		if (shouldReport(3))
			Report.report(3, "get string for java field " + this.fieldType + " in class " + this.containingClass);
		
		boolean canParameterizeType = (polyglotField.flags().isStatic() == false);

		String fieldString = polyglotField.flags().toString() + " " + fieldType.getName(importTypes, 
				this.containingClass.parameterize(), 
				canParameterizeType, 
				canParameterizeType) + " " + polyglotField.name();

		if (polyglotField.isConstant()) {
			// if this is a constant field, i.e. SOME_CONSTANT = value, then 
			// include that value 
			// (this is not necessary for Jif, but it may be helpful for debugging)
			if (polyglotField.type().toString().equals("java.lang.String")) {
				fieldString += " = \"" + polyglotField.constantValue() + "\"";
			}
			else {
				fieldString += " = " + polyglotField.constantValue();
				if (polyglotField.type().isFloat())
					fieldString += "F";
				if (polyglotField.type().isLong())
					fieldString += "L";
			}
		}
		else if (!polyglotField.type().isPrimitive()) {
			// otherwise, it's a reference type: initialize it to null 
			fieldString += " = null";
			notifyTypeUsed(importTypes, fieldType);
		}
		else if (polyglotField.flags().isFinal() && polyglotField.type().isPrimitive()) {
			fieldString += " = " + getDefaultValueForType(polyglotField.type());
			fieldString += "; // AUTOMATICALLY INSERTED VALUE";
		}
		
		// TODO: no longer possible
		if (fieldString.equals(""))
			return "";
		
		return (fieldString + ";");
	}

	private String getDefaultValueForType(Type type) {
		if (type.isNumeric())
			return "-1";
		if (type.isBoolean())
			return "false";
		throw new InternalCompilerError("can't think of default field value for " + type);
	}

	public MemberInstance getMemberInstance() {
		return polyglotField;
	}
}
