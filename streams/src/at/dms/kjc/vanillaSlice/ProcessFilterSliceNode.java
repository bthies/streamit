package at.dms.kjc.vanillaSlice;

import java.util.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

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

    /** set of filters for which we have written basic code. */
    // uses WeakHashMap to be self-cleaning, but now have to insert some value.
    private static Map<SliceNode,Boolean>  basicCodeWritten = new WeakHashMap<SliceNode,Boolean>();
    
    /**
     * Create code for a FilterSliceNode (actually for the whole slice).
     * @param filterNode   the filterNode that needs code generated.
     * @param whichPhase   a scheduling phase {@link SchedulingPhase}
     * @param backEndBits  a BackEndFactory to access layout, etc.
     */
    public static  void processFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {

        CodeStoreHelper filter_code = CodeStoreHelper.findHelperForSliceNode(filterNode);
        
        // We should only generate code once for a filter node.
        
        if (filter_code == null) {
            System.err.println(
                    "filter " + filterNode.getFilter() +
                    ", make_joiner " + backEndBits.sliceNeedsJoinerCode(filterNode.getParent()) + 
                    ", make_peek_buffer " + backEndBits.sliceNeedsPeekBuffer(filterNode.getParent()) +
                    ", has_upstream_channel " + backEndBits.sliceHasUpstreamChannel(filterNode.getParent()) +
                    ", make_splitter " + backEndBits.sliceNeedsSplitterCode(filterNode.getParent()) +
                    ", has_downstream_channel " + backEndBits.sliceHasDownstreamChannel(filterNode.getParent()));
            
            Channel inputChannel = null;
            
            if (backEndBits.sliceHasUpstreamChannel(filterNode.getParent())) {
                inputChannel = backEndBits.getChannel(filterNode.getPrevious().getEdgeToNext());
            }
            
            Channel outputChannel = null;
            
            if (backEndBits.sliceHasDownstreamChannel(filterNode.getParent())) {
                outputChannel = backEndBits.getChannel(filterNode.getEdgeToNext());
            }

            filter_code = getFilterCode(filterNode,inputChannel,outputChannel,backEndBits);
        } 

       ComputeNode location = backEndBits.getLayout().getComputeNode(filterNode);
       assert location != null;
       ComputeCodeStore codeStore = location.getComputeCode();

       switch (whichPhase) {
        case INIT:
            // Have the main function for the CodeStore call out init.
            codeStore.addInitFunctionCall(filter_code.getInitMethod());
            JMethodDeclaration workAtInit = filter_code.getInitStageMethod();
            if (workAtInit != null) {
                // if there are calls to work needed at init time then add
                // method to general pool of methods
                codeStore.addMethod(workAtInit);
                // and add call to list of calls made at init time.
                // Note: these calls must execute in the order of the
                // initialization schedule -- so caller of this routine 
                // must follow order of init schedule.
                codeStore.addInitStatement(new JExpressionStatement(null,
                        new JMethodCallExpression(null, new JThisExpression(null),
                                workAtInit.getName(), new JExpression[0]), null));
            }
            break;
        case PRIMEPUMP:
            JMethodDeclaration primePump = filter_code.getPrimePumpMethod();
            if (primePump != null && ! codeStore.hasMethod(primePump)) {
                // Add method -- but only once
                codeStore.addMethod(primePump);
            }
            if (primePump != null) {
                // for each time this method is called, it adds another call
                // to the primePump routine to the initialization.
                codeStore.addInitStatement(new JExpressionStatement(
                                null,
                                new JMethodCallExpression(null,
                                        new JThisExpression(null), primePump
                                                .getName(), new JExpression[0]),
                                null));

            }
            break;
        case STEADY:
            JStatement steadyBlock = filter_code.getSteadyBlock();
            // helper has now been used for the last time, so we can write the basic code.
            // write code deemed useful by the helper into the corrrect ComputeCodeStore.
            // write only once if multiple calls for steady state.
            if (!basicCodeWritten.containsKey(filterNode)) {
                codeStore.addFields(filter_code.getUsefulFields());
                codeStore.addMethods(filter_code.getUsefulMethods());
                basicCodeWritten.put(filterNode,true);
            }
            codeStore.addSteadyLoopStatement(steadyBlock);
            
            // debug info only: expected splitter and joiner firings.
            System.err.print("(Filter" + filterNode.getFilter().getName());
            System.err.print(" " + FilterInfo.getFilterInfo(filterNode).getMult(SchedulingPhase.INIT));
            System.err.print(" " + FilterInfo.getFilterInfo(filterNode).getMult(SchedulingPhase.STEADY));
            System.err.println(")");
            System.err.print("(Joiner joiner_" + filterNode.getFilter().getName());
            System.err.print(" " + FilterInfo.getFilterInfo(filterNode).totalItemsReceived(SchedulingPhase.INIT));
            System.err.print(" " + FilterInfo.getFilterInfo(filterNode).totalItemsReceived(SchedulingPhase.STEADY));
            System.err.println(")");
            System.err.print("(Splitter splitter_" + filterNode.getFilter().getName());
            System.err.print(" " + FilterInfo.getFilterInfo(filterNode).totalItemsSent(SchedulingPhase.INIT));
            System.err.print(" " + FilterInfo.getFilterInfo(filterNode).totalItemsSent(SchedulingPhase.STEADY));
            System.err.println(")");
            
            break;
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
     * @return a CodeStoreHelper with no push, peek, or pop instructions in the methods.
     */
    private static CodeStoreHelper makeFilterCode(FilterSliceNode filter, 
            Channel inputChannel, Channel outputChannel,
            BackEndFactory backEndBits) {
        
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
        
        CodeStoreHelper helper = backEndBits.getCodeStoreHelper(filter);
        JMethodDeclaration[] methods = helper.getMethods();
       
        // relies on fact that a JMethodDeclaration is not replaced so 
        // work, init, preWork are still identifiable after replacement.
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
            // Add markers to code for debugging of emitted code:
            String methodName = "filter " + filter.getFilter().getName() + "." + method.getName();
            method.addStatementFirst(new SIRBeginMarker(methodName));
            method.addStatement(new SIREndMarker(methodName));
        }
        
        return helper;
    }
    
    /**
     * Get code for a filter.
     * If code not yet made, then makes it.
     * @param filter         A FilterSliceNode for which we want code.
     * @param inputChannel   The input channel -- specified routines to call to replace peek, pop.
     * @param outputChannel  The output channel -- specified routeines to call to replace push.
     * @param backEndBits
     * @return
     */
    public static  CodeStoreHelper getFilterCode(FilterSliceNode filter, 
            Channel inputChannel, Channel outputChannel, BackEndFactory backEndBits) {
        CodeStoreHelper filter_code = CodeStoreHelper.findHelperForSliceNode(filter);
        if (filter_code == null) {
            filter_code = makeFilterCode(filter,inputChannel,outputChannel,backEndBits);
            CodeStoreHelper.addHelperForSliceNode(filter, filter_code);
        }
        return filter_code;
    }
}


