/******************************************************************** 
 * File          : SignatureGenerator.java
 * Project       : siggen
 * Description   : <DESCRIPTION>
 * Author(s)     : dhking
 *  
 * Created       : Feb 7, 2008 10:27:55 AM
 *  Copyright (c) 2007-2008 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */
package polyglot.ext.siggen.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import polyglot.ext.siggen.SignatureOptions;
import polyglot.ext.siggen.reflect.InlineSignatureClass;
import polyglot.ext.siggen.reflect.PolyglotToJavaConverter;
import polyglot.ext.siggen.reflect.SignatureClass;
import polyglot.ext.siggen.reflect.SignatureConstructor;
import polyglot.ext.siggen.reflect.SignatureDefaultConstructor;
import polyglot.ext.siggen.reflect.SignatureElement;
import polyglot.ext.siggen.reflect.SignatureField;
import polyglot.ext.siggen.reflect.SignatureMethod;
import polyglot.ext.siggen.reflect.SignatureType;
import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.FunctionInstance;
import polyglot.types.Importable;
import polyglot.types.MemberInstance;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.ProcedureInstance;
import polyglot.types.SemanticException;
import polyglot.types.SourceClassResolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.TypedList;

public class SignatureGenerator {

	protected SignatureContents sigContents;
	protected PolyglotToJavaConverter ptjc;
	protected SourceClassResolver noRawClassResolver;

	protected HashMap<TypeObject, SignatureElement> cache;
	protected TypeSystem ts;
	
	public SignatureGenerator(TypeSystem ts, SignatureContents sigContents, SourceClassResolver noRawClassResolver) {
		this.ts = ts;
		this.sigContents = sigContents;
		this.ptjc = new PolyglotToJavaConverter();
		this.noRawClassResolver = noRawClassResolver;
		this.cache = new HashMap<TypeObject, SignatureElement>();
	}

	public SignatureClass getSignatureClass(ParsedClassType type) {
		ParsedClassType ct = (ParsedClassType) type;
		boolean shouldParameterize = shouldParameterize(ct);

		SignatureClass sc = new SignatureClass(null, ct, shouldParameterize);

		// choose the superclass to extend
		ParsedClassType superClassToGenerate = getSuperClassToGenerate(ct);
		if (superClassToGenerate != null) {
			SignatureType superClass = getSignatureType(superClassToGenerate, sc);
			sc.setSignatureSuperClass(superClass);
		}

		// get the interfaces to extend -- should be a list of Type
		TypedList.check(ct.interfaces(), ParsedClassType.class);
		Collection<Type> interfaces = ct.interfaces();
		Collection<Type> superInterfaces = getSuperInterfaces(interfaces);
		//Collection<ParsedClassType> interfacesToGenerate = getClassesToGenerateFromTypeCollection(superInterfaces);

		Collection<SignatureType> signatureInterfaces = getSignatureTypesFromTypeCollection(getClassesToGenerateFromTypeCollection(superInterfaces), sc);
		sc.setSignatureInterfaces(signatureInterfaces);

		LinkedList<SignatureElement> signatureElements = new LinkedList<SignatureElement>();
		Collection<SignatureElement> stubElements = new LinkedList<SignatureElement>();

		Collection<MemberInstance> inlineMembers = new LinkedList<MemberInstance>();
		boolean seenDefaultConstructor = false;
		
		Collection<MemberInstance> memberInstances = sigContents.getMemberInstancesForClass(ct);
		for(MemberInstance mi : memberInstances) {
			if (shouldIncludeMemberInstance(mi, sc, inlineMembers)) {
				SignatureElement se = getSignatureElement(sc, mi, false);
				signatureElements.add(se);
				if (isDefaultConstructor(mi)) {
					seenDefaultConstructor = true;
				}
			}
		}

		Collection<MemberInstance> stubs = sigContents.getStubsForClass(ts, ct);
		for(MemberInstance stub : stubs) {
			SignatureElement se = getSignatureElement(sc, stub, true);
			stubElements.add(se);
			if (isDefaultConstructor(stub)) {
				seenDefaultConstructor = true;
			}
		}

		if (!sc.isInterface() && !seenDefaultConstructor) {
			signatureElements.addFirst(getDefaultConstructor(sc));
		}
		sc.setSignatureElements(signatureElements);
		sc.setStubElements(stubElements);

		if (!inlineMembers.isEmpty()) {
			InlineSignatureClass isc = new InlineSignatureClass(null, ct, shouldParameterize);
			LinkedList<SignatureElement> inlineSignatureElements = new LinkedList<SignatureElement>();
			
			for(MemberInstance mi : inlineMembers) {
				SignatureElement se = getSignatureElement(sc, mi, false);
				inlineSignatureElements.add(se);
			}
			
			isc.setSignatureElements(inlineSignatureElements);
			isc.setSignatureInterfaces(new LinkedList<SignatureType>());
			isc.setStubElements(new LinkedList<SignatureElement>());
			sc.setInlineClass(isc);
			sigContents.addInlineClass(isc);
			
			System.err.println("\tWARNING: " + sc.getShortNameForClass() + " has inline members");
			System.err.println("\tOutput inline members as " + isc.getShortNameForClass() + " in inline source directory");
			
		}
		
		return sc;
	}
	
