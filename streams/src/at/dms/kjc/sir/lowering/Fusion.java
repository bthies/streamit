package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;
import streamit.scheduler.simple.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This fuses filters.
 */
public class Fusion {

    /**
     * Fuses f1 and f2 with common parent <parent>.  For now, assume
     * that:
     *  1. <f1> and <f2> have no control flow
     *  2. <f2> does not peek
     *  3. the init functions are empty (actually, adopts init function of #1)
     *  4. there are no name conflicts in fields or methods (just put
     *     'em together)
     *
     * Need to be careful with args of init function... should have
     * multiple init functions per fused filter?  fuse the init
     * functions, cascading their arguments?  need to adjust the
     * SIRInitStatements, too.
     * 
     */
    public static void fuse(SIRPipeline parent, final SIRFilter f1, 
			    final SIRFilter f2) {
	// make a scheduler
	Scheduler scheduler = new SimpleHierarchicalScheduler();
	// ask the scheduler to schedule <f1>, <f2>
	SchedPipeline sp = scheduler.newSchedPipeline(parent);
	sp.addChild(scheduler.newSchedFilter(f1, 
					     f1.getPushInt(), 
					     f1.getPopInt(),
					     f1.getPeekInt()));
	sp.addChild(scheduler.newSchedFilter(f2, 
					     f2.getPushInt(), 
					     f2.getPopInt(),
					     f2.getPeekInt()));
	scheduler.useStream(sp);
	Schedule schedule = scheduler.computeSchedule();
	// get the schedule -- expect it to be a list
	List schedList = (List)schedule.getSteadySchedule();
	//SIRScheduler.printSchedule(schedList, "two fused filters");
	// for now, assume we have the first one executing some number
	// of times -- get this number of times
	int count1 = 
	  ((SchedRepSchedule)schedList.get(0)).getTotalExecutions().intValue();
	int count2 =
	  ((SchedRepSchedule)schedList.get(1)).getTotalExecutions().intValue();

	// so now start building the new work function
	JBlock newStatements = new JBlock(null, new JStatement[0], null);
	// add the set of statements from <f1>, <count1> times
	for (int i=0; i<count1; i++) {
	    // get old work function
	    JMethodDeclaration old = f1.getWork();
	    // clone the function (we have to clone the function since
	    // we can only clone JPhylum objects; can't just clone the
	    // list of statements in the function.)
	    JMethodDeclaration oldClone 
		= (JMethodDeclaration)ObjectDeepCloner.deepCopy(old, false);
	    // add these statements to new body
	    newStatements.addAllStatements(0, oldClone.getStatements());
	}

	// change each push() statement into an assignment to a local var...
	// keep list of variable def's
	final List vars = new LinkedList();
	newStatements.accept(new SLIRReplacingVisitor() {
		public Object visitPushExpression(SIRPushExpression oldSelf,
						CType oldTapeType,
						JExpression oldArg) {
		    // do the recursing
		    SIRPushExpression self = (SIRPushExpression)
			super.visitPushExpression(oldSelf,
						  oldTapeType,
						  oldArg);

		    // define a variable to assign to
		    JVariableDefinition var = 
			new JVariableDefinition(/* where */ null,
						/* modifiers */ 0,
						/* type */ 
						self.getTapeType(),
						/* ident */ 
						LoweringConstants.
						getUniqueVarName(),
						/* initializer */
						null);

		    // add var to list of var's
		    vars.add(var);
		    
		    // replace with an assignment expression
		    return new JAssignmentExpression(null,
		    new JLocalVariableExpression(null, var),
						     self.getArg());
		}
	    });

	// add var decl's to newStatements
	for (ListIterator it = vars.listIterator(); it.hasNext(); ) {
	    // get var
	    JVariableDefinition var = (JVariableDefinition)it.next();
	    // make a declaration statement for our new variable
	    JVariableDeclarationStatement varDecl =
		new JVariableDeclarationStatement(null, var, null);
	    // add to newStatements
	    newStatements.addStatementFirst(varDecl);
	}

	// add the set of statements from <f2>, <count2> times
	JBlock newConsumers = new JBlock(null, new JStatement[0], null);
	for (int i=0; i<count2; i++) {
	    // get old work function
	    JMethodDeclaration old = f2.getWork();
	    // clone the function (we have to clone the function since
	    // we can only clone JPhylum objects; can't just clone the
	    // list of statements in the function.)
	    JMethodDeclaration oldClone 
		= (JMethodDeclaration)ObjectDeepCloner.deepCopy(old, false);
	    // add these statements to new body
	    newConsumers.addAllStatements(0, oldClone.getStatements());
	}

	// go through consumers, replacing pop's with references to
	// local vars...

	// keep a count of which var we're popping
	final int popCount = 0;
	// do the replacement
	newConsumers.accept(new SLIRReplacingVisitor() {
		public Object visitPopExpression(SIRPopExpression oldSelf,
						    CType oldTapeType) {
		    // visit super
		    SIRPopExpression self = 
			(SIRPopExpression)
			super.visitPopExpression(oldSelf, oldTapeType);
		    
		    // get the local var that we should be referencing
		    JLocalVariable var = (JLocalVariable)vars.get(popCount);
		    
		    // increment the pop count
		    popCount++;

		    // make local var references instead of popping
		    return new JLocalVariableExpression(null, var);
		}
	    });
	
	// add all the consumer statements to the new block
	newStatements.addAllStatements(newStatements.size(),
				       newConsumers.getStatements());

	// make a new work function for the fused filter
	JMethodDeclaration newWork = 
	    new JMethodDeclaration(null,
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   "work",
				   JFormalParameter.EMPTY,
				   CClassType.EMPTY,
				   newStatements,
				   null,
				   null);

	// copy fields
	JFieldDeclaration[] newFields = 
	    new JFieldDeclaration[f1.getFields().length + 
				  f2.getFields().length];
	for (int i=0; i<f1.getFields().length; i++) {
	    newFields[i] = f1.getFields()[i];
	}
	for (int i=0; i<f2.getFields().length; i++) {
	    newFields[i+f1.getFields().length] = f2.getFields()[i];
	}

	// copy methods.  subtract 4 to avoid init, work
	JMethodDeclaration[] newMethods = 
	    new JMethodDeclaration[f1.getMethods().length + 
				   f2.getMethods().length - 4];
	int j=0;
	for (int i=0; i<f1.getMethods().length; i++) {
	    if (f1.getMethods()[i]!=f1.getInit() &&
		f1.getMethods()[i]!=f1.getWork()) {
		j++;
		newMethods[j] = f1.getMethods()[i];
	    }
	}
	j=0;
	for (int i=0; i<f2.getMethods().length; i++) {
	    if (f2.getMethods()[i]!=f2.getInit() &&
		f2.getMethods()[i]!=f2.getWork()) {
		j++;
		newMethods[j+f1.getMethods().length] = f2.getMethods()[i];
	    }
	}

	// make a new filter to represent the fused combo
	final SIRFilter fused = new SIRFilter(f1.getParent(),
					"Fused_" + 
					f1.getIdent() + "_" + f2.getIdent(),
					newFields,
					newMethods,
					/* ASSUME PEEK=0 !! */ 
					new JIntLiteral(0),
					new JIntLiteral(f1.getPopInt()*count1),
				       new JIntLiteral(f2.getPushInt()*count2),
					newWork,
					f1.getInputType(),
					f2.getOutputType());

	// set init function to init function of first, arbitrarily (CHANGE)
	fused.setInit(f1.getInit());

	// replace <f1>..<f2> with <fused>
	parent.replace(f1, f2, fused);

	// replace the SIRInitStatements in the parent
	parent.getInit().accept(new SLIRReplacingVisitor() {
		public Object visitInitStatement(SIRInitStatement oldSelf,
						 JExpression[] oldArgs,
						 SIRStream oldTarget) {
		    // do the super
		    SIRInitStatement self = 
			(SIRInitStatement)
			super.visitInitStatement(oldSelf, oldArgs, oldTarget);
		    
		    // if we're f1, change target to be <fused>
		    if (self.getTarget()==f1) {
			self.setTarget(fused);
			return self;
		    } else if (self.getTarget()==f2) {
			// if we're f2, remove the init statement
			return new JEmptyStatement(null, null);
		    } else {
			// otherwise, return self
			return self;
		    }
		}
	    });
    }
}
