/******************************************************************** 
 * File          : ExtensionInfo.java
 * Project       : siggen
 * Description   : Main class for siggen.
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:34:00 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */

package polyglot.ext.siggen;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import polyglot.ast.NodeFactory;
import polyglot.ext.siggen.frontend.DetermineUsedInstancesGoal;
import polyglot.ext.siggen.frontend.GenerateSignaturesGoal;
import polyglot.ext.siggen.frontend.SignatureSourceClassResolver;
import polyglot.ext.siggen.util.SignatureContents;
import polyglot.frontend.Compiler;
import polyglot.frontend.CyclicDependencyException;
import polyglot.frontend.FileSource;
import polyglot.frontend.Job;
import polyglot.frontend.Parser;
import polyglot.frontend.goals.Goal;
import polyglot.main.Options;
import polyglot.main.Version;
import polyglot.types.LoadedClassResolver;
import polyglot.types.MemberInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;

public class ExtensionInfo extends polyglot.frontend.JLExtensionInfo {
	
	protected polyglot.frontend.ExtensionInfo sigExtensionInfo;

	protected SignatureContents sc;
	protected Collection<ParsedClassType> signaturesToGenerate;// = new HashSet<Import>();
	protected Map<ParsedClassType, Collection<MemberInstance>> signatureContents;

	protected Goal generateSignaturesGoal = null;
	protected Goal determineInstancesGoal = null;

    public ExtensionInfo() {
	}
    
	@Override
	public String compilerName() {
		return "siggen";
	}

	@Override
	protected NodeFactory createNodeFactory() {
		if (sigExtensionInfo == null)
			createSignatureExtensionInfo();
		return sigExtensionInfo.nodeFactory();
	}

	@Override
	protected TypeSystem createTypeSystem() {
		if (sigExtensionInfo == null)
			createSignatureExtensionInfo();
		return sigExtensionInfo.typeSystem();
	}

	@Override
	public String defaultFileExtension() {
		if (sigExtensionInfo == null)
			createSignatureExtensionInfo();
		return sigExtensionInfo.defaultFileExtension();
	}

    private void generateGoals() {
    	if (generateSignaturesGoal == null || determineInstancesGoal == null) {
    		generateSignaturesGoal = scheduler().internGoal(new GenerateSignaturesGoal(signaturesToGenerate, signatureContents, sc));
    		determineInstancesGoal = scheduler().internGoal(new DetermineUsedInstancesGoal(scheduler(), typeSystem(), nodeFactory(), sc));
    		
    		scheduler.addGoal(generateSignaturesGoal);
    		try {
				scheduler.addPrerequisiteDependency(generateSignaturesGoal, determineInstancesGoal);
			} catch (CyclicDependencyException e) {
				throw new InternalCompilerError(e);
			}
    	}
    }
	
	@Override
	public Goal getCompileGoal(Job job) {
		generateGoals();
		// do up through type checking
		List<Goal> l = new ArrayList<Goal>();

        l.add(scheduler.TypeChecked(job));
        l.add(scheduler.ConstantsChecked(job));
        // then determine the instances
        l.add(scheduler.internGoal(this.determineInstancesGoal));
        // then output the signatures
//        l.add(generateSignaturesGoal);

        try {
            scheduler.addPrerequisiteDependencyChain(l);
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        
        return this.generateSignaturesGoal;
	}

	@Override
	protected void initTypeSystem() {
		// type system should be initialized in sigExtensionInfo already
		// need to re-initialize it with a signature-tolerant extension info (!)
        try {
            LoadedClassResolver lr;
            lr = new SignatureSourceClassResolver(compiler, this, 
                    sigExtensionInfo, ((SignatureOptions) getOptions()).constructSignatureClasspath(),
                    compiler.loader(), true, // allow raw classes!
		    getOptions().compile_command_line_only,
                    getOptions().ignore_mod_times);
            ts.initialize(lr, this);
            this.sc = new SignatureContents(ts);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(
                "Unable to initialize type system: ", e);
        }
	}

	@Override
	public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
		if (sigExtensionInfo == null)
			createSignatureExtensionInfo();
		return sigExtensionInfo.parser(reader, source, eq);
	}

	@Override
	protected Options createOptions() {
		SignatureOptions theOptions = new SignatureOptions(this);
		theOptions.post_compiler = null;
		return theOptions;
	}	

	protected void createSignatureExtensionInfo() {
		SignatureOptions sigOptions = (SignatureOptions) getOptions();
		String sigExtInfoClassName = sigOptions.sigExtensionInfo;
		{
			try {
				Class<ExtensionInfo> sigExtInfoClass = (Class<polyglot.ext.siggen.ExtensionInfo>) ClassLoader.getSystemClassLoader().loadClass(sigExtInfoClassName);
				this.sigExtensionInfo = sigExtInfoClass.newInstance();
				System.err.println("successfully loaded " + sigExtensionInfo);
			}
			catch (ClassNotFoundException e) {
				throw new InternalCompilerError("could not load signature class: " + e);
			} catch (Exception e) {
				throw new InternalCompilerError("got exception while loading signature class: " + e);
			}
		}
	}
	
	@Override
	public Version version() {
		return new SiggenVersion();
	}	
	
	@Override
	public void initCompiler(Compiler compiler) {
		if (sigExtensionInfo == null)
			createSignatureExtensionInfo();
		
		super.initCompiler(compiler);
		// initialize our signature's extension info as well
		sigExtensionInfo.initCompiler(compiler);
		// re-init type system (want to allow raw classes)
		initTypeSystem();
	}

	public polyglot.frontend.ExtensionInfo getSignatureExtInfo() {
		return sigExtensionInfo;
	}
}
