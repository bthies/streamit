/**
 * 
 */
package at.dms.kjc.smp;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.LevelizeSliceGraph;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

import java.util.HashMap;

/**
 * @author mgordon
 *
 */
public class SMPBackEndFactory extends BackEndFactory<SMPMachine, Core, CoreCodeStore, Integer> {

    private SMPMachine chip;
    private SMPBackEndScaffold scaffold;

    /** scheduler used by backend */
    private static Scheduler scheduler;
    /** splits the slicegraph into levels */
    private static LevelizeSliceGraph lsg; 
    /** the number of filters that we have yet to process from a level the init stage */
    private static HashMap<Integer, Integer> levelLeftToProcessInit;
    /** the number of filters that we have yet to process from a level the init stage */
    private static HashMap<Integer, Integer> levelLeftToProcessPP;
    
    public SMPBackEndFactory(SMPMachine chip, Scheduler scheduler) {
        this.chip = chip;
        this.scheduler = scheduler;
        this.setLayout(scheduler);

        if (scheduler.isTMD()) {
        	//levelize the slicegraph
        	lsg = new LevelizeSliceGraph(scheduler.getGraphSchedule().getSlicer().getTopSlices());

            //fill the left to process maps with the number of filters in a level
            levelLeftToProcessInit = new HashMap<Integer, Integer>();
            levelLeftToProcessPP = new HashMap<Integer, Integer>();

        	Slice[][] levels = lsg.getLevels();
            for (int i = 0; i < levels.length; i++) {
                levelLeftToProcessInit.put(i, levels[i].length);
                levelLeftToProcessPP.put(i, levels[i].length);
            }
        }

        scaffold = new SMPBackEndScaffold();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getBackEndMain()
     */
    @Override
    public SMPBackEndScaffold getBackEndMain() {
        return scaffold;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getChannel(at.dms.kjc.slicegraph.Edge)
     */
    @Override
    public Channel getChannel(Edge e) {
        // TODO Auto-generated method stub
    	assert false;
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getChannel(at.dms.kjc.slicegraph.SliceNode, at.dms.kjc.slicegraph.SliceNode)
     */
    @Override
    public Channel getChannel(SliceNode src, SliceNode dst) {
        // TODO Auto-generated method stub
    	assert false;
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getCodeStoreHelper(at.dms.kjc.slicegraph.SliceNode)
     */
    @Override
    public CodeStoreHelper getCodeStoreHelper(SliceNode node) {
        // TODO Auto-generated method stub
        if (node instanceof FilterSliceNode) {
            // simply do appropriate wrapping of calls...
            return new FilterCodeGeneration((FilterSliceNode)node,this);
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getComputeCodeStore(at.dms.kjc.backendSupport.ComputeNode)
     */
    @Override
    public CoreCodeStore getComputeCodeStore(Core parent) {
        return parent.getComputeCode();
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getComputeNode(java.lang.Object)
     */
    @Override
    public Core getComputeNode(Integer coreNum) {
        return chip.getNthComputeNode(coreNum.intValue());
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getComputeNodes()
     */
    @Override
    public SMPMachine getComputeNodes() {
        return chip;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#processFilterSliceNode(at.dms.kjc.slicegraph.FilterSliceNode, at.dms.kjc.backendSupport.SchedulingPhase, at.dms.kjc.backendSupport.ComputeNodesI)
     */
    @Override
    public void processFilterSliceNode(FilterSliceNode filter,
            SchedulingPhase whichPhase, SMPMachine chip) {

        if (filter.isPredefined()) {
            if (filter.isFileInput())
                (new ProcessFileReader(filter, whichPhase, this)).processFileReader();
            else if (filter.isFileOutput()) {
                (new ProcessFileWriter(filter, whichPhase, this)).processFileWriter();
            }
        } 
        else {
            if(KjcOptions.sharedbufs && FissionGroupStore.isFizzed(filter.getParent())) {
                for(Slice slice : FissionGroupStore.getFizzedSlices(filter.getParent())) {
                    (new ProcessFilterSliceNode(slice.getFirstFilter(), whichPhase, this)).processFilterSliceNode();
                }
            }
            else {
                (new ProcessFilterSliceNode(filter, whichPhase, this)).processFilterSliceNode();
            }

            if (scheduler.isTMD()) {
                //if we are using the tmd scheduler we have to add barriers between each 
                //init/primepump call of different levels 
                //so we keep a hashmap that will tell us how many more filters needs to be 
                //processed in the level so that we only add the barrier after the last to be processed
                //so after the entire level has executed

                if(whichPhase == SchedulingPhase.INIT) {
                    int level = lsg.getLevel(filter.getParent());
                    int leftToProcess = levelLeftToProcessInit.get(level);
                    leftToProcess--;
                    levelLeftToProcessInit.put(level, leftToProcess);
                    if (leftToProcess == 0)
                        CoreCodeStore.addBarrierInit();
                }
                else if(whichPhase == SchedulingPhase.PRIMEPUMP) {
                    int level = lsg.getLevel(filter.getParent());
                    int leftToProcess = levelLeftToProcessPP.get(level);
                    leftToProcess--;
                    levelLeftToProcessPP.put(level, leftToProcess);
                    if (leftToProcess == 0) {
                        CoreCodeStore.addBarrierInit();
                        levelLeftToProcessPP.put(level, lsg.getLevels()[level].length);
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#processFilterSlices(at.dms.kjc.slicegraph.Slice, at.dms.kjc.backendSupport.SchedulingPhase, at.dms.kjc.backendSupport.ComputeNodesI)
     */
    @Override
    public void processFilterSlices(Slice slice, SchedulingPhase whichPhase,
            SMPMachine chip) {
        assert false : "The SMP backend does not support slices with multiple filters (processFilterSlices()).";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#processInputSliceNode(at.dms.kjc.slicegraph.InputSliceNode, at.dms.kjc.backendSupport.SchedulingPhase, at.dms.kjc.backendSupport.ComputeNodesI)
     */
    @Override
    public void processInputSliceNode(InputSliceNode input,
            SchedulingPhase whichPhase, SMPMachine chip) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#processOutputSliceNode(at.dms.kjc.slicegraph.OutputSliceNode, at.dms.kjc.backendSupport.SchedulingPhase, at.dms.kjc.backendSupport.ComputeNodesI)
     */
    @Override
    public void processOutputSliceNode(OutputSliceNode output,
            SchedulingPhase whichPhase, SMPMachine chip) {
        // TODO Auto-generated method stub
    }
}
