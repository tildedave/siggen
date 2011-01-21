/******************************************************************** 
 * File          : DetermineUsedInstancesGoal.java
 * Project       : siggen
 * Description   : Goal to determine the types and methods to include in signatures
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:33:19 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */

package polyglot.ext.siggen.frontend;

import polyglot.ast.NodeFactory;
import polyglot.ext.siggen.util.SignatureContents;
import polyglot.ext.siggen.visit.DetermineUsedInstancesVisitor;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Barrier;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;
import polyglot.types.TypeSystem;

public class DetermineUsedInstancesGoal extends Barrier implements Goal {

	protected TypeSystem ts;
	protected NodeFactory nf;
	protected SignatureContents sc;
	
	public DetermineUsedInstancesGoal(Scheduler scheduler, TypeSystem ts, NodeFactory nf, SignatureContents sc) {
		super(scheduler);
		this.ts = ts;
		this.nf = nf;
		this.sc = sc;
	}

	@Override
	public Goal goalForJob(Job job) {
		return scheduler.internGoal(new VisitorGoal(job, new DetermineUsedInstancesVisitor(job, ts, nf, sc)));
	}
}
