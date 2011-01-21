/******************************************************************** 
 * File          : SignatureType.java
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

import polyglot.types.ClassType;
import polyglot.types.Named;
import polyglot.types.ParsedClassType;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;

public class SignatureType extends SignatureElement {

	protected Type polyglotType;
	protected boolean shouldParameterize;

	public SignatureType(SignatureClass containingClass, Type type, boolean shouldParameterize) {
		super(containingClass);
		this.polyglotType = type;
		this.shouldParameterize = shouldParameterize;
	}

	@Override
	public String getElementString(Collection<SignatureImport> importTypes) {
		throw new InternalCompilerError("should not be calling getElementString on SignatureType " + this);
	}
	
	public String getName(Collection<SignatureImport> importTypes, 
			boolean canParameterizeInContainer, 
			boolean canParameterizeWithLabel,
			boolean canParameterizeWithThis) {
		// the name of the class includes any parameterized label
		String simpleName = getSimpleName();
		String superName = this.containingClass.getName(importTypes, canParameterizeInContainer, canParameterizeWithLabel, canParameterizeWithThis);
		// we run into some issues with java.sql.Date
		if (superName.equals(simpleName))
			simpleName = getFullName();
		String appendParameter = "";
		if (shouldParameterize) {
//			if (!canParameterizeWithLabel && !canParameterizeWithThis)
//				appendParameter = "[{}]";
			if (canParameterizeWithLabel && canParameterizeInContainer)
				appendParameter = "[L]";
			else if (canParameterizeWithThis)
				appendParameter = "[{this}]";
			else
				appendParameter = "[{}]";
		}
		
		return simpleName += appendParameter;
	}

	public String getSimpleName() {
		if (polyglotType instanceof ClassType) {
			if (polyglotType instanceof ParsedClassType) {
				ParsedClassType pct = (ParsedClassType) polyglotType;
				if (pct.isMember())
					return ((Named) pct.container()).name() + ((Named) polyglotType).name();
			}
			return ((ClassType) polyglotType).name();
		}
		return polyglotType.toString();
	}

	public String getFullName() {
		if (polyglotType instanceof ClassType) {
			if (polyglotType instanceof ParsedClassType) {
				ParsedClassType pct = (ParsedClassType) polyglotType;
				if (pct.isMember())
					return pct.package_().fullName() + "." + ((Named) pct.container()).name() + ((Named) polyglotType).name();
			}
			return ((ClassType) polyglotType).fullName();
		}
		return polyglotType.toString();
	}

	public boolean isVoid() {
		return polyglotType.isVoid();
	}

	public boolean isNotPrimitiveType() {
		return !polyglotType.isPrimitive();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SignatureType) {
			System.err.println(this.polyglotType + " " + ((SignatureType) obj).polyglotType);
			return this.polyglotType.equals(((SignatureType) obj).polyglotType);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.polyglotType.hashCode();
	}
}