	protected boolean shouldIncludeMemberInstance(MemberInstance mi, SignatureClass sc, Collection<MemberInstance> inlineMembers) {
		// do not include:
		// protected/private constructors
		// default constructors
		// constructors for interfaces
		// methods that involve types in the package (and output a warning)
		if (mi instanceof ConstructorInstance) {
			ConstructorInstance ci = (ConstructorInstance) mi;
			if (ci.flags().isProtected() && ci.formalTypes().size() == 0)
				return false;
			if (ci.flags().isPrivate() && ci.formalTypes().size() == 0)
				return false;
			if (!ci.flags().isPublic() && 
				!ci.flags().isProtected() && 
				!ci.flags().isPrivate() && 
				ci.formalTypes().size() == 0)
				return false;
			if (sc.isInterface())
				return false;
		}
		
		if (containsReferenceToSpecialPackages(mi)) {
			inlineMembers.add(mi);
			return false;
		}
		
		return true;
	}

	private boolean containsReferenceToSpecialPackages(MemberInstance mi) {
		if (mi instanceof ProcedureInstance) {
			ProcedureInstance pi = (ProcedureInstance) mi;
			for(Type t : (Collection<Type>) pi.formalTypes()) {
				if (containsReferenceToSpecialPackages(t))
					return true;
			}
			for(Type t : (Collection<Type>) pi.throwTypes()) {
				if (containsReferenceToSpecialPackages(t))
					return true;
			}
			if (pi instanceof FunctionInstance) {
				if (containsReferenceToSpecialPackages(((FunctionInstance) pi).returnType()))
					return true;
			}
		}
		if (mi instanceof FieldInstance) {
			FieldInstance fi = (FieldInstance) mi;
			if (containsReferenceToSpecialPackages(fi.type()))
				return true;
		}
		
		return false;
	}

	private boolean containsReferenceToSpecialPackages(Type t) {
		Collection<String> specialPackages = ((SignatureOptions) SignatureOptions.global).packages;
		if (t instanceof ParsedClassType) {
			return specialPackages.contains(((ParsedClassType) t).package_().toString());
		}
		return false;
	}

	private SignatureElement getDefaultConstructor(SignatureClass sc) {
		return new SignatureDefaultConstructor(sc, 
				ts.constructorInstance(Position.COMPILER_GENERATED, (ClassType) sc.getPolyglotClass(), Flags.PUBLIC, new LinkedList(), new LinkedList()));
	}

	private boolean isDefaultConstructor(MemberInstance mi) {
		if (mi instanceof ConstructorInstance) {
			ConstructorInstance ci = (ConstructorInstance) mi;
			return (ci.flags().isPublic() && ci.formalTypes().size() == 0);
		}
		
		return false;
	}

	protected Collection<SignatureType> getSignatureTypesFromTypeCollection(
			Collection<Type> types, SignatureClass containingClass) {
		Collection<SignatureType> sigClasses = new LinkedList<SignatureType>();
		for(Type t : types) {
			if (shouldIncludeType(t))
				sigClasses.add(getSignatureType(t, containingClass));
		}

		return sigClasses;
	}

	private boolean shouldIncludeType(Type t) {
		// either t is a 1) primitive, 2) not a raw class, 3) going to be generated
		// 4) an array of a type we should include
		if (t.isPrimitive())
			return true;
		if (t instanceof ParsedClassType) {
			ParsedClassType ct = (ParsedClassType) t;
			return !isRawClass(ct) || shouldGenerate(ct);
		}
		if (t instanceof ArrayType) {
			ArrayType at = (ArrayType) t;
			return shouldIncludeType(at.base());
		}
		
		throw new InternalCompilerError("can't tell if we should include " + t + " from the type: " + t.getClass());
	}

