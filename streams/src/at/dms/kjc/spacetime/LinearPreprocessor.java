package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;

/**
 * Replaces linear filters with two stage filters that
 * have and initWork that peeks 2(peek-pop) and pops (peek-pop)
 * Important for linear codegen
 */
public class LinearPreprocessor extends EmptyStreamVisitor {
    private LinearAnalyzer lfa;

    public LinearPreprocessor(LinearAnalyzer lfa) {
	this.lfa=lfa;
    }

    public void visitFilter(SIRFilter self,SIRFilterIter iter) {
	LinearFilterRepresentation linRep=lfa.getLinearRepresentation(self);
	if(linRep!=null) { //If linear
	    System.out.println("Found Linear Filter: "+self);
	    //Create new filter and copy state
	    SIRTwoStageFilter newFilter=new SIRTwoStageFilter();
	    newFilter.copyState(self);
	    //Replace old filter
	    SIRContainer parent=self.getParent();
	    parent.set(parent.indexOf(self),newFilter);
	    //Update linear analyzer
	    lfa.addLinearRepresentation(newFilter,linRep);
	    lfa.removeLinearRepresentation(self);
	    //Set initWork peek,pop,push amount
	    SIRWorkFunction init=newFilter.getInitPhases()[0];
	    SIRWorkFunction steady=self.getPhases()[0];
	    int peek=steady.getPeekInt();
	    int pop=steady.getPopInt();
	    int push=steady.getPushInt();
	    int diff=peek-pop;
	    init.setPeek(new JIntLiteral(2*diff));
	    init.setPop(new JIntLiteral(diff));
	    init.setPush(new JIntLiteral((peek/pop-1)*push));
	    System.out.println("Steady: "+steady.getPeek()+" "+steady.getPop()+" "+steady.getPush());
	    System.out.println("Init: "+init.getPeek()+" "+init.getPop()+" "+init.getPush());
	}
    }
}
