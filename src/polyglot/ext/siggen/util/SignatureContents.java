/******************************************************************** 
 * File          : SignatureContents.java
 * Project       : siggen
 * Description   : Container class for signature information
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:31:28 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */

package polyglot.ext.siggen.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import polyglot.ext.siggen.SignatureOptions;
import polyglot.ext.siggen.reflect.InlineSignatureClass;
import polyglot.ext.siggen.reflect.SignatureElement;
import polyglot.ext.siggen.reflect.SignatureMember;
import polyglot.main.Report;
import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.Declaration;
import polyglot.types.FieldInstance;
import polyglot.types.MemberInstance;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.ProcedureInstance;
import polyglot.types.ReferenceType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

//container class

public class SignatureContents {

	protected Map<ReferenceType, Collection<MemberInstance>> memberInstances;
	protected Collection<ParsedClassType> classDeclarations;
	protected TypeSystem ts;
	private Collection<InlineSignatureClass> inlineClasses;

	public SignatureContents(TypeSystem ts) {
		this.memberInstances = new HashMap<ReferenceType, Collection<MemberInstance>>();
		this.classDeclarations = new LinkedHashSet<ParsedClassType>();
		this.inlineClasses = new LinkedList<InlineSignatureClass>();
		this.ts = ts;
	}

	protected void addClass(ReferenceType type) {
		if (!memberInstances.containsKey(type))
			memberInstances.put(type, new LinkedHashSet<MemberInstance>());
	}

	public void addProcedureInstance(ProcedureInstance pi) {
		if (pi instanceof MemberInstance) {
			MemberInstance memPi = (MemberInstance) pi;
			addMemberInstanceToClass(getDeclaringClass((memPi).container(), pi), memPi);
			Iterator<Type> itFormalTypes = pi.formalTypes().iterator();
			while(itFormalTypes.hasNext()) {
				Type formalType = itFormalTypes.next();
				addClassesInType(formalType);
			}
			if (pi instanceof MethodInstance) {
				addClassesInType(((MethodInstance) pi).returnType());
			}
		}
	}

	private void addClassesInType(Type formalType) {
		if (formalType instanceof ArrayType) {
			ArrayType at = (ArrayType) formalType;
			addClassesInType(at.ultimateBase());
		}
		else if (formalType instanceof ReferenceType) {
			addClass((ReferenceType) formalType);
		}
//		else if (formalType instanceof LabeledType) {
//		addClassesInType(((LabeledType) formalType).typePart());
//		}
		else if (formalType.isPrimitive())
			return;
		else {
			Report.report(1, "warning: don't know how to add classes in " + formalType + " (" + formalType.getClass() + ")");
		}
	}

	private ReferenceType getDeclaringClass(ReferenceType type, ProcedureInstance pi) {
		ProcedureInstance piDeclaration =  (ProcedureInstance) ((Declaration) pi).declaration();
		return ((MemberInstance) piDeclaration).container();
	}

	private ReferenceType getDeclaringClass(ReferenceType container,
			FieldInstance fi) {
		FieldInstance fiDeclaration = (FieldInstance) fi.declaration();
		return fiDeclaration.container();
	}

	public void addFieldInstance(FieldInstance fi) {
		addMemberInstanceToClass(getDeclaringClass(fi.container(), fi), fi);
	}

	public void addClassDeclaration(ParsedClassType ct) {
		// add this if:
		// either the packages in the options are empty
		// if it is in a package inside of the packages in the options
		Collection<String> packages = ((SignatureOptions) SignatureOptions.global).packages;
		if (packages.isEmpty() || packages.contains(ct.package_().toString())) {
			classDeclarations.add(ct);
		}
	}

	private void addMemberInstanceToClass(ReferenceType type, MemberInstance mi) {
		addClass(type);
		Collection<MemberInstance> mis = memberInstances.get(type);
		mis.add(mi);
		memberInstances.put(type, mis);
	}

	public void printOutSignatures() {
		// for each key in memberInstances, if it is NOT in classDeclarations, spill it
		for(ReferenceType rt : memberInstances.keySet()) {
			if (!classDeclarations.contains(rt) && rt instanceof ParsedClassType) {
				System.err.println(rt);
				Collection<MemberInstance> rtMemberInstances = memberInstances.get(rt);
				for(MemberInstance mi : rtMemberInstances) {
					System.err.println("\t" + mi);
				}
			}
		}
	}

