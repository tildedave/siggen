/******************************************************************** 
 * File          : GenerateSignaturesGoal.java
 * Project       : siggen
 * Description   : Goal to generate signatures
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:32:37 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */

package polyglot.ext.siggen.frontend;

import java.util.Collection;
import java.util.Map;

import polyglot.ext.siggen.util.SignatureContents;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Pass;
import polyglot.frontend.goals.AbstractGoal;
import polyglot.frontend.goals.Goal;
import polyglot.types.MemberInstance;
import polyglot.types.ParsedClassType;

public class GenerateSignaturesGoal extends AbstractGoal implements Goal {

	private Collection<ParsedClassType> signaturesToGenerate;
	protected Map<ParsedClassType, Collection<MemberInstance>> signatureContents;
	protected SignatureContents sc;

	public GenerateSignaturesGoal(Collection<ParsedClassType> signaturesToGenerate, Map<ParsedClassType, Collection<MemberInstance>> signatureContents, SignatureContents sc) {
		super(null);
		this.signaturesToGenerate = signaturesToGenerate;
		this.signatureContents = signatureContents;
		this.sc = sc;
	}

	@Override
	public Pass createPass(ExtensionInfo extInfo) {
		return new GenerateSignaturePass(this, sc, extInfo, ((polyglot.ext.siggen.ExtensionInfo) extInfo).getSignatureExtInfo());
	}

}
