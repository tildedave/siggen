/******************************************************************** 
 * File          : SignatureCompiler.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import polyglot.ext.siggen.SignatureOptions;
import polyglot.util.InternalCompilerError;

public class SignatureCompiler {

	public boolean compileSignatures(Collection<File> filesToCompile) {
		StringBuffer compileFileString = new StringBuffer();
		for(File f : filesToCompile) {
			try {
				String fileAndPath = f.getCanonicalPath();
				// TODO: do this right
				if (fileAndPath.endsWith("java/lang/Object.jif")) {
					compileFileString = new StringBuffer(fileAndPath + " " + compileFileString.toString());
				}
				else
					compileFileString.append(fileAndPath + " ");
			} catch (IOException e) {
				throw new InternalCompilerError("IOException getting path of " + f);
			}
		}
		
		SignatureOptions sigOptions = (SignatureOptions) SignatureOptions.global; 
		String jifc = sigOptions.jifc;
		String sigOutputClassPath = sigOptions.signature_class_path;
		
		String compileCommand = jifc + " -d " + sigOutputClassPath + " " + compileFileString;
		// TODO: put these into an array -- might be more secure?  not sure
		try {
			System.err.println("executing: " + compileCommand);
			Process p = Runtime.getRuntime().exec(compileCommand);
			InputStream stderr = p.getErrorStream();
			InputStreamReader isr = new InputStreamReader(stderr);
			BufferedReader br = new BufferedReader(isr);
			
			String line = null;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			throw new InternalCompilerError("IOException compiling signatures");
		}
		
		return false;
	}
	
}