	public void addParsedClassType(ParsedClassType type) {
		addClass(type);
		if (type.isThrowable())
			addClass(type.typeSystem().Throwable());
		if (type.descendsFrom(type.typeSystem().RuntimeException())) {
			addClass(type.typeSystem().RuntimeException());
		}
	}

	public Collection<ParsedClassType> getUndeclaredClasses() {
		Collection<ParsedClassType> undeclaredClasses = new LinkedHashSet<ParsedClassType>();
		for(ReferenceType rt : memberInstances.keySet()) {
			if (rt instanceof ParsedClassType)
				undeclaredClasses.add((ParsedClassType) rt);
		}
		undeclaredClasses.removeAll(classDeclarations);

		return undeclaredClasses;
	}

	public Collection<MemberInstance> getMemberInstancesForClass(ParsedClassType ct) {
		if (memberInstances.containsKey(ct))
			return memberInstances.get(ct);
		else
			return new LinkedList<MemberInstance>();
	}

	public Collection<MemberInstance> getStubsForClass(TypeSystem ts, ParsedClassType ct) {
		return addStubsForAbstractParentsAndInterfaces(ts, ct, getMemberInstancesForClass(ct));
	}

	private Collection<MemberInstance> addStubsForAbstractParentsAndInterfaces(TypeSystem ts,
			ParsedClassType ct,
			Collection<MemberInstance> members) {
		Collection<MemberInstance> stubs = new LinkedHashSet<MemberInstance>();

		if (!ct.flags().isAbstract()) {
			Collection<MemberInstance> abstractStubs = getStubsFromAbstractMembers(ts, ct, members);
			stubs.addAll(abstractStubs);
		}

		Collection<MemberInstance> interfaceStubs = getStubsFromInterfaces(ts, ct, members);	
		stubs.addAll(interfaceStubs);

		return stubs;
	}

	private Collection<MemberInstance> getStubsFromInterfaces(
			TypeSystem ts, ParsedClassType ct, Collection<MemberInstance> members) {

		Collection<MemberInstance> instanceMembers = getMethodsFromInterfaces(ct);
		Collection<MemberInstance> initialStubsToGenerate = new LinkedHashSet<MemberInstance>(instanceMembers);

		for (MemberInstance ab : instanceMembers) {
			for(MemberInstance m : members) {
				if (m instanceof MethodInstance) {
					List<MethodInstance> implemented = ts.implemented((MethodInstance) m);
					if (implemented.contains(ab)) {
						// ab is covered by what we're going to do so far... let's not add a stub for it
						initialStubsToGenerate.remove(ab);
					}
				}
			}
		}
		Collection<MemberInstance> stubsToGenerate = new LinkedList<MemberInstance>(initialStubsToGenerate);
		for (MemberInstance stub : initialStubsToGenerate) {
			// remove any memberInstances that 'stub' is implementing that are in 
			// stubsToGenerate.  i.e. don't include both java.util.Collection.iterator() and 
			// java.util.List.iterator()
			if (stub instanceof MethodInstance) {
				MethodInstance mi = (MethodInstance) stub;
				HashSet<MethodInstance> implemented = new HashSet(ts.implemented(mi));
				implemented.remove(mi);
				stubsToGenerate.removeAll(implemented);
			}
		}

		return stubsToGenerate;
	}

	private Collection<MemberInstance> getMethodsFromAbstractParents(
			ParsedClassType ct) {

		// ct may have some superclasses that define abstract methods.  if ct is 
		// not abstract, then we should add member instances for each of these to
		// members; these are just stubs, and they are not used
		Collection<ClassType> parents = new LinkedList<ClassType>();
		getAbstractParents(ct, parents);

		// for each abstract parent, look at the method instances that are 
		// abstract inside it
		Collection<MemberInstance> abstractMembers = new LinkedList<MemberInstance>();
		for(ClassType p : parents) {
			Collection<MemberInstance> pUsedMembers = getMemberInstancesForClass((ParsedClassType) p);
			if (pUsedMembers != null) {
				for(MemberInstance m : pUsedMembers) {
					if (m.flags().isAbstract()) {
						abstractMembers.add(m);
					}
				}
			}
		}
		return abstractMembers;
	}

