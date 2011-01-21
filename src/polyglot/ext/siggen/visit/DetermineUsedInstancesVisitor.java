/******************************************************************** 
 * File          : DetermineUsedInstancesVisitor.java
 * Project       : siggen
 * Description   : Determines the types and methods we should output in signatures
 * Author(s)     : dhking
 *  
 * Created       : Nov 30, 2007 4:32:08 PM
 *  Copyright (c) 2007 The Pennsylvania State University
 *  Systems and Internet Infrastructure Security Laboratory
 ********************************************************************
 */


package polyglot.ext.siggen.visit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.ClassDecl;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Import;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.ProcedureCall;
import polyglot.ast.TypeNode;
import polyglot.ext.siggen.SignatureOptions;
import polyglot.ext.siggen.util.SignatureContents;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.MethodInstance;
import polyglot.types.Named;
import polyglot.types.ParsedClassType;
import polyglot.types.ProcedureInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.TypedList;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

public class DetermineUsedInstancesVisitor extends ContextVisitor {

	protected SignatureContents sc;
	
	public DetermineUsedInstancesVisitor(Job job, TypeSystem ts, NodeFactory nf, SignatureContents sc) {
		super(job, ts, nf);
		this.sc = sc;
	}
	
	@Override
	public Node leaveCall(Node parent, Node old, Node n, NodeVisitor v) throws SemanticException {
		Context A = context();

		if (n instanceof ProcedureCall) {
			ProcedureCall pc = (ProcedureCall) n;
			sc.addProcedureInstance(pc.procedureInstance());
/*			if (shouldParameterize() && procedureCallReliesOnSubtyping(pc)) {
				System.err.println("should modify call to " + pc.procedureInstance());
				ProcedureInstance pi = constructorProcedureInstanceFromCall(pc);
				System.err.println("new procedure instance: " + pi);
				sc.addProcedureInstance(pi);
			}
*/		}
		if (n instanceof Field) {
			Field f = (Field) n;
			sc.addFieldInstance(f.fieldInstance());
		}
		if (n instanceof ClassDecl) {
			ClassDecl cd = (ClassDecl) n;
			sc.addClassDeclaration(cd.type());
		}
		if (n instanceof TypeNode) {
			TypeNode tn = (TypeNode) n;
			if (tn.type() instanceof ParsedClassType)
				sc.addParsedClassType((ParsedClassType) tn.type());
		}
		if (n instanceof Import) {
			Import i = (Import) n;
			if (i.kind().equals(Import.CLASS)) {
				Named t = ts.forName(i.name());
				if (t instanceof ParsedClassType)
					sc.addParsedClassType((ParsedClassType) t);
			}
		}

		return super.leaveCall(parent, old, n, v);
	}
/*
	private ProcedureInstance constructorProcedureInstanceFromCall(
			ProcedureCall pc) {
		ProcedureInstance pi = pc.procedureInstance();
		List<Type> argTypes = new LinkedList<Type>();
		TypedList.check(pc.arguments(), Expr.class);
		Iterator<Expr> exprIterator = pc.arguments().iterator();
		int i = 0;
		while(exprIterator.hasNext()) {
			Type type = exprIterator.next().type();
			if (shouldParameterize(type)) {
				argTypes.add(type);
			}
			else {
				argTypes.add((Type) pi.formalTypes().get(i));
			}
			++i;
		}
		
		if (pi instanceof MethodInstance) {
			MethodInstance mi = (MethodInstance) pi;
			return ts.methodInstance(mi.position(), mi.container(), mi.flags(), mi.returnType(), mi.name(), argTypes, mi.throwTypes());
		}
		if (pi instanceof ConstructorInstance) {
			ConstructorInstance ci = (ConstructorInstance) pi;
			return ts.constructorInstance(ci.position(), (ClassType) ci.container(), ci.flags(), argTypes, ci.throwTypes());
		}
		
		throw new InternalCompilerError("pi: " + pi + " not a MethodInstance or ConstructorInstance: " + pi.getClass());
	}

	private boolean procedureCallReliesOnSubtyping(ProcedureCall pc) {
		// args should be expressions
		TypedList.check(pc.arguments(),Expr.class);
		Iterator<Expr> exprIterator = pc.arguments().iterator();
		Iterator<Type> formalIterator = pc.procedureInstance().formalTypes().iterator();
		while(exprIterator.hasNext()) {
			Expr e = exprIterator.next();
			if (!e.type().equals(formalIterator.next()))
				return true;
		}
		return false;
	}

	private boolean shouldParameterize() {
		return ((SignatureOptions) SignatureOptions.global).should_parameterize;
	}
*/
}
