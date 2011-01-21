/******************************************************************** 
 * File          : SiggenVersion.java
 * Project       : siggen
 * Description   : Signature Version information
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:33:50 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */

package polyglot.ext.siggen;

import polyglot.main.Version;

public class SiggenVersion extends Version {

	@Override
	public int major() {
		return 0;
	}

	@Override
	public int minor() {
		return 2;
	}

	@Override
	public String name() {
		return "siggen";
	}

	@Override
	public int patch_level() {
		return 0;
	}

}
