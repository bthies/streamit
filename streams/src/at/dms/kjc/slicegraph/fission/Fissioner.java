package at.dms.kjc.slicegraph.fission;

import java.util.LinkedList;

import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JEmptyStatement;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JForStatement;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JRelationalExpression;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.ObjectDeepCloner;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.sir.SIRPopExpression;
import at.dms.kjc.slicegraph.*;

public class Fissioner {
    /** the slice we are fissing */
    private Slice slice;
    /** the amount we are fizzing slice by */
    private int fizzAmount;
    /** the filter of the slice we are fissing */
    private FilterSliceNode filter;
    /** the filter info of the filter of the slice we are fissing */
    private FilterInfo fInfo;
    /** the identity slice inserted downstream fo the fizzed slices */
    private Slice idOutput;
    /** the identity slice inserted upstream to the fizzed slices */
    private Slice idInput;
    /** the fission products of the slice */
    private Slice[] sliceClones;
    private Slice[] inputsInit;
    private Slice[] inputsSteady;
    private Slice[] outputsInit;
    private Slice[] outputsSteady;
    /** the stats from the original filter, these don't change! */
    private int slicePeek;
    private int slicePop;
    private int slicePush;
    private int slicePrePeek;
    private int slicePrePop;
    private int slicePrePush;
    private int sliceInitMult;
    private int sliceSteadyMult;
    private int sliceCopyDown;
    /** the pop rate (also peek) of the clones) */
    private int newPop; 
    
    /**
     * Return true if <slice> can be fissed, meaning it is stateless.  The method 
     * does not check that the schedule allows for fission.
     */
    public static boolean canFizz(Slice slice, boolean debug) {

        // Get information on Slice rates
        FilterInfo.reset();

        FilterSliceNode filter = slice.getFirstFilter();
        
        // Check to see if Slice has file reader/writer.  Don't fizz file
        // reader/writer
        if(filter.isPredefined()) {
            if(debug) System.out.println("Can't fizz: Slice contains file reader/writer");
            return false;
        }

        // Make sure that Slice has only one FilterSliceNode
        if(!(slice.getNumFilters() == 1)) {
            if(debug) System.out.println("Can't fizz: Slice has more than one FilterSliceNode");
            return false;
        }

        // Check to make sure that Slice is stateless
        if(MutableStateExtractor.hasMutableState(slice.getFirstFilter().getFilter())) {
            if(debug) System.out.println("Can't fizz: Slice is not stateless!!");
            return false;
        }

        // Check to see if FilterSliceNode contains a linear filter.  At the
        // moment, we can't fizz linear filters
        if(filter.getFilter().isLinear()) {
            if(debug) System.out.println("Can't fizz: Slice contains linear filter, presently unsupported");
            return false;
        }
        
        //TODO: make sure the rates match between the slice and its inputs and the slices 
        //and its outputs

        return true;
    }

    private Fissioner(Slice s, int d) {
        // reset the filter info's just in case things have change
        FilterInfo.reset();

        this.slice = s;
        this.fizzAmount = d;
        this.fInfo = FilterInfo.getFilterInfo(s.getFirstFilter());
        this.filter = slice.getFirstFilter();
        slicePeek = fInfo.peek;
        slicePop = fInfo.pop;
        slicePush = fInfo.push;

        slicePrePeek = fInfo.prePeek;
        slicePrePop = fInfo.prePop;
        slicePrePush = fInfo.prePush;

        sliceInitMult = fInfo.initMult;
        sliceSteadyMult = fInfo.steadyMult;
        sliceCopyDown = fInfo.copyDown;
        int i = 0;
        
        inputsInit = new Slice[s.getHead().getSourceSet(SchedulingPhase.INIT).size()];
        i = 0;
        for (InterSliceEdge edge : s.getHead().getSourceSet(SchedulingPhase.INIT)) {
            inputsInit[i++] = edge.getSrc().getParent();
        }

        inputsSteady = new Slice[s.getHead().getSourceSet(SchedulingPhase.STEADY).size()];
        i = 0;
        for (InterSliceEdge edge : s.getHead().getSourceSet(SchedulingPhase.STEADY)) {
            inputsSteady[i++] = edge.getSrc().getParent();
        }
        
        outputsInit = new Slice[s.getTail().getDestSet(SchedulingPhase.INIT).size()];
        i = 0; 
        for (InterSliceEdge edge : s.getTail().getDestSet(SchedulingPhase.INIT)) {
            outputsInit[i++] = edge.getDest().getParent();
        }
        
        outputsSteady = new Slice[s.getTail().getDestSet(SchedulingPhase.STEADY).size()];
        i = 0; 
        for (InterSliceEdge edge : s.getTail().getDestSet(SchedulingPhase.STEADY)) {
            outputsSteady[i++] = edge.getDest().getParent();
        }
    }
    
