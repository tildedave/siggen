/******************************************************************** 
 * File          : SignatureSourceClassResolver.java
 * Project       : siggen
 * Description   : Customized SourceClassResolver
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:32:25 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */


package polyglot.ext.siggen.frontend;

import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.types.SourceClassResolver;
import polyglot.types.reflect.ClassFileLoader;

/**
 * SignatureSourceClassResolver is an extension of the SourceClassResolver.  
 * The basic problem is that when getting jif signature files, the default
 * SourceClassResolver will try and find class files of the same extension
 * (in this case, "siggen").  This resolver uses the signature extension 
 * info that the user has specified.
 * 
 * As an aside, the java version of siggen can read jif signature files, as 
 * by default, sigExt.version() will be the jl version, which is encoded into
 * jif signature files.
 * @author dhking
 *
 */

public class SignatureSourceClassResolver extends SourceClassResolver {

	public SignatureSourceClassResolver(Compiler compiler, 
			ExtensionInfo ext, ExtensionInfo sigExt,
			String classpath, ClassFileLoader loader, boolean allowRawClasses,
			boolean compileCommandLineOnly, boolean ignoreModTimes) {
		super(compiler, ext, classpath, loader, allowRawClasses,
				compileCommandLineOnly, ignoreModTimes);
		// use the signature's version information for finding signature files
		this.version = sigExt.version();
	}
}
