package at.dms.kjc.sir.stats;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.partition.*;

import java.util.*;

/**
 *
 * This class gathers very basic properties about a program and prints
 * them to the screen.
 *
 * Written by Bill to generate benchmarks table in his Ph.D. thesis.
 */

public class BasicStats {
    // number of filters, pipelines, splitjoins, feedback loops
    int numFilters = 0;
    int numPipelines = 0;
    int numRRSplitjoins = 0;
    int numDupSplitjoins = 0;
    int numFeedbackloops = 0;
    // number of splitters, joiners with a zero-weight
    int splittersWithZero = 0;
    int joinersWithZero = 0;
    // number of peeking filters
    int numPeekingFilters = 0;
    // maximum peeking work
    long maxPeekingWork = 0;
    // number of filters with dynamic rates
    int numDynamicRates = 0;
    // number of stateful filters
    int numStatefulFilters = 0;
    // number of identity filters
    int numIdentityFilters = 0;
    // nuber of filters that push 1, pop 1, peek 1
    int numOneToOneFilters = 0;
    // number of filters with a multiplicity of 1 in the steady state
    int multiplicityOneFilters = 0;
    // names of stateful filters
    List stateful = new ArrayList();
    // names of peeking filters
    List peeking = new ArrayList();
    // dynamic rate filters
    List<SIRFilter> dynamicRateFilters = new ArrayList<SIRFilter>();
    // total work in the program
    long totalWork = 0;
    // total stateful work
    long totalStatefulWork = 0;
    // maximum stateful work
    long maxStatefulWork = 0;
    // name of filter with max stateful work
    SIRFilter maxStatefulFilter;
    // total work in feedback loops
    long totalFeedbackWork = 0;
    // list of multiplicities of filters (in the schedule)
    List multiplicities = new LinkedList();
    // unique multiplicities of filters (in the schedule)
    HashSet<Integer> uniqueMultiplicities = new HashSet<Integer>();

    /**
     * Gather statistics for <str>.
     */
    public static void doit(SIRStream str) {
        new BasicStats().collectBasicStats(str);
    }

