package at.dms.kjc.raw;

import at.dms.kjc.sir.*;

/** 
 * This class keeps the counters for weights of the splitter/joiners
 * and performs the test to check whether the simulation is finished 
 */
public class SimulationCounter {
    
    private HashMap counts;
    
    public SimulationCounter() {
	counts = new HashMap();
    }

    /* traverse the given arc, <num> corresponds to the channel
       number
    */
    public void traverseSplitter(SIRSplitter splitter, int num) {
	if (SIRContainer instanceof SIRPipeline)
	    return;
	counts.get(splitter
	
	
