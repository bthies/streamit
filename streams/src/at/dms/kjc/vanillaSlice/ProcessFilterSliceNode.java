package at.dms.kjc.vanillaSlice;

//import java.util.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
//import at.dms.util.Utils;
/**
 * Process a FilterSliceNode creating code in the code store and buffers for connectivity.
 * @author dimock
 *
 */
public class ProcessFilterSliceNode {
    
    private static int uid = 0;
    static int getUid() {
        return uid++;
    }

    
    /**
     * Create code for a FilterSliceNode (actually for the whole slice).
     * @param <T>          type of a BackEndFactory to access layout, etc.
     * @param filterNode   the filterNode that needs code generated.
     * @param whichPhase   a scheduling phase {@link SchedulingPhase}
     * @param backEndBits  a BackEndFactory to access layout, etc.
     */
    public static <T extends BackEndFactory> void processFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, T backEndBits) {

        // We should only generate code once for a filter node.
        
        if (SliceNodeToCodeUnit.findCodeForSliceNode(filterNode) == null) {
            System.err.println(
                    "filter " + filterNode.getFilter() +
                    ", make_joiner " + UniChannel.sliceNeedsJoinerCode(filterNode.getParent()) + 
                    ", make_peek_buffer " + UniChannel.filterNeedsPeekBuffer(filterNode) +
                    ", has_upstream_channel " + UniChannel.sliceHasUpstreamChannel(filterNode.getParent()) +
                    ", make_splitter " + UniChannel.sliceNeedsSplitterCode(filterNode.getParent()) +
                    ", has_downstream_channel " + UniChannel.sliceHasDownstreamChannel(filterNode.getParent()));
            
            Channel inputChannel = null;
            
            if (UniChannel.sliceHasUpstreamChannel(filterNode.getParent())) {
                inputChannel = backEndBits.getChannel(filterNode.getPrevious().getEdgeToNext());
            }
            
            Channel outputChannel = null;
            
            if (UniChannel.sliceHasDownstreamChannel(filterNode.getParent())) {
                outputChannel = backEndBits.getChannel(filterNode.getEdgeToNext());
            }
            
            SIRCodeUnit filter_code = addFilterCode(filterNode,inputChannel,outputChannel,backEndBits);
        }

    }

    
    /**
     * Take a code unit (here a FilterContent) and return one with all push, peek, pop 
     * replaced with calls to channel routines.
     * Clones the input methods and munges on the clones, further changes to the returned code
     * will not affect the methods of the input code unit.
     * @param code           The code (fields and methods)
     * @param inputChannel   The input channel -- specifies routines to call to replace peek, pop.
     * @param outputChannel  The output channel -- specifies routines to call to replace push.
     * @return a SIRCodeUnit with no push, peek, or pop instructions.
     */
    private static SIRCodeUnit makeFilterCode(SIRCodeUnit code, 
            Channel inputChannel, Channel outputChannel) {
        
        final String peekName;

        final String popName;
        final String pushName;
        final String popManyName;

        if (inputChannel != null) {
            peekName = inputChannel.peekMethodName();
            popName = inputChannel.popMethodName();
            popManyName = inputChannel.popManyMethodName();
        } else {
            peekName = "/* peek from non-existent channel */";
            popName = "/* pop() from non-existent channel */";
            popManyName = "/* pop(N) from non-existent channel */";
        }
        
        if (outputChannel != null) {
            pushName = outputChannel.pushMethodName();
        } else {
            pushName = "/* push() to non-existent channel */";
        }
        
        JMethodDeclaration[] oldMethods = code.getMethods();
        JMethodDeclaration[] methods = new JMethodDeclaration[oldMethods.length];
        for (int i = 0; i < oldMethods.length; i++) {
            methods[i] = (JMethodDeclaration)AutoCloner.deepCopy(oldMethods[i]);
        }
        
        for (JMethodDeclaration method : methods) {
            method.accept(new SLIRReplacingVisitor(){
                @Override
                public Object visitPopExpression(SIRPopExpression self,
                        CType tapeType) {
                    if (self.getNumPop() > 1) {
                        return new JMethodCallExpression(popManyName, 
                                new JExpression[]{new JIntLiteral(self.getNumPop())});
                    } else {
                        return new JMethodCallExpression(popName, 
                                new JExpression[0]);
                    }
                }
                
                @Override
                public Object visitPeekExpression(SIRPeekExpression self,
                        CType tapeType,
                        JExpression arg) {
                    JExpression newArg = (JExpression)arg.accept(this);
                    return new JMethodCallExpression(peekName, new JExpression[]{newArg});
                }
                
                @Override
                public Object visitPushExpression(SIRPushExpression self,
                        CType tapeType,
                        JExpression arg) {
                    JExpression newArg = (JExpression)arg.accept(this);
                    return new JMethodCallExpression(pushName, new JExpression[]{newArg});
                }
            });
        }
        
        return new MinCodeUnit(code.getFields(),methods);
    }
    
    /**
     * Get code for a filter.
     * If code not yet made, then makes it.
     * @param <T>
     * @param filter         A FilterSliceNode for which we want code.
     * @param inputChannel   The input channel -- specified routines to call to replace peek, pop.
     * @param outputChannel  The output channel -- specified routeines to call to replace push.
     * @param backEndBits
     * @return
     */
    static <T extends BackEndFactory> SIRCodeUnit getFilterCode(FilterSliceNode filter, 
            Channel inputChannel, Channel outputChannel, T backEndBits) {
        SIRCodeUnit filter_code = SliceNodeToCodeUnit.findCodeForSliceNode(filter);
        if (filter_code == null) {
            filter_code = makeFilterCode(filter.getFilter(),inputChannel,outputChannel);
        }
        return filter_code;
    }
  
    /**
     * Get code for a filter and add it to the appropriate ComputeCodeStore.
     * If code not yet made, then makes the code and adds it to the appropriate ComputeCodeStore
     * @param <T> Type of the caller's BackEndFactory.
     * @param filter         A FilterSliceNode for which we want code.
     * @param inputChannel   The input channel -- specified routines to call to replace peek, pop.
     * @param outputChannel  The output channel -- specified routeines to call to replace push.
     * @param backEndBits
     */
    static <T extends BackEndFactory> SIRCodeUnit addFilterCode(FilterSliceNode filter, 
            Channel inputChannel, Channel outputChannel, T backEndBits) {
        SIRCodeUnit filter_code = SliceNodeToCodeUnit.findCodeForSliceNode(filter);
        if (filter_code == null) {
            filter_code = makeFilterCode(filter.getFilter(),inputChannel,outputChannel);
            ComputeNode location = backEndBits.getLayout().getComputeNode(filter);
            assert location != null;
            location.getComputeCode().addFields(filter_code.getFields());
            location.getComputeCode().addMethods(filter_code.getMethods());
        }
        return filter_code;
    }
  
}


