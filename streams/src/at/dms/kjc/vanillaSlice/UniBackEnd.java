package at.dms.kjc.vanillaSlice;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.CommonPasses;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.BasicGreedyLayout;
import at.dms.kjc.backendSupport.NoSWPipeLayout;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndPartitioner;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.spacetime.EmitStandaloneCode;

/**
 * The entry to the back end for a uniprocesor or cluster.
 */
public class UniBackEnd {
    /**
     * Top level method for SpaceTime backend, called via reflection from {@link at.dms.kjc.Main}.
     * @param str               SIRStream from {@link at.dms.kjc.Kopi2SIR}
     * @param interfaces        JInterfaceDeclaration[] from {@link at.dms.kjc.Kopi2SIR}
     * @param interfaceTables   SIRInterfaceTable[] from  {@link at.dms.kjc.Kopi2SIR}
     * @param structs           SIRStructure[] from  {@link at.dms.kjc.Kopi2SIR}
     * @param helpers           SIRHelper[] from {@link at.dms.kjc.Kopi2SIR}
     * @param global            SIRGlobal from  {@link at.dms.kjc.Kopi2SIR}
     */
    public static void run(SIRStream str,
            JInterfaceDeclaration[] interfaces,
            SIRInterfaceTable[] interfaceTables,
            SIRStructure[]structs,
            SIRHelper[] helpers,
            SIRGlobal global) {

        int numCores = KjcOptions.newSimple;
        
        // The usual optimizations
        CommonPasses commonPasses = new CommonPasses();
        /*Slice[] sliceGraph = */ commonPasses.run(str, interfaces, 
                interfaceTables, structs, helpers, global, numCores);
        // partitioner contains information about the Slice graph.
        Partitioner partitioner = commonPasses.getPartitioner();
        partitioner.ensureSimpleSlices();
        
        
        SpaceTimeScheduleAndPartitioner schedule = new SpaceTimeScheduleAndPartitioner(partitioner);

        // create a collection of (very uninformative) processor descriptions.
        UniProcessors processors = new UniProcessors(numCores);

        // assign SliceNodes to processors
        Layout<UniProcessor> layout;
        if (KjcOptions.spacetime && !KjcOptions.noswpipe) {
            layout = new BasicGreedyLayout<UniProcessor>(schedule, processors.toArray());
        } else {
            layout = new NoSWPipeLayout<UniProcessor,UniProcessors>(schedule, processors);
        }
        layout.run();
 
        // create other info needed to convert Slice graphs to Kopi code + Channels
        UniBackEndFactory backEndBits = new UniBackEndFactory(processors);
        backEndBits.setLayout(layout);
        
        // now convert to Kopi code plus channels.
        backEndBits.getBackEndMain().run(schedule, backEndBits);

        /*
         * Quite a lot to do here
         */
        
        // write out C code, currently only for single slice...
        EmitStandaloneCode.emitForSingleSlice(partitioner.getSliceGraph());
        // return success
        System.exit(0);
    }
}
