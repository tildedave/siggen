/******************************************************************** 
 * File          : GenerateSignaturePass.java
 * Project       : siggen
 * Description   : Polyglot pass that generates the needed signature files
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:32:51 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */

package polyglot.ext.siggen.frontend;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import polyglot.ext.siggen.SignatureOptions;
import polyglot.ext.siggen.reflect.PolyglotToJavaConverter;
import polyglot.ext.siggen.reflect.SignatureClass;
import polyglot.ext.siggen.util.PolyglotNamedComparator;
import polyglot.ext.siggen.util.SignatureCompiler;
import polyglot.ext.siggen.util.SignatureContents;
import polyglot.ext.siggen.util.SignatureGenerator;
import polyglot.ext.siggen.util.SignatureOutputter;
import polyglot.frontend.AbstractPass;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.goals.Goal;
import polyglot.main.Report;
import polyglot.types.ClassType;
import polyglot.types.MemberInstance;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.types.SourceClassResolver;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public class GenerateSignaturePass extends AbstractPass {

	protected SignatureContents sc;
	protected PolyglotToJavaConverter ptjc;
	protected SourceClassResolver noRawClassResolver;
	protected ExtensionInfo extInfo;
	protected ExtensionInfo sigExtInfo;

	public GenerateSignaturePass(Goal goal, SignatureContents sc, ExtensionInfo extInfo, ExtensionInfo sigExtInfo) {
		super(goal);
		this.sc = sc;
		this.extInfo = extInfo;
		this.sigExtInfo = sigExtInfo;
		this.ptjc = new PolyglotToJavaConverter();
	}

	@Override
	public boolean run() {
		try {
			createNoRawClassesResolver();
			//sc.printOutSignatures();

			List<ParsedClassType> undeclaredPolyglotClasses = new LinkedList<ParsedClassType>(sc.getUndeclaredClasses());
			Collections.sort(undeclaredPolyglotClasses, new PolyglotNamedComparator());
			SignatureOutputter so = 
				new SignatureOutputter(noRawClassResolver, 
						extInfo.version(), 
						extInfo.compiler().errorQueue(), 
						((SignatureOptions) SignatureOptions.global).should_output_inline_classes);
			SignatureGenerator sg = new SignatureGenerator(extInfo.typeSystem(), sc, noRawClassResolver);
			Collection<File> filesToCompile = new LinkedList<File>();

			for(ParsedClassType ct : undeclaredPolyglotClasses) {
				if (shouldGenerateSignatureForPolyglotClass(ct)) {
					Report.report(1, "generate signature for polyglot class: " + ct);

					SignatureClass sc = sg.getSignatureClass(ct);
					filesToCompile.add(so.generateSignature(sc));
					so.generateInlineSignature(sc);

					if (ct.isNested()) {
						System.err.println("changed " + ct + " to " + sc.getShortNameForClass() + " -- update your source file");
					}
				}
			}

			SignatureCompiler sigCompiler = new SignatureCompiler();
			sigCompiler.compileSignatures(filesToCompile);

			return true;
		}
		catch (Exception e) {
			System.err.println("got exception: " + e);
			e.printStackTrace();
			return false;
		}
	}

	private boolean shouldGenerateSignatureForPolyglotClass(ParsedClassType ct) {
		return isRawClass(ct) || !ct.package_().fullName().equals("org.lobobrowser.html.domimpl");
	}

	private Collection<MemberInstance> addStubsForAbstractParentsAndInterfaces(ParsedClassType ct,
			Collection<MemberInstance> members) {
		Collection<MemberInstance> stubs = new LinkedHashSet<MemberInstance>();


		if (ct.flags().isAbstract())
			return stubs;

		Collection<MemberInstance> abstractStubs = getStubsFromAbstractMembers(ct, members);
		Collection<MemberInstance> interfaceStubs = getStubsFromInterfaces(ct, members);

		stubs.addAll(abstractStubs);
		stubs.addAll(interfaceStubs);

		return stubs;
	}

	private Collection<MemberInstance> getStubsFromInterfaces(
			ParsedClassType ct, Collection<MemberInstance> members) {
		Collection<MemberInstance> instanceMembers = getMethodsFromInterfaces(ct);

		TypeSystem ts = extInfo.typeSystem();
		Collection<MemberInstance> stubsToGenerate = new LinkedList<MemberInstance>(instanceMembers);

		for (MemberInstance ab : instanceMembers) {
			for(MemberInstance m : members) {
				if (m instanceof MethodInstance) {
					List<MethodInstance> implemented = ts.implemented((MethodInstance) m);
					if (implemented.contains(ab)) {
						// ab is covered by what we're going to do so far... let's not add a stub for it
						stubsToGenerate.remove(ab);
					}
				}
			}
		}
		return stubsToGenerate;
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
			Collection<MemberInstance> pUsedMembers = sc.getMemberInstancesForClass((ParsedClassType) p);
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
			for(Type ct : ctInterfaces) {
				if (ct instanceof ClassType) {
					interfacesInSupertypes.add((ClassType) ct);
					interfacesInSupertypes.addAll(getInterfacesInSupertypes((ClassType) ((ClassType) ct).superType()));
				}
			}

			interfacesInSupertypes.addAll(getInterfacesInSupertypes((ClassType) t.superType()));
		}

		return interfacesInSupertypes;
	}

	private Collection<MemberInstance> getStubsFromAbstractMembers(
			ParsedClassType ct, Collection<MemberInstance> members) {
		Collection<MemberInstance> abstractMembers = getMethodsFromAbstractParents(ct);

		// now check if this abstract member is in the list of things that we're including already
		// i.e. is it in members (?).  if it is not, then add ct's implementation of it to "members"

		TypeSystem ts = extInfo.typeSystem();
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
			Collection<MemberInstance> pUsedMembers = sc.getMemberInstancesForClass((ParsedClassType) p);
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

	private boolean isRawClass(ParsedClassType ct) {
		String classFullName = ct.fullName();
		if (ct.isMember() && ct.container() instanceof ParsedClassType) {
			return isRawClass((ParsedClassType) ct.container());
		}
		else {
			return isRawClassString(classFullName);
		}
	}

	private boolean isRawClassString(String classFullName) {
		try {
			noRawClassResolver.find(classFullName);
			return false;
		} catch (SemanticException e) {
			if (e.getMessage().contains("A class file was found, but")) {
				// got a raw class error
				return true;
			}
			return false;
		}
	}


	private Collection<Class> getRawClassesInCollection(
			Collection<Class> undeclaredClasses) {
		Collection<Class> rawClasses = new LinkedList<Class>();
		for(Class ud : undeclaredClasses) {
			if (isRawClassString(ud.getCanonicalName()))
				rawClasses.add(ud);
		}

		return rawClasses;
	}


	private void createNoRawClassesResolver() {
		this.noRawClassResolver = 
			new SignatureSourceClassResolver(extInfo.compiler(), extInfo, 
					sigExtInfo, ((SignatureOptions) extInfo.getOptions()).constructSignatureClasspath(),
					extInfo.compiler().loader(), false, // no raw classes
					extInfo.getOptions().compile_command_line_only,
					extInfo.getOptions().ignore_mod_times);
	}
}
