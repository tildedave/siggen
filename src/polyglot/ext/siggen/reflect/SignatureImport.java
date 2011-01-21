/******************************************************************** 
 * File          : SignatureImport.java
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

import polyglot.types.ArrayType;
import polyglot.types.Named;
import polyglot.types.ParsedClassType;
import polyglot.types.ReferenceType;
import polyglot.types.Type;

public class SignatureImport extends SignatureElement {

	protected ReferenceType polyglotType;

	public SignatureImport(SignatureType c) {
		super(c.containingClass);
		this.polyglotType = (ReferenceType) c.polyglotType;
	}

	@Override
	public String getElementString(Collection<SignatureImport> importTypes) {
		Type ct = getBaseType(polyglotType);
		if (ct instanceof ParsedClassType) {
			ParsedClassType pct = ((ParsedClassType) ct);
			String fullName = pct.fullName();
			if (pct.isMember())
				// TODO: does not handle multiple named classes.  these do not arise 
				// much in APIs however
				fullName = pct.package_().fullName() + "." + ((Named) pct.container()).name() + pct.name().replaceAll("\\.", "");
			
			return "import " + fullName + ";";
		}
		return "";
	}

	private Type getBaseType(ReferenceType polyglotType) {
		ReferenceType rt = polyglotType;
		if (rt.isArray()) {
			ArrayType at = (ArrayType) rt;
			return at.base();
		}
		return rt;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SignatureImport)
			return this.polyglotType.equals(((SignatureImport) obj).polyglotType);
		return false;
	}
	
	@Override
	public int hashCode() {
		return polyglotType.hashCode();
	}
}