	protected SignatureType getSignatureType(Type t, SignatureClass containingClass) {
		return new SignatureType(containingClass, t, shouldParameterize(t));
	}

	protected Collection<Type> getClassesToGenerateFromTypeCollection(
			Collection<Type> types) {
		Collection<Type> classesToGenerate = new LinkedList<Type>();
		for(Type t : types) {
			if (t instanceof ParsedClassType) {
				ParsedClassType ct = (ParsedClassType) t;
				if (shouldGenerate(ct))
					classesToGenerate.add(ct);
			}
		}

		return classesToGenerate;
	}

	protected Collection<Type> getSuperInterfaces(Collection<Type> interfaces) {
		Collection<Type> superInterfaces = new LinkedHashSet<Type>(interfaces);
		for(Type i : interfaces) {
			if (i instanceof ParsedClassType) {
				ParsedClassType ct = (ParsedClassType) i;
				superInterfaces.addAll(ct.interfaces());
			}
		}

		if (interfaces.containsAll(superInterfaces))
			return superInterfaces;
		else
			return getSuperInterfaces(superInterfaces);
	}


	private ParsedClassType getSuperClassToGenerate(ParsedClassType ct) {
		if (ct.superType() == null)
			return null;

		ParsedClassType superCt = (ParsedClassType) ct.superType();
		if (shouldGenerate(superCt) || !isRawClass(superCt)) {
			return superCt;
		}

		return getSuperClassToGenerate(superCt);
	}

	protected SignatureElement getSignatureElement(SignatureClass containingClass, MemberInstance mi, boolean isStub) {
		if (mi instanceof ConstructorInstance) {
			return getSignatureConstructor(containingClass, (ConstructorInstance) mi);
		}
		if (mi instanceof FieldInstance) {
			return getSignatureField(containingClass, (FieldInstance) mi);
		}
		if (mi instanceof MethodInstance) {
			return getSignatureMethod(containingClass, (MethodInstance) mi, isStub);
		}
		throw new InternalCompilerError("can't convert mi (" + mi.getClass() + ")");
	}

	protected SignatureElement getSignatureMethod(SignatureClass containingClass, MethodInstance mi, boolean isStub) {
		Collection<SignatureType> parameterTypes = getSignatureTypesFromTypeCollection(mi.formalTypes(), containingClass);
		Collection<SignatureType> exceptionTypes = getSignatureTypesFromTypeCollection(mi.throwTypes(), containingClass);
		SignatureType returnType = getSignatureType(mi.returnType(), containingClass);

		return new SignatureMethod(containingClass, parameterTypes, exceptionTypes, mi, returnType, isStub);
	}

	protected  SignatureElement getSignatureField(SignatureClass containingClass, FieldInstance fi) {
		FieldInstance polyglotField = fi;
		SignatureType fieldType = getSignatureType(fi.type(), containingClass);
		return new SignatureField(containingClass, polyglotField, fieldType);
	}

	private SignatureElement getSignatureConstructor(SignatureClass containingClass, ConstructorInstance ci) {
		Collection<SignatureType> parameterTypes = getSignatureTypesFromTypeCollection(ci.formalTypes(), containingClass);		
		Collection<SignatureType> exceptionTypes = getSignatureTypesFromTypeCollection(ci.throwTypes(), containingClass);
		ConstructorInstance polyglotConstructor = ci;
		return new SignatureConstructor(containingClass, parameterTypes, exceptionTypes, polyglotConstructor);
	}

	private boolean shouldParameterize(Type t) {
		if (t instanceof ParsedClassType) {
			ParsedClassType ct = (ParsedClassType) t;
			return (shouldGenerate(ct) && 
					((SignatureOptions) SignatureOptions.global).should_parameterize &&
					!t.isThrowable() && 
					!ct.fullName().equals("java.lang.Object") &&
					!ct.fullName().equals("java.lang.Class") &&
					!ct.fullName().equals("java.lang.String"));
		}
		return false;
	}

	private boolean shouldGenerate(ParsedClassType ct) {
		return sigContents.getUndeclaredClasses().contains(ct);
	}

	protected boolean isRawClass(ParsedClassType ct) {
		try {
			noRawClassResolver.find(ct.fullName());
		} catch (SemanticException e) {
			if (e.getMessage().contains("A class file was found, but")) {
				return true;
			}
		}

		return false;
	}
}
