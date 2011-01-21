/******************************************************************** 
 * File          : SignaturePrimitiveType.java
 * Project       : siggen
 * Description   : <DESCRIPTION>
 * Author(s)     : dhking
 *  
 * Created       : Feb 7, 2008 ${}time}
 *  Copyright (c) 2007-2008 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */
package polyglot.ext.siggen.reflect;

import java.util.Collection;

import polyglot.types.ParsedClassType;
import polyglot.types.Type;

public class SignaturePrimitiveType extends SignatureType {

	public SignaturePrimitiveType(Type type) {
		super(null, type, false);
	}
	
	@Override
	public String getName(Collection<SignatureImport> importTypes,
			boolean canParameterizeInContainer,
			boolean canParameterizeWithLabel, boolean canParameterizeWithThis) {
		return getSimpleName();
	}
}