    private boolean checks() {
        // Check copyDown constraint: copyDown < mult * pop
        if  (fInfo.copyDown < fInfo.steadyMult * fInfo.pop) { 
            System.out.println("Can't fizz: Slice does not meet copyDown constraint");
            return false;
        }
        
        // Check copyDown constraint: copyDown < mult * pop
        if  (fInfo.copyDown < fInfo.steadyMult * fInfo.pop) { 
            System.out.println("Can't fizz: Slice does not meet copyDown constraint");
            return false;
        }
           
        return true;
    }
    
    private boolean fizz() {
        if (!checks())
            return false;
        
        createFissedSlices();
        createIDInputSlice();
        createIDOutputSlice();
        setupInitPhase();
        replaceInputEdges(SchedulingPhase.INIT); replaceInputEdges(SchedulingPhase.STEADY);
        moveJoinToInputID();
        installFissionSplitPattern();
        synchRemoveIDs();
        
        return true;
    }
    
    /**
     * Calculate and install the splitting pattern for the id intput to the slice clones.
     * This is based on the number of filters we are duplicating to.  See PLDI paper for 
     * more explanation.
     */
    private void installFissionSplitPattern() {
        //calculate the fission split pattern for duplication to as most 2 slices
        
        InterSliceEdge[][] dests = new InterSliceEdge[2 * fizzAmount + 1][];
        int weights[] = new int[2 * fizzAmount + 1];
        //the duplication factor
        int dup = slicePeek - slicePop;
        
        //the first weight clone's pop - copydown - dup, so just what is for this filter minus 
        //what is already in the buffer (copydown)
        weights[0] = newPop - sliceCopyDown - dup;
        //the first dest is just the first filter
        dests[0] = new InterSliceEdge[]{getEdge(idInput, sliceClones[0])};
        //the second weight is just the duplication factor
        weights[1] = dup;
        //second dests are the first and second filter
        dests[1] = new InterSliceEdge[]{getEdge(idInput, sliceClones[0]), getEdge(idInput, sliceClones[1])};
        
        //generate the middle dests and weights
        for (int i = 2; i < 2 * fizzAmount; i += 2) {
            weights[i] = newPop - dup - dup;
            dests[i] =  new InterSliceEdge[]{getEdge(idInput, sliceClones[i/2])};
            weights[i + 1] = dup;
            dests[i + 1] = new InterSliceEdge[]{getEdge(idInput, sliceClones[i/2]), 
                    getEdge(idInput, sliceClones[(i/2) + 1])};
        }
        
        //now take care of the last weight and dest
        weights[2 * fizzAmount] = sliceCopyDown - dup;
        dests[2 * fizzAmount] = new InterSliceEdge[]{getEdge(idInput, sliceClones[0])};
        
        //install the weights and edges for the steady state
        idInput.getTail().setWeights(weights);
        idInput.getTail().setDests(dests);
    }
    
    /**
     * Move slice's joining schedule to the ID and replace the references to slice with idInput
     * in the edges.
     */
    private void moveJoinToInputID() {
        InterSliceEdge[] joining= slice.getHead().getSources(SchedulingPhase.STEADY);
        InterSliceEdge[] newJoin = new InterSliceEdge[joining.length];
        
        for (int i = 0; i < joining.length; i++) {
            assert joining[i].getDest() == slice.getHead();
            InterSliceEdge newEdge = getEdge(joining[i].getSrc().getParent(), idInput);
            newJoin[i] = newEdge;
        }
        
        int[] newWeights = slice.getHead().getWeights(SchedulingPhase.STEADY).clone();
        
        idInput.getHead().setSources(newJoin);
        idInput.getHead().setWeights(newWeights);
    }
    
