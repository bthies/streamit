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
     */
    public static void fuse(SIRPipeline parent, SIRFilter f1, SIRFilter f2) {
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
	// for now, assume we have the first one executing some number
	// of times -- get this number of times
	int count1 = 0;
	while (schedList.get(count1)==f1) {
	    count1++;
	}
	// then count2 is just however many are left
	int count2 = schedList.size()-count1;

	// so now start building the new work function
	JBlock newStatements = new JBlock(null, new JStatement[0], null);
	// add the set of statements from <f1>, <count1> times
	for (int i=0; i<count1; i++) {
	    // get old statement list
	    List old = f1.getWork().getStatements();
	    // clone the list
	    List oldClone = (List)ObjectDeepCloner.deepCopy(old);
	    // add these statements to new body
	    newStatements.addAllStatements(0, oldClone);
	}

	// change each push() statement into an assignment to a local var
	
	
	// add the set of statements from <f2>, <count2> times
    }
}
