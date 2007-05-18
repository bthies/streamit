package at.dms.kjc.cell;

import java.util.HashMap;

import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndPartitioner;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

public class SimpleCellLayout implements Layout<CellPU> {

    private HashMap<SliceNode,CellPU> layout;
    SpaceTimeScheduleAndPartitioner spaceTime;
    CellChip cellChip;
    
    public SimpleCellLayout(SpaceTimeScheduleAndPartitioner spaceTime, CellChip cellChip) {
        this.spaceTime = spaceTime;
        this.cellChip = cellChip;
    }
    
    public CellPU getComputeNode(SliceNode node) {
        // TODO Auto-generated method stub
        return null;
    }

    public void run() {
        Slice[] schedule = spaceTime.getSchedule();
        for (int i=0; i<schedule.length; i++) {
            Slice s = schedule[i];
            //if (s.getTail().isFileInput() s.getHead().isInputSlice())
        }
    }

    public void setComputeNode(SliceNode node, CellPU computeNode) {
        // TODO Auto-generated method stub

    }

}