    /**
     * Query/Replace the edges to original slice with edges to the new input ID in the 
     * outputs of the inputs.  
     */
    private void replaceInputEdges(SchedulingPhase phase) {
        Slice[] inputs = (phase == SchedulingPhase.INIT ? inputsInit : inputsSteady);
        
        for (int i = 0; i < inputs.length; i++) {
            InterSliceEdge edge = getEdge(inputs[i], idInput);
            InterSliceEdge oldEdge = getEdge(inputs[i], slice);
            InterSliceEdge[][] newEdges = replaceEdge(inputs[i].getTail().getDests(phase), oldEdge, edge);
            if (SchedulingPhase.INIT == phase)
                inputs[i].getTail().setInitDests(newEdges);
            else
                inputs[i].getTail().setDests(newEdges);
        }
    }
            
    
    private void setupInitPhase() {
        /*
         * The unfizzed Slice has both an initialization phase and a steady-
         * state phase.  Once the Slice is fizzed, only one of the Slice clones
         * needs to execute the initialization phase.
         *
         * We have chosen that the first Slice clone be the clone to handle
         * initialization.  The remaining Slice clones are simply disabled
         * during initialization.
         */

        // For the first Slice clone, move initialization work into prework.
        // This involves copying the work body into the prework after the
        // original prework body.  The initialization multiplicity is rolled
        // into a loop around the copied work body.
        int newPrePeek = slicePrePeek;
        int newPrePop = slicePrePop;
        int newPrePush = slicePrePush;
        int newInitMult = sliceInitMult;
        
        if(sliceInitMult > 0) {
            JBlock firstWorkBody =
                slice.getFirstFilter().getFilter().getWork().getBody();
            
            JBlock newPreworkBody = new JBlock();
            
            if(slice.getFirstFilter().getFilter().getPrework() != null &&
               slice.getFirstFilter().getFilter().getPrework().length > 0 &&
               slice.getFirstFilter().getFilter().getPrework()[0] != null &&
               slice.getFirstFilter().getFilter().getPrework()[0].getBody() != null) {
                newPreworkBody.addStatement(slice.getFirstFilter().getFilter().getPrework()[0].getBody());
            }

            JVariableDefinition initMultLoopVar =
                new JVariableDefinition(0,
                                        CStdType.Integer,
                                        "initMultCount",
                                        new JIntLiteral(0));

            JVariableDeclarationStatement initMultLoopVarDecl = new JVariableDeclarationStatement(initMultLoopVar);
            newPreworkBody.addStatementFirst(initMultLoopVarDecl);
            
            JRelationalExpression initMultLoopCond =
                new JRelationalExpression(JRelationalExpression.OPE_LT,
                                          new JLocalVariableExpression(initMultLoopVar),
                                          new JIntLiteral(sliceInitMult));
            
            JExpressionStatement initMultLoopIncr =
                new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(initMultLoopVar),
                                                                   new JAddExpression(new JLocalVariableExpression(initMultLoopVar),
                                                                                      new JIntLiteral(1))));
            
            JForStatement initMultLoop =
                new JForStatement(new JEmptyStatement(),
                                  initMultLoopCond,
                                  initMultLoopIncr,
                                  (JBlock)ObjectDeepCloner.deepCopy(firstWorkBody));
            newPreworkBody.addStatement(initMultLoop);
            
            if(slice.getFirstFilter().getFilter().getPrework() == null ||
               slice.getFirstFilter().getFilter().getPrework().length == 0 ||
               slice.getFirstFilter().getFilter().getPrework()[0] == null) {
                JMethodDeclaration newPreworkMethod =
                    new JMethodDeclaration(null,
                                           at.dms.kjc.Constants.ACC_PUBLIC,
                                           CStdType.Void,
                                           "fissionPrework",
                                           JFormalParameter.EMPTY,
                                           CClassType.EMPTY,
                                           newPreworkBody,
                                           null,
                                           null);

                slice.getFirstFilter().getFilter().setPrework(newPreworkMethod);

                newPrePeek = 0;
                newPrePush = 0;
                newPrePop = 0;
            }
            else {
                sliceClones[0].getFirstFilter().getFilter().getPrework()[0].setBody(newPreworkBody);
            }
            
            // For the first Slice clone, adjust prework rates to reflect that 
            // initialization work was moved into prework
            
