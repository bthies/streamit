package at.dms.kjc.smp;

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
import at.dms.kjc.KjcOptions;
import at.dms.kjc.ObjectDeepCloner;
import at.dms.kjc.sir.SIRPopExpression;

import at.dms.kjc.backendSupport.FilterInfo;

import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.MutableStateExtractor;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.Slicer;

import at.dms.kjc.slicegraph.fission.Fissioner;
import at.dms.kjc.slicegraph.fission.FissionGroup;

public class StatelessFissioner {

    /** unique id generator */
    private static int uniqueID;
    private int myID;

    /** the slice we are fissing */
    private Slice slice;
    /** the amount we are fizzing slice by */
    private int fizzAmount;
    /** the filter of the slice we are fissing */
    private FilterSliceNode filter;
    /** the filter info of the filter of the slice we are fissing */
    private FilterInfo fInfo;

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

    /** the fission products of the slice */
    private Slice[] sliceClones;

    public static FissionGroup doit(Slice slice, Slicer slicer, int fissAmount) {
        if(!KjcOptions.sharedbufs) {
            return Fissioner.doit(slice, slicer, fissAmount);
        }
        else {
            System.out.println("Performing fission on: " + slice.getFirstFilter() + ", fizzAmount: " + fissAmount);
            StatelessFissioner fissioner = new StatelessFissioner(slice, fissAmount);
            if(canFizz(slice, false))
                return fissioner.fizz();
            return null;
        }
    }

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

    private StatelessFissioner(Slice slice, int fizzAmount) {
        this.slice = slice;
        this.fizzAmount = fizzAmount;
        this.filter = slice.getFirstFilter();
        this.fInfo = FilterInfo.getFilterInfo(filter);

        slicePeek = fInfo.peek;
        slicePop = fInfo.pop;
        slicePush = fInfo.push;
        slicePrePeek = fInfo.prePeek;
        slicePrePop = fInfo.prePop;
        slicePrePush = fInfo.prePush;
        sliceInitMult = fInfo.initMult;
        sliceSteadyMult = fInfo.steadyMult;
        sliceCopyDown = fInfo.copyDown;

        myID = uniqueID++;
    }

    private boolean checks() {
        // Check copyDown constraint: copyDown < mult * pop
        if  (fInfo.pop > 0 && fInfo.copyDown >= fInfo.steadyMult * fInfo.pop / fizzAmount) { 
            System.out.println("Can't fizz: Slice does not meet copyDown constraint");
            return false;
        }
                   
        return true;
    }
   
    private FissionGroup fizz() {
        if(!checks())
            return null;

        createFissedSlices();
        setupInitPhase();

        return new FissionGroup(slice, fInfo, sliceClones);
    }

    private void createFissedSlices() {        
        // Fill array with clones of Slice
        sliceClones = new Slice[fizzAmount];
        for(int x = 0 ; x < fizzAmount ; x++)
            sliceClones[x] = (Slice)ObjectDeepCloner.deepCopy(slice);
        
        // Give each Slice clone a unique name
        String origName = slice.getFirstFilter().getFilter().getName();
        for(int x = 0 ; x < fizzAmount ; x++)
            sliceClones[x].getFirstFilter().getFilter().setName(origName + "_fizz" + fizzAmount + "_clone" + x);

        // Modify name of original Slice
        slice.getFirstFilter().getFilter().setName(origName + "_fizz" + fizzAmount);
        
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
            //don't unroll
            steadyMultLoop.setUnrolled(true);
            newWorkBody.addStatement(steadyMultLoop);

            // Set new work body
            sliceClones[x].getFirstFilter().getFilter().getWork().setBody(newWorkBody);
        }

        // Now that steady-state multiplicity has been rolled around the work
        // bodies of the Slices, change steady-state multiplicity to 1.
        // Recalculate new Slice rates given new steady-state multiplicity.
        int newPeek = slicePop * newSteadyMult + slicePeek - slicePop;
        int newPop = slicePop * newSteadyMult;
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
        
        //new initMult only counts the calls to work in the init stage
        //and if this filter is a 2stage, then the first call is to prework
        if (fInfo.isTwoStage()) {
            newInitMult--;
        }
        
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
                                          new JIntLiteral(newInitMult));
            
            JExpressionStatement initMultLoopIncr =
                new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(initMultLoopVar),
                                                                   new JAddExpression(new JLocalVariableExpression(initMultLoopVar),
                                                                                      new JIntLiteral(1))));
            
            JForStatement initMultLoop =
                new JForStatement(new JEmptyStatement(),
                                  initMultLoopCond,
                                  initMultLoopIncr,
                                  (JBlock)ObjectDeepCloner.deepCopy(firstWorkBody));
            //don't unroll
            initMultLoop.setUnrolled(true);
            newPreworkBody.addStatement(initMultLoop);
            
            ///if slice did not have an existing prework, create it and install it at sliceClone[0]
            if(slice.getFirstFilter().getFilter().getPrework() == null ||
               slice.getFirstFilter().getFilter().getPrework().length == 0 ||
               slice.getFirstFilter().getFilter().getPrework()[0] == null) {
                JMethodDeclaration newPreworkMethod =
                    new JMethodDeclaration(null,
                                           at.dms.kjc.Constants.ACC_PUBLIC,
                                           CStdType.Void,
                                           "__fission_prework__" + myID,
                                           JFormalParameter.EMPTY,
                                           CClassType.EMPTY,
                                           newPreworkBody,
                                           null,
                                           null);

                sliceClones[0].getFirstFilter().getFilter().setPrework(newPreworkMethod);

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
                                    slicePrePop + (newInitMult * slicePop) + (slicePeek - slicePop));
            newPrePop = slicePrePop + newInitMult * slicePop;
            newPrePush = slicePrePush + newInitMult * slicePush;
            
            sliceClones[0].getFirstFilter().getFilter().getPrework()[0].setPeek(newPrePeek);
            sliceClones[0].getFirstFilter().getFilter().getPrework()[0].setPop(newPrePop);
            sliceClones[0].getFirstFilter().getFilter().getPrework()[0].setPush(newPrePush);
            
            // Since the initialization work has been moved into prework, set
            // the initialization multiplicity of the first Slice clone to 1
            
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
         
        // Don't change 0th initialization schedule, set the rest of the slice 
        // clones' init dests to null

        for(int x = 1 ; x < fizzAmount ; x++) {
            sliceClones[x].getHead().setInitWeights(null);
            sliceClones[x].getHead().setInitSources(null);
        }
        for(int x = 1 ; x < fizzAmount ; x++) {
            sliceClones[x].getTail().setInitWeights(null);
            sliceClones[x].getTail().setInitDests(null);
        }
    }
}