	private void getAbstractParents(ClassType ct, Collection<ClassType> parents) {
		if (ct.flags().isAbstract()) {
			parents.add(ct);
		}
		if (ct.superType() != null)
			getAbstractParents((ClassType) ct.superType(), parents);
	}

	private Collection<MemberInstance> getMethodsFromInterfaces(
			ParsedClassType ct) {
		// ct may have some superclasses that define abstract methods.  if ct is 
		// not abstract, then we should add member instances for each of these to
		// members; these are just stubs, and they are not used
		Collection<ClassType> parents = new LinkedList<ClassType>();
		getInterfaces(ct, parents);

		// for each abstract parent, look at the method instances that are 
		// abstract inside it

		Collection<MemberInstance> interfaceMembers = new LinkedList<MemberInstance>();
		for(ClassType p : parents) {
			Collection<MemberInstance> pUsedMembers = getMemberInstancesForClass((ParsedClassType) p);
			if (pUsedMembers != null) {
				for(MemberInstance m : pUsedMembers) {
					interfaceMembers.add(m);
				}
			}
		}

		return interfaceMembers;
	}

	private void getInterfaces(ParsedClassType ct, Collection<ClassType> interfaces) {
		interfaces.addAll(getInterfacesInSupertypes(ct));
	}

	private Collection<ClassType> getInterfacesInSupertypes(ClassType t) {
		Collection<ClassType> interfacesInSupertypes = new LinkedHashSet<ClassType>();
		if (t != null) {
			List<Type> ctInterfaces = t.interfaces();
			for(Type tInterface : ctInterfaces) {
				if (tInterface instanceof ClassType) {
					ClassType ct = (ClassType) tInterface;
					interfacesInSupertypes.add(ct);
					Collection<ClassType> superInterfaces = getInterfacesInSupertypes(ct);
					interfacesInSupertypes.addAll(superInterfaces);
				}
			}

			interfacesInSupertypes.addAll(getInterfacesInSupertypes((ClassType) t.superType()));
		}

		return interfacesInSupertypes;
	}


	private Collection<MemberInstance> getStubsFromAbstractMembers(
			TypeSystem ts, ParsedClassType ct, Collection<MemberInstance> members) {
		Collection<MemberInstance> abstractMembers = getMethodsFromAbstractParents(ct);

		// now check if this abstract member is in the list of things that we're including already
		// i.e. is it in members (?).  if it is not, then add ct's implementation of it to "members"
		Collection<MemberInstance> stubsToGenerate = new LinkedList<MemberInstance>(abstractMembers);

		for (MemberInstance ab : abstractMembers) {
			for(MemberInstance m : members) {
				if (m instanceof MethodInstance) {
					List<MethodInstance> overrides = ts.overrides((MethodInstance) m);
					if (overrides.contains(ab)) {
						// ab is covered by what we're going to do so far... let's not add a stub for it
						stubsToGenerate.remove(ab);
					}
				}
			}
		}
		return stubsToGenerate;
	}

	public void addInlineClass(InlineSignatureClass isc) {
		inlineClasses.add(isc);
	}

	public Collection<InlineSignatureClass> getInlineClasses() {
		return inlineClasses;
	}

	Collection<MemberInstance> instanceMembers = null;

	public Collection<MemberInstance> getInstanceMembers() {
		if (instanceMembers == null) {
			instanceMembers = new HashSet<MemberInstance>();
			for(InlineSignatureClass isc : inlineClasses) {
				for (SignatureElement se : isc.getSignatureElements()) {
					if (se instanceof SignatureMember) {
						instanceMembers.add(((SignatureMember) se).getMemberInstance());
					}
				}
			}
		}

		return instanceMembers;
	}
	
	Collection<ParsedClassType> instanceClasses = null;
	
	public Collection<ParsedClassType> getInstanceClasses() {
		if (instanceClasses == null) {
			instanceClasses = new HashSet<ParsedClassType>();
			for(InlineSignatureClass isc : inlineClasses) {
				instanceClasses.add(isc.getPolyglotClass());
			}
		}

		return instanceClasses;
	}
}