            newPrePeek = Math.max(slicePrePeek,
                                    slicePrePop + (sliceInitMult * slicePop) + (slicePeek - slicePop));
            newPrePop = slicePrePop + sliceInitMult * slicePop;
            newPrePush = slicePrePush + sliceInitMult * slicePush;
            
            sliceClones[0].getFirstFilter().getFilter().getPrework()[0].setPeek(newPrePeek);
            sliceClones[0].getFirstFilter().getFilter().getPrework()[0].setPop(newPrePop);
            sliceClones[0].getFirstFilter().getFilter().getPrework()[0].setPush(newPrePush);
            
            // Since the initialization work has been moved into prework, set
            // the initialization multiplicity of the first Slice clone to 0
            
            sliceClones[0].getFirstFilter().getFilter().setInitMult(1);
        }

        // Disable all other Slice clones in initialization.  This involves
        // disabling prework and seting initialization multiplicty to 0

        for(int x = 1 ; x < fizzAmount ; x++) {
            sliceClones[x].getFirstFilter().getFilter().setPrework(null);
            sliceClones[x].getFirstFilter().getFilter().setInitMult(0);
        }

        // Since only the first Slice clone executes, it will be the only Slice
        // clone to receive during initialization.
        //
        // If there are multiple source Slices, it is assumed that only the 
        // first source Slice will execute during initialization.  Only the 
        // first source Slice will transmit during initialization.
        //
        // Setup the splitter-joiner schedules to reflect that only the first
        // source Slice transmits and that only the first Slice clone receives.
 
        
        //set the incoming schedule of the 0th clone to receive from the input ID
        InterSliceEdge idTo0 = getEdge(idInput, sliceClones[0]);
        sliceClones[0].getHead().setInitWeights(new int[]{1});
        sliceClones[0].getHead().setInitSources(new InterSliceEdge[]{idTo0});
        
        //set the outgoing schedule of the 0th clone to send to the output ID
        InterSliceEdge toId = getEdge(sliceClones[0], idOutput);
        sliceClones[0].getTail().setInitWeights(new int[]{1});
        InterSliceEdge[] outgoing = {toId};
        sliceClones[0].getTail().setInitDests(new InterSliceEdge[][]{outgoing});
        
        //set the rest of the slice clones' init dists to null
        for(int x = 1 ; x < fizzAmount ; x++) {
            sliceClones[x].getHead().setInitWeights(null);
            sliceClones[x].getHead().setInitSources(null);
        }
        for(int x = 1 ; x < fizzAmount ; x++) {
            sliceClones[x].getTail().setInitWeights(null);
            sliceClones[x].getTail().setDests(null);
        }
    }
    
    private InterSliceEdge getEdge(Slice s1, Slice s2) {
        InterSliceEdge edge = InterSliceEdge.getEdge(s1.getTail(), s2.getHead());
        if (edge == null)
            edge = new InterSliceEdge(s1.getTail(), s2.getHead());
        
        return edge;
    }
    
    private void synchRemoveIDs() {
        FilterInfo idI = FilterInfo.getFilterInfo(idInput.getFirstFilter());
        FilterInfo idO = FilterInfo.getFilterInfo(idOutput.getFirstFilter());
        
        assert idI.copyDown == 0;
        assert idO.copyDown == 0;
    }
    
    private InterSliceEdge[][] replaceEdge(InterSliceEdge[][] oldEdges, 
            InterSliceEdge oldEdge, InterSliceEdge newEdge) {
        InterSliceEdge[][] newEdges = new InterSliceEdge[oldEdges.length][];
        
        for (int i = 0; i < newEdges.length; i++) {
            newEdges[i] = replaceEdge(newEdges[i], oldEdge, newEdge);
        }
        
        return newEdges;
    }
    
    private InterSliceEdge[] replaceEdge(InterSliceEdge[] oldEdges, 
            InterSliceEdge oldEdge, InterSliceEdge newEdge) {
        InterSliceEdge[] newEdges = new InterSliceEdge[oldEdges.length];
        
        for (int i = 0; i < newEdges.length; i++) {
            if (oldEdges[i] == oldEdge) 
                newEdges[i] = newEdge;
            else
                newEdges[i] = oldEdges[i];
        }
        
        return newEdges;
    }
    
    private void createIDInputSlice() {
        //create the ID slice
        idInput = IDFilterContent.createIDSlice();
        
        //set the init mult of the id
        int items = 0;
        for (int i = 0; i < inputsInit.length; i++) {
            InterSliceEdge edge = InterSliceEdge.getEdge(inputsSteady[i].getTail(), slice.getHead());
            items += inputsInit[i].getTail().itemsSentOn(edge, SchedulingPhase.INIT);
        }
        idInput.getFirstFilter().getFilter().setInitMult(items);
        
        //set the steady mult of the filter
        //check to make sure that we pop all we receive in the steady
        int totalItemsReceived = 0;
        for (int i = 0; i < inputsSteady.length; i++) {
            InterSliceEdge edge = InterSliceEdge.getEdge(inputsSteady[i].getTail(), slice.getHead());
            totalItemsReceived += inputsSteady[i].getTail().itemsSentOn(edge, SchedulingPhase.STEADY);
        }
        assert fInfo.steadyMult * fInfo.pop == totalItemsReceived;
        idInput.getFirstFilter().getFilter().setSteadyMult(fInfo.steadyMult * fInfo.pop);
        
        //set the join schedule for each of the clones to just receive from the ID
        for (int i = 0; i < sliceClones.length; i++) {
            InterSliceEdge edge = getEdge(idInput, sliceClones[i]);
            sliceClones[i].getHead().setWeights(new int[]{1});
            sliceClones[i].getHead().setSources(new InterSliceEdge[]{edge});
        }
    }

    private void createIDOutputSlice() {
        idOutput = IDFilterContent.createIDSlice();
        
        //set the init mult of the id
        idOutput.getFirstFilter().getFilter().setInitMult(fInfo.totalItemsSent(SchedulingPhase.INIT));
        
        //set the steadymult of the id
        int steadyItems = fInfo.totalItemsSent(SchedulingPhase.STEADY);
        idOutput.getFirstFilter().getFilter().setSteadyMult(steadyItems);
        int items = 0;
        for (int i = 0; i < outputsSteady.length; i++) {
            InterSliceEdge edge = InterSliceEdge.getEdge(slice.getTail(), outputsSteady[i].getHead());
            items += outputsSteady[i].getHead().itemsReceivedOn(edge, SchedulingPhase.STEADY);
        }
        assert items == steadyItems;
        
        //set the split schedule of each of the clones to just send to the id output
        for (int i = 0; i < sliceClones.length; i++) {
            InterSliceEdge[] edge =  {getEdge(sliceClones[i], idOutput)};
            sliceClones[i].getTail().setWeights(new int[]{1});
            sliceClones[i].getTail().setDests(new InterSliceEdge[][]{edge});
        }
    }
    
    private void createFissedSlices() {
        
        // Fill array with clones of Slice, put original copy first in array
        sliceClones = new Slice[fizzAmount];
        for(int x = 0 ; x < fizzAmount ; x++)
            sliceClones[x] = (Slice)ObjectDeepCloner.deepCopy(slice);

        // Give each Slice clone a unique name
        String origName = slice.getFirstFilter().getFilter().getName();
        for(int x = 0 ; x < fizzAmount ; x++)
            sliceClones[x].getFirstFilter().getFilter().setName(origName + "_fizz" + x);
        
        // Calculate new steady-state multiplicity based upon fizzAmount.  
        // Because work is equally shared among all Slice clones, steady-state 
        // multiplicity is divided by fizzAmount for each Slice clone

        int newSteadyMult = sliceSteadyMult / fizzAmount;

        for(int x = 0 ; x < fizzAmount ; x++)
            sliceClones[x].getFirstFilter().getFilter().setSteadyMult(newSteadyMult);


        /**********************************************************************
         *               Roll steady-state multiplicity into loop             *
         **********************************************************************/

        /*
         * To assist code generation, the steady-state multiplicity is rolled
         * into a loop around the work body of each Slice clone.  The steady-
         * state multiplicity of each Slice clone is then set to 1.  
         *
         * The amount of work completed in each execution of a Slice clone stays
         * constant through this transform.  The main benefit of this transform
         * is that code can now be added to execute at the end of each steady-
         * state iteration.
         *
         * This capability is needed in order to remove unneeded elements from
         * each Slice clone at the end of every steady-state iteration.
         *
         * Unfortunately, this transform breaks the initialization multiplicity.
         * Fortunately, the initialization multiplicity is no longer needed at
         * this point since the initialization work has been copied into
         * prework.
         */

        // Get the work body for each Slice
        JBlock origWorkBodies[] = new JBlock[fizzAmount];

        for(int x = 0 ; x < fizzAmount ; x++)
            origWorkBodies[x] =
                sliceClones[x].getFirstFilter().getFilter().getWork().getBody();

        // Roll the steady-state multiplicity into a loop around the work
        // body of each Slice.
        for(int x = 0 ; x < fizzAmount ; x++) {

            // Construct new work body
            JBlock newWorkBody = new JBlock();

            // Add declaration for for-loop counter variable
            JVariableDefinition steadyMultLoopVar =
                new JVariableDefinition(0, 
                                        CStdType.Integer,
                                        "steadyMultCount",
                                        new JIntLiteral(0));

            JVariableDeclarationStatement steadyMultLoopVarDecl = new JVariableDeclarationStatement(steadyMultLoopVar);
            newWorkBody.addStatement(steadyMultLoopVarDecl);

            // Add for-loop that wraps around existing work body
            JRelationalExpression steadyMultLoopCond =
                new JRelationalExpression(JRelationalExpression.OPE_LT,
                                          new JLocalVariableExpression(steadyMultLoopVar),
                                          new JIntLiteral(newSteadyMult));

            JExpressionStatement steadyMultLoopIncr = 
                new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(steadyMultLoopVar),
                                                                   new JAddExpression(new JLocalVariableExpression(steadyMultLoopVar),
                                                                                      new JIntLiteral(1))));

            JForStatement steadyMultLoop =
                new JForStatement(new JEmptyStatement(),
                                  steadyMultLoopCond,
                                  steadyMultLoopIncr,
                                  (JBlock)ObjectDeepCloner.deepCopy(origWorkBodies[x]));
            newWorkBody.addStatement(steadyMultLoop);

            // Set new work body
            sliceClones[x].getFirstFilter().getFilter().getWork().setBody(newWorkBody);
        }

        // Now that steady-state multiplicity has been rolled around the work
        // bodies of the Slices, change steady-state multiplicity to 1.
        // Recalculate new Slice rates given new steady-state multiplicity.
            
        int newPeek = slicePop * newSteadyMult + slicePeek - slicePop;
        newPop = slicePop * newSteadyMult;
        int newPush = slicePush * newSteadyMult;

        for(int x = 0 ; x < fizzAmount ; x++) {
            sliceClones[x].getFirstFilter().getFilter().setSteadyMult(1);
            sliceClones[x].getFirstFilter().getFilter().getWork().setPeek(newPeek);
            sliceClones[x].getFirstFilter().getFilter().getWork().setPop(newPop);
            sliceClones[x].getFirstFilter().getFilter().getWork().setPush(newPush);
        }
        
        /**********************************************************************
         *                 Perform fission hacks on Slice rates               *
         **********************************************************************/
        
        // Normally, Slices remember peek - pop elements between steady-state
        // iterations.  However, after fizzing, these elements no longer need to
        // be remembered between iterations.  These elements therefore need to 
        // be removed at the end of each steady-state iteration
        //
        // This code adds a pop statement to the end of each work body, removing
        // the unneeded peek - pop elements.  The code also adjusts the pop rate
        // to reflect that more elements are being popped.

        if(newPeek -  newPop > 0) {
            // Add pop statement to end of each work body
            for(int x = 0 ; x < fizzAmount ; x++) {
                CType inputType = 
                    sliceClones[x].getFirstFilter().getFilter().getInputType();
                
                SIRPopExpression popExpr =
                    new SIRPopExpression(inputType, newPeek - newPop);
                JExpressionStatement popStmnt =
                    new JExpressionStatement(popExpr);
                
                sliceClones[x].getFirstFilter().getFilter().getWork().getBody()
                    .addStatement(popStmnt);
            }

            // Adjust pop rates since more elements are now popped
            newPop += (newPeek - newPop);

            for(int x = 0 ; x < fizzAmount ; x++)
                sliceClones[x].getFirstFilter().getFilter().getWork().setPop(newPop);
        }
    }
    
    /**
     * Attempt to fiss <slice> by <fissAmount>.  Return true if the fission was successful.
     */
    public static boolean doit(Slice slice, int fissAmount) {
        Fissioner fissioner = new Fissioner(slice, fissAmount);
                
        return fissioner.fizz();
    }
}
