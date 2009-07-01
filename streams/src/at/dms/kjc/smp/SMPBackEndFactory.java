/**
 * 
 */
package at.dms.kjc.smp;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

/**
 * @author mgordon
 *
 */
public class SMPBackEndFactory extends BackEndFactory<SMPMachine, Core, CoreCodeStore, Integer> {

    private SMPMachine chip;
    private SMPBackEndScaffold scaffold;
    
    public SMPBackEndFactory(SMPMachine tChip) {
        chip = tChip;
        scaffold = new SMPBackEndScaffold();
    }
    
    public void setScheduler(Scheduler scheduler) {
    	this.setLayout(scheduler);
    	ProcessFilterSliceNode.setScheduler(scheduler);
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
        //System.out.println("Processing: " + filter + " on tile " + layout.getComputeNode(filter).getCoreNumber() + "(" + whichPhase + ")");
        if (filter.isPredefined()) {
            if (filter.isFileInput())
                (new ProcessFileReader(filter, whichPhase, this)).processFileReader();
            else if (filter.isFileOutput()) {
                (new ProcessFileWriter(filter, whichPhase, this)).processFileWriter();
            }
        } 
        else {
            (new ProcessFilterSliceNode(filter, whichPhase, this)).processFilterSliceNode();
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
