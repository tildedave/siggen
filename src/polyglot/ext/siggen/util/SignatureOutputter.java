/******************************************************************** 
 * File          : SignatureGenerator.java
 * Project       : siggen
 * Description   : Writes out signatures to files
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:31:56 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */

package polyglot.ext.siggen.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.TreeSet;

import polyglot.ext.siggen.SignatureOptions;
import polyglot.ext.siggen.reflect.InlineSignatureClass;
import polyglot.ext.siggen.reflect.SignatureClass;
import polyglot.ext.siggen.reflect.SignatureImport;
import polyglot.frontend.ExtensionInfo;
import polyglot.main.Version;
import polyglot.types.SourceClassResolver;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;

public class SignatureOutputter {

	protected ClassLoader cl;

	protected int varCounter = 0;
	protected ExtensionInfo info;
	protected SourceClassResolver noRawClassResolver;
	protected boolean shouldOutputInlineClasses;

	protected Version version;

	protected ErrorQueue eq;


	public SignatureOutputter(SourceClassResolver noRawClassResolver,
			Version version, ErrorQueue eq, boolean shouldOutputInlineClasses) {
		this.cl = ClassLoader.getSystemClassLoader();
		this.noRawClassResolver = noRawClassResolver;
		this.version = version;
		this.shouldOutputInlineClasses = shouldOutputInlineClasses;
		this.eq = eq;
	}

	public File generateSignature(SignatureClass c) {
		String fileContentString = getFileContentString(c);

		String outputPathName = getOutputPathForSignatureClass(c);
		String outputFileName = getOutputFileForSignatureClass(c,
				outputPathName);
		
		File outputPath = new File(outputPathName);
		File outputFile = new File(outputFileName);
		FileOutputStream fos;
		try {
			if (!outputPath.exists()) {
				boolean pathSuccess = outputPath.mkdirs();
				if (!pathSuccess)
					throw new InternalCompilerError("problem creating path "
							+ outputPath);
			}

			if (!outputFile.exists()) {
				boolean fileSuccess = outputFile.createNewFile();
				if (!fileSuccess)
					throw new InternalCompilerError("problem creating file "
							+ outputFile);
			}

			fos = new FileOutputStream(outputFile);
			PrintStream fps = new PrintStream(fos);
			fps.println(fileContentString);
			fps.close();
		} catch (FileNotFoundException e) {
			throw new InternalCompilerError("file " + outputFile
					+ " did not exist");
		} catch (IOException e) {
			throw new InternalCompilerError("IOException creating "
					+ outputFile);
		}

		return outputFile;
	}

	protected String getOutputFileForSignatureClass(SignatureClass c,
			String outputPathName) {
		String className = c.getShortNameForClass();
		return outputPathName + className + ".jif";
	}

	protected String getFileContentString(SignatureClass c) {
		Collection<SignatureImport> importTypes = new LinkedHashSet<SignatureImport>();
		String classString = c.getElementString(importTypes);
		String packageString = c.getPackageString();
		String importString = "";

		Collection<String> sortedImportTypes = new TreeSet<String>();
		for (SignatureImport si : importTypes) {
			String siString = si.getElementString(null);
			sortedImportTypes.add(siString);
		}
		for (String s : sortedImportTypes) {
			importString += s + "\n";
		}

		if (!importString.equals(""))
			importString += "\n";

		String fileContentString = getSignatureCommentString(c) + packageString
				+ "\n" + importString + classString;
		return fileContentString;
	}

	protected String getOutputPathForSignatureClass(SignatureClass c) {
		String path;
		if (c instanceof InlineSignatureClass)
			path = ((SignatureOptions) SignatureOptions.global).inline_signature_source_path;
		else
			path = ((SignatureOptions) SignatureOptions.global).signature_source_path;
		String packageName = c.getPolyglotClass().package_().toString();
		String[] directoryNames = packageName.split("\\.");

		String outputFile = path;
		if (!outputFile.endsWith("/"))
			outputFile += "/";
		for (String dir : directoryNames) {
			outputFile += dir + "/";
		}
		return outputFile;
	}

	protected String getSignatureCommentString(SignatureClass c) {
		if (c instanceof InlineSignatureClass) {
			return "/*\n"
			 	+ "* This is an inline signature for a Java class.  It contains methods\n"
			 	+ "* and fields that use types within a specified subset of a larger\n"
			 	+ "* program that could not be included in the signatures.  These files\n"
			 	+ "* should be added to the codebase, which should be modified to use these\n" 
			 	+ "* classes as necessary.\n" + "*\n"
				+ "* Automatically generated with Siggen " + this.version
				+ ".\n"
				+ "* Available from: http://www.cse.psu.edu/~dhking/siggen\n"
				+ "*/\n";
		}
		return "/*\n"
				+ "* This is a Jif signature for a Java class. It provides Jif label\n"
				+ "* annotations for the Java class, allowing it to be usable by Jif\n"
				+ "* code. There is no automated check that the signature provided here\n"
				+ "* agrees with the actual Java code.\n" + "*\n"
				+ "* Automatically generated with Siggen " + this.version
				+ ".\n"
				+ "* Available from: http://www.cse.psu.edu/~dhking/siggen\n"
				+ "*/\n";
	}

	public File generateInlineSignature(SignatureClass sc) {
		if (sc.getInlineClass() != null && shouldOutputInlineClasses)
			return this.generateSignature(sc.getInlineClass());
		return null;
	}
}
