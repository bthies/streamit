/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.library.iriter;

import streamit.library.Filter;

/**
 * This provides a view of filters that is fitting with the SDEP
 * calculation.  For details, see SDEPIterFactory.
 */
public class SDEPFilterIter extends FilterIter
{
    /**
     * SDEP factory with information on current SDEP calculation.
     */
    private SDEPIterFactory sdepFactory;

    SDEPFilterIter(Filter _filter, SDEPIterFactory _sdepFactory) {
        super(_filter, _sdepFactory);
        this.sdepFactory = _sdepFactory;
    }

    /**
     * Returns whether or not any phases of filter should be
     * considered for visibility.
     */
    private boolean phasesVisible() {
        return (sdepFactory.containsSender(filter) ||
                sdepFactory.containsReceiver(filter));
    }

    /**
     * Returns umber of visible init stages for current filter.
     * Currently multiple stages are visible only if they are in the
     * sender or receiver, and they are doing I/O in the direction of
     * the message.
     */
    int numInitStages = -1;
    public int getNumInitStages () {
        // cache result for speed
        if (numInitStages >= 0) {
            return numInitStages;
        }

        int orig = super.getNumInitStages();
        // if not the sender or receiver, then either 0 or 1 stage
        if (!(phasesVisible())) {
            // return 0 or 1, depending on if there is any initialization
            return (int)Math.min(orig, 1);
        }

        // figure out if we should count pushes or pops as stages
        boolean countPop = (sdepFactory.containsReceiver(filter) && sdepFactory.isDownstream() ||
                            sdepFactory.containsSender(filter) && !sdepFactory.isDownstream());

        // count how many stages are eligible
        int count = 0;
        for (int i=0; i<orig; i++) {
            // count popping stages where appropriate
            if (countPop && super.getInitPopStage(i)>0) count++;
            // count pushing stages where appropriate
            if (!countPop && super.getInitPushStage(i)>0) count++;
        }
        
        numInitStages = count;
        return numInitStages;
    }
    /**
     * Returns umber of visible work phases for current filter.
     * Currently multiple phases are visible only if they are in the
     * sender or receiver, and they are doing I/O in the direction of
     * the message.
     */
    int numWorkPhases = -1;
    public int getNumWorkPhases () {
        // cache result for speed
        if (numWorkPhases >= 0) {
            return numWorkPhases;
        }

        int orig = super.getNumWorkPhases();
        // if not the sender or receiver, then return 1
        if (!(phasesVisible())) {
            // assume that all filters have at least 1 work phase
            return 1;
        }

        // figure out if we should count pushes or pops as phases
        boolean countPop = (sdepFactory.containsReceiver(filter) && sdepFactory.isDownstream() ||
                            sdepFactory.containsSender(filter) && !sdepFactory.isDownstream());

        // count how many phases are eligible
        numWorkPhases = 0;
        for (int i=0; i<orig; i++) {
            // count popping phases where appropriate
            if (countPop && super.getPopPhase(i)>0) numWorkPhases++;
            // count pushing phases where appropriate
            if (!countPop && super.getPushPhase(i)>0) numWorkPhases++;
        }
        
        return numWorkPhases;
    }
        
    /**
     * Assuming that some of the stages of the current filter are
     * visible, returns the index of the <pre>target</pre>'th visible init stage.
     * For now, only see stages that do I/O in the direction of the
     * message.
     */
    private int visibleInitStage(int target) {
        // figure out if we should count pushes or pops as stages
        boolean countPop = (sdepFactory.containsReceiver(filter) && sdepFactory.isDownstream() ||
                            sdepFactory.containsSender(filter) && !sdepFactory.isDownstream());

        // count of visible stage we have found
        int count = -1; 
        // index in terms of original phases
        int index=0;
        while (true) {
            // count popping stages where appropriate
            if (countPop && super.getInitPopStage(index)>0) count++;
            // count pushing stages where appropriate
            if (!countPop && super.getInitPushStage(index)>0) count++;
            // quit when we reach the desired <pre>target</pre> 
            if (count == target) break;
            index++;
        }
        
        return index;
    }
    /**
     * Assuming that some of the phases of the current filter are
     * visible, returns the index of the <pre>target</pre>'th visible steady
     * phase.  For now, only see phases that do I/O in the direction
     * of the message.
     */
    private int visibleWorkPhase(int target) {
        // figure out if we should count pushes or pops as phases
        boolean countPop = (sdepFactory.containsReceiver(filter) && sdepFactory.isDownstream() ||
                            sdepFactory.containsSender(filter) && !sdepFactory.isDownstream());

        // count of visible phases we have found
        int count = -1; 
        // index in terms of original phases
        int index=0;
        while (true) {
            // count popping phases where appropriate
            if (countPop && super.getPopPhase(index)>0) count++;
            // count pushing phases where appropriate
            if (!countPop && super.getPushPhase(index)>0) count++;
            // quit when we reach the desired <pre>target</pre> 
            if (count == target) break;
            index++;
        }
        
        return index;
    }
   
    public int getInitPeekStage (int stage) {
        if (phasesVisible()) {
            // return phase if applicable
            assert getNumInitStages() > stage;
            return super.getInitPeekStage (visibleInitStage(stage));
        } else {
            // otherwise return aggregate I/O
            return filter.getInitPeekSummary();
        }
    }

    public int getInitPopStage (int stage) {
        if (phasesVisible()) {
            // return phase if applicable
            assert getNumInitStages() > stage;
            return super.getInitPopStage (visibleInitStage(stage));
        } else {
            // otherwise return aggregate I/O
            return filter.getInitPopSummary();
        }
    }
    
    public int getInitPushStage (int stage) {
        if (phasesVisible()) {
            // return phase if applicable
            assert getNumInitStages() > stage;
            return super.getInitPushStage (visibleInitStage(stage));
        } else {
            // otherwise return aggregate I/O
            return filter.getInitPushSummary();
        }
    }
    
    public Object getInitNameStage (int stage) {
        if (phasesVisible()) {
            // return phase if applicable
            assert getNumInitStages() > stage;
            return super.getInitFunctionStage (visibleInitStage(stage));
        } else {
            // otherwise return aggregate I/O
            return filter.getInitNameSummary();
        }
    }
    
    public int getPeekPhase (int phase) {
        if (phasesVisible()) {
            // return phase if applicable
            assert getNumWorkPhases() > phase;
            return super.getPeekPhase (visibleWorkPhase (phase));
        } else {
            // otherwise return aggregate I/O
            return filter.getSteadyPeekSummary();
        }
    }
    
    public int getPopPhase (int phase) {
        if (phasesVisible()) {
            // return phase if applicable
            assert getNumWorkPhases() > phase;
            return super.getPopPhase (visibleWorkPhase (phase));
        } else {
            // otherwise return aggregate I/O
            return filter.getSteadyPopSummary();
        }
    }
    
    public int getPushPhase (int phase) {
        if (phasesVisible()) {
            // return phase if applicable
            assert getNumWorkPhases() > phase : "Requesting phase " + phase + " of " + filter;
            return super.getPushPhase (visibleWorkPhase (phase));
        } else {
            // otherwise return aggregate I/O
            return filter.getSteadyPushSummary();
        }
    }

    public Object getWorkNamePhase (int phase) {
        if (phasesVisible()) {
            // return phase if applicable
            assert getNumWorkPhases() > phase;
            return super.getWorkFunctionPhase (visibleWorkPhase (phase));
        } else {
            // otherwise return aggregate I/O
            return filter.getSteadyNameSummary();
        }
    }
}