    private void collectBasicStats(SIRStream str) {
        // don't deal with dynamic rates here
        SIRDynamicRateManager.pushConstantPolicy(1);

        // get a work estimate
        final WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        // dump graph
        work.printGraph(str, "stats-graph.dot");

        SIRDynamicRateManager.popPolicy();

        // now that graph has been dumped with all pipelines labeled,
        // remove redundant pipelines for the sake of later counts
        Lifter.liftPreservingSync(str);

        // gather counts of stream type
        IterFactory.createFactory().createIter(str).accept((new EmptyStreamVisitor() {
                public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {
                    numPipelines++;
                }
                
                public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {
                    if (self.getSplitter().getType().isDuplicate()) {
                        numDupSplitjoins++;
                    } else {
                        numRRSplitjoins++;
                        // test for zero splitter weights
                        int[] weights = self.getSplitter().getWeights();
                        for (int i=0; i<weights.length; i++) {
                            if (weights[i]==0) {
                                splittersWithZero++;
                                break;
                            }
                        }
                    }
                    // test for zero joiner weights
                    int[] weights = self.getJoiner().getWeights();
                    for (int i=0; i<weights.length; i++) {
                        if (weights[i]==0) {
                            joinersWithZero++;
                            break;
                        }
                    }                    
                }
                
                public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {
                    numFeedbackloops++;
                }
                
                public void visitFilter(SIRFilter self, SIRFilterIter iter) {
                    numFilters++;
                    long myWork = work.getWork(self);
                    totalWork += myWork;
                    multiplicities.add(new Integer(work.getReps(self)));
                    uniqueMultiplicities.add(new Integer(work.getReps(self)));
                    if (work.getReps(self)==1) {
                        multiplicityOneFilters++;
                    }
                    // see if this filter is in a feedback loop
                    SIRContainer parent = self.getParent();
                    while (parent!=null) {
                        if (parent instanceof SIRFeedbackLoop) {
                            totalFeedbackWork += myWork;
                            break;
                        }
                        parent = parent.getParent();
                    }
                    // count identities
                    if (self instanceof SIRIdentity) {
                        numIdentityFilters++;
                    } else if (self.getPeek().isDynamic() || self.getPop().isDynamic() || self.getPush().isDynamic()) {
                        numDynamicRates++;
                        dynamicRateFilters.add(self);
                    }  else if (self.getPeekInt()==1 && self.getPushInt()==1) {
                            numOneToOneFilters++;
                    }
                    if (!self.getPeek().isDynamic() && !self.getPop().isDynamic()) {
                        // don't worry about checking peeking for dynamic-rate filters
                        if (self.getPeekInt()>self.getPopInt()) {
                            numPeekingFilters++;
                            peeking.add(self.getCleanIdent());
                            if (myWork > maxPeekingWork) {
                                maxPeekingWork = myWork;
                            }
                        }
                    }
                    if (StatelessDuplicate.hasMutableState(self)) {
                        // ignore source and sink nodes when
                        // considering state.  these nodes typically
                        // are generating input, or are checking
                        // output, and are not part of the
                        // computation.  They should be replaced with File I/O.
                        boolean COUNT_STATEFUL_SOURCES = false;
                        boolean COUNT_STATEFUL_SINKS = false;
                        if ((self.getPop().isDynamic() || self.getPopInt()>0 || COUNT_STATEFUL_SOURCES) && (self.getPush().isDynamic() || self.getPushInt()>0 || COUNT_STATEFUL_SINKS)) {
                            stateful.add(self.getCleanIdent());
                            numStatefulFilters++;
                            totalStatefulWork += myWork;
                            if (myWork>maxStatefulWork) {
                                maxStatefulWork = myWork;
                                maxStatefulFilter = self;
                            }
                        }
                    }
                }

            }));

        // calculate the min, median, max multiplicity
        Integer[] mults = (Integer[])multiplicities.toArray(new Integer[0]);
        Arrays.sort(mults);
        int minMult = mults[0].intValue();
        int medianMult = (mults.length%2==0 ? 
                          // even number of filters: median is average of middle two
                          (mults[(mults.length-1)/2].intValue()+mults[(mults.length+1)/2].intValue())/2 :
                          // odd number of filters: median is in the middle
                          (mults[(mults.length+1)/2].intValue()));
        int maxMult = mults[mults.length-1].intValue();

        // calculate the mode multiplicity and the number of filters having that multiplicity
        int modeFreq = 0;
        int modeMult = 0;
        int lastMult = -1;
        int curFreq = 0;
        for (int i=0; i<mults.length; i++) {
            if (mults[i] == lastMult) {
                // continue a trend
                curFreq++;
            } else {
                // start a new trend
                if (curFreq > modeFreq) {
                    modeFreq = curFreq;
                    modeMult = lastMult;
                }
                curFreq = 1;
                lastMult = mults[i];
            }
        }
        // check if last run was best
        if (curFreq > modeFreq) {
            modeFreq = curFreq;
            modeMult = lastMult;
        }
        
        // print out results
        double fractionStatefulTotal = (double)totalStatefulWork/(double)totalWork;
        double fractionStatefulMax = (double)maxStatefulWork/(double)totalWork;
        double fractionPeekingMax = (double)maxPeekingWork/(double)totalWork;
        double fractionFeedbackWork = (double)totalFeedbackWork/(double)totalWork;
        System.out.println("Static filters types: " + Kopi2SIR.numFilters);
        System.out.println("Rest of stats refer to dynamic filter instances, not static types...");
        System.out.println("Number of non-identity filters: " + (numFilters - numIdentityFilters));
        System.out.println("Number of identity filters: " + numIdentityFilters);
        System.out.println("Number of pipelines: " + numPipelines);
        System.out.println("Number of splitjoins (dup + RR = total): " + numDupSplitjoins + " + " + numRRSplitjoins + " = " + (numDupSplitjoins + numRRSplitjoins));
        System.out.println("Number of feedback loops: " + numFeedbackloops);
        System.out.println("Number of splitters with a zero-weight: " + splittersWithZero);
        System.out.println("Number of joiners with a zero-weight: " + joinersWithZero);
        System.out.println("Number of peeking filters: " + numPeekingFilters);
        System.out.println("Max work in peeking filter: " + fractionPeekingMax);
        System.out.println("Total work in feedback loops: " + fractionFeedbackWork);
        System.out.println("Number of dynamic-rate filters: " + numDynamicRates);
        System.out.println("Number of pop 1, peek 1, push 1 filters: " + numOneToOneFilters);
        System.out.println("Number of stateful filters: " + numStatefulFilters);
        System.out.println("Min / median / max multiplicity: " + minMult + " / " + medianMult + " / " + maxMult);
        System.out.println("Mode multiplicity: " + modeMult);
        System.out.println("Number of filters with mode multiplicity: " + modeFreq);
        System.out.println("Number of multiplicity-one filters: " + multiplicityOneFilters);
        System.out.println("Number of distinct multiplicities: " + uniqueMultiplicities.size());
        if (stateful.size()>0) {
            System.out.println("Total stateful work in graph (ignoring sources/sinks): " + fractionStatefulTotal + "%");
            System.out.println("Max stateful filter in single filter (ignoring sources/sinks): " + fractionStatefulMax + "%");
            System.out.println("Name of max stateful filter (ignoring sources/sinks): " + maxStatefulFilter.getIdent());
            System.out.println("Stateful filters (ignoring sources/sinks):");
            Collections.sort(stateful); // output in alphabetical order
            for (int i=0; i<stateful.size(); i++) {
                // don't print duplicates
                if (i==0 || (i>0 && !stateful.get(i).equals(stateful.get(i-1)))) {
                    System.out.println(" - " + stateful.get(i));
                }
            }
        } else {
            System.out.println("No stateful filters (ignoring sources/sinks)");
        }
        if (peeking.size()>0) {
            System.out.println("Peeking filters:");
            Collections.sort(peeking); // output in alphabetical order
            for (int i=0; i<peeking.size(); i++) {
                // don't print duplicates
                if (i==0 || (i>0 && !peeking.get(i).equals(peeking.get(i-1)))) {
                    System.out.println(" - " + peeking.get(i));
                }
            }
        }
        if (numDynamicRates>0) {
            System.out.println("Dynamic rate filters:");
            for (int i=0; i<dynamicRateFilters.size(); i++) {
                SIRFilter filter = dynamicRateFilters.get(i);
                System.out.println(" - " + filter.getCleanIdent() + 
                                   " push " +  (filter.getPush().isDynamic() ? ""+filter.getPush() : ""+filter.getPushInt()) +
                                   " pop " + (filter.getPop().isDynamic() ? ""+filter.getPop() : ""+filter.getPopInt()) +
                                   " peek " + (filter.getPeek().isDynamic() ? ""+filter.getPeek() : ""+filter.getPeekInt()));
            }
        }

        System.out.println();

        System.out.print("Static filter types" + ",");
        System.out.print("Non-identity filters" + ",");
        System.out.print("Identity filters" + ",");
        System.out.print("Pipelines" + ",");
        System.out.print("Splitjoins (dup)" + ",");
        System.out.print("Splitjoins (RR)" + ",");
        System.out.print("Feedback loops" + ",");
        System.out.print("Splitters with zero-weight" + ",");
        System.out.print("Joiners with zero-weight" + ",");
        System.out.print("Peeking filters" + ",");
        System.out.print("Max peeking work in single filter" + ",");
        System.out.print("Total work in feedback loops" + ",");
        System.out.print("Dynamic-rate filters" + ",");
        System.out.print("One-to-one filters" + ",");
        System.out.print("Stateful filters" + ",");
        System.out.print("Total stateful work in graph" + ",");
        System.out.print("Max stateful work in single filter" + ",");
        System.out.print("Min mult"  + ",");
        System.out.print("Median mult"  + ",");
        System.out.print("Max mult"  + ",");
        System.out.print("Mode multiplicity: " + ",");
        System.out.print("Filters with mode multiplicity: " + ",");
        System.out.print("Mult-one filters"  + ",");
        System.out.print("Number of distinct multiplicities" + ",");
        System.out.println();

        System.out.print(Kopi2SIR.numFilters + ",");
        System.out.print((numFilters - numIdentityFilters) + ",");
        System.out.print(numIdentityFilters + ",");
        System.out.print(numPipelines + ",");
        System.out.print(numDupSplitjoins + ",");
        System.out.print(numRRSplitjoins + ",");
        System.out.print(numFeedbackloops + ",");
        System.out.print(splittersWithZero + ",");
        System.out.print(joinersWithZero + ",");
        System.out.print(numPeekingFilters + ",");
        System.out.print(fractionPeekingMax + ",");
        System.out.print(fractionFeedbackWork + ",");
        System.out.print(numDynamicRates + ",");
        System.out.print(numOneToOneFilters + ",");
        System.out.print(numStatefulFilters + ",");
        System.out.print(fractionStatefulTotal + ",");
        System.out.print(fractionStatefulMax + ",");
        System.out.print(minMult + ",");
        System.out.print(medianMult + ",");
        System.out.print(maxMult + ",");
        System.out.print(modeMult + ",");
        System.out.print(modeFreq + ",");
        System.out.print(multiplicityOneFilters + ",");
        System.out.print(uniqueMultiplicities.size() + ",");
        System.out.println();

        // we're done
        System.exit(1);
    }
}
