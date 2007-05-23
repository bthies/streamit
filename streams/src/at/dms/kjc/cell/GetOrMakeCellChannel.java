package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.ChannelAsArray;
import at.dms.kjc.backendSupport.ChannelAsCircularArray;
import at.dms.kjc.backendSupport.GetOrMakeChannel;
import at.dms.kjc.slicegraph.InterSliceEdge;

public class GetOrMakeCellChannel extends GetOrMakeChannel {

    public GetOrMakeCellChannel(CellBackendFactory backEndBits) {
        super(backEndBits);
    }
    
    @Override
    public Channel makeInterSliceChannel(InterSliceEdge e) {
        return new InterSPUChannel(e);
    }
    
}
