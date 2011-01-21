/******************************************************************** 
 * File          : SignatureClass.java
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

import polyglot.main.Report;
import polyglot.types.Flags;
import polyglot.types.MemberInstance;
import polyglot.types.ParsedClassType;

public class SignatureClass extends SignatureElement {

	protected ParsedClassType polyglotClass;
	protected boolean shouldParameterize;
	protected boolean isInterface;
	protected int variableCounter;
	
	protected SignatureType signatureSuperClass;
	protected Collection<SignatureType> signatureInterfaces;
	
	protected Collection<SignatureElement> signatureElements;
	protected Collection<SignatureElement> stubElements;
	protected InlineSignatureClass inlineClass = null;
	
	public SignatureClass(SignatureClass containingClass, 
			ParsedClassType type, boolean shouldParameterize) {
		super(containingClass);
		this.polyglotClass = type;
		this.shouldParameterize = shouldParameterize;
		this.variableCounter = 0;
		this.isInterface = polyglotClass.flags().isInterface();
	}

	public void setSignatureInterfaces(
			Collection<SignatureType> signatureInterfaces) {
		this.signatureInterfaces = signatureInterfaces;
	}
	
	public void setSignatureElements(
			Collection<SignatureElement> signatureElements) {
		this.signatureElements = signatureElements;
	}
	
	public void setSignatureSuperClass(SignatureType signatureSuperClass) {
		this.signatureSuperClass = signatureSuperClass;
	}
	
	@Override
	public String getElementString(Collection<SignatureImport> importTypes) {
		// get header string
		String headerString = getClassHeaderString(importTypes);
		// get element string
		String memberString = getClassMemberString(importTypes);
		
		return headerString + "{\n" + memberString + "}";
	}

	public String getPackageString() {
		return "package " + polyglotClass.package_() + ";\n";
	}

	protected String getClassMemberString(Collection<SignatureImport> importTypes) {
		String signatureIndicatorString = getSignatureIndicatorString(); 
		if (!signatureIndicatorString.equals(""))
			signatureIndicatorString += "\n";
		
		String elementString = getSignatureElementString(importTypes);
		String stubString = getStubElementString(importTypes);
		String stubSeparator = "";
		if (!stubString.equals(""))
			stubSeparator = getStubSeparator();

		return signatureIndicatorString + elementString + stubSeparator + stubString;
	}

	protected String getStubElementString(Collection<SignatureImport> importTypes) {
		StringBuffer elementBuffer = new StringBuffer();
		for(SignatureElement se : stubElements) {
			elementBuffer.append("\t" + se.getElementString(importTypes) + "\n");
		}
		return elementBuffer.toString();
	}

	protected String getSignatureElementString(
			Collection<SignatureImport> importTypes) {
		StringBuffer elementBuffer = new StringBuffer();
		for(SignatureElement se : signatureElements) {
			elementBuffer.append("\t" + se.getElementString(importTypes) + "\n");
		}
		return elementBuffer.toString();
	}

	protected String getStubSeparator() {
		return "// STUB: methods not called by the program, but required by abstract superclass\n";
	}

	protected String getSignatureIndicatorString() {
		return "\tstatic int __JIF_SIG_OF_JAVA_CLASS$20030619 = 0; // jif signature of a java class\n";
	}

	public ParsedClassType getPolyglotClass() {
		return polyglotClass;
	}

	public String getSelfLabelForClass(boolean canParameterizeWithLabel) {
		if (shouldParameterize && canParameterizeWithLabel)
			return "L";
		else
			return "this";
	}

	public int getAndIncrementVariableCounter() {
		++variableCounter;
		return variableCounter;
	}

	public String getName(Collection<SignatureImport> importTypes, 
			boolean canParameterizeInContainer, 
			boolean canParameterizeWithLabel,
			boolean canParameterizeWithThis) {
		// the name of the class includes any parameterized label
		String simpleName = getShortNameForClass();
		String appendParameter = "";
		if (shouldParameterize) {
			if (!canParameterizeWithLabel && !canParameterizeWithThis)
				appendParameter = "[{}]";
			else if (canParameterizeWithLabel && canParameterizeInContainer)
				appendParameter = "[L]";
			else if (canParameterizeWithThis)
				appendParameter = "[{this}]";
		}
		
		return simpleName += appendParameter;
	}

	public boolean parameterize() {
		return this.shouldParameterize;
	}
	
	protected String getClassHeaderString(Collection<SignatureImport> importTypes) {
		//String classHeader = Modifier.toString(javaClass.getModifiers());
		String classHeader = getClassHeaderDeclarationString();

		if (isInterface && classHeader.contains("abstract")) {
			classHeader = classHeader.replace("abstract", "");
			classHeader = squish(classHeader);
		}
		if (classHeader.length() > 0)
			classHeader += " ";
		if (!isInterface)
			classHeader += "class ";
//		else
//		classHeader += "interface";

		classHeader += getShortNameForClass();
		if (shouldParameterize) {
			classHeader += "[label L]";
		}

		if (signatureSuperClass != null && !isInterface) {
			classHeader += " extends " + signatureSuperClass.getName(importTypes, this.shouldParameterize, true, false);
			notifyTypeUsed(importTypes, signatureSuperClass);
		}

		if (shouldReport(3)) {
			Report.report(3, "interfaces to generate for " + polyglotClass + ": " + signatureInterfaces);
		}
		
		if (signatureInterfaces.size() > 0) {
			if (isInterface) {
				// if c is an interface, then cannot implement these interfaces, it can only extend them
				classHeader += " extends ";
			}
			else {
				classHeader += " implements ";
			}
		}
		
		Iterator<SignatureType> genInterfaceIterator = signatureInterfaces.iterator();
		while(genInterfaceIterator.hasNext()) {
			SignatureType interfaceToGenerate = genInterfaceIterator.next();
			classHeader += interfaceToGenerate.getName(importTypes, this.shouldParameterize, true, false);
			notifyTypeUsed(importTypes, interfaceToGenerate);
			if (genInterfaceIterator.hasNext())
				classHeader += ", ";
		}
		
		if (shouldReport(2)) 
			Report.report(2, "class header for Java class " + polyglotClass + ": " + classHeader);

		return squish(classHeader) + "\n";
	}
	
	protected String getClassHeaderDeclarationString() {
		Flags f = polyglotClass.flags();
		String returnString = "";
		if (f.isPublic()) {
			f = f.clearPublic();
			returnString += "public ";
		}
		if (f.isPrivate()) {
			f = f.clearPrivate();
			returnString += "private ";
		}
		if (polyglotClass.isNested()) {
			f = f.clearStatic();
		}
		f = f.clearSynchronized();
		returnString += f.toString();
		
		return returnString;
	}

	public String getShortNameForClass() {
		if (polyglotClass instanceof ParsedClassType)
			return getNameForPotentiallyNestedClass((ParsedClassType) polyglotClass);
		else 
			return polyglotClass.toString();
	}

	protected String getNameForPotentiallyNestedClass(ParsedClassType ct) {
		if (ct.isNested() && ct.container() instanceof ParsedClassType) {
			return getNameForPotentiallyNestedClass((ParsedClassType) ct.container()) + ct.name(); 
		}
		
		return ct.name();
	}

	public void setStubElements(Collection<SignatureElement> stubElements) {
		this.stubElements = stubElements;
	}
	
	public Collection<SignatureElement> getSignatureElements() {
		return signatureElements;
	}
	
	public Collection<SignatureElement> getStubElements() {
		return stubElements;
	}

	public boolean isInterface() {
		return polyglotClass.flags().isInterface();
	}
	
	@Override
	public String toString() {
		return getShortNameForClass();
	}

	public InlineSignatureClass getInlineClass() {
		return this.inlineClass;
	}

	public void setInlineClass(InlineSignatureClass sc) {
		this.inlineClass = sc;
		sc.signatureSuperClass = new SignatureType(null, this.getPolyglotClass(), this.shouldParameterize); 
	}
}
