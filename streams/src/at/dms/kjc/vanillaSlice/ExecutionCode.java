package at.dms.kjc.vanillaSlice;

import java.util.*;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.FilterInfo;
//import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.FilterContent;
/**
 * Code for filters, from Mike's RawExecutionCode sub-classes.
 * @author dimock, ripping off mgordon
 *
 */
public class ExecutionCode {
    /** the method that executes each stage of the prime pump schedule */
    protected JMethodDeclaration primePumpMethod;
    /** saves the FilterInfo from creation time. */
    protected FilterInfo filterInfo;

    public static String initStage = "__INITSTAGE__";
    public static String primePumpStage = "__PRIMEPUMP__";
    public static String steadyStage = "__STEADYSTAGE__";

    public static String workCounter = "__WORKCOUNTER__";

    public static String exeIndex1Name = "__EXEINDEX__1__";
    private JVariableDefinition exeIndex1;

    public static boolean INLINE_WORK = false;
    
    //keep a unique integer for each filter in each trace
    //so var names do not clash
    private static int globalID = 0;
    protected int uniqueID;

    /** */
    public static int getUniqueID() 
    {
        return globalID++;
    }

    
    public ExecutionCode(FilterInfo filterInfo) 
    {
        this.filterInfo = filterInfo;
        uniqueID = getUniqueID();
        primePumpMethod = null;
    }
    
    /**
     * Calculate and return the method that will implement one execution
     * of this filter in the primepump stage.  This method may be called multiple
     * times depending on the number of stages in the primepump stage itself.   
     * 
     * @return The method that implements one stage of the primepump exeuction of this
     * filter. 
     */
    public JMethodDeclaration getPrimePumpMethod() 
    {
        if (primePumpMethod != null)
            return primePumpMethod;
        
        JBlock statements = new JBlock(null, new JStatement[0], null);
        FilterContent filter = filterInfo.filter;
    
        //add the calls to the work function in the prime pump stage
        statements.addStatement(getWorkFunctionBlock(filterInfo.steadyMult)); 

        primePumpMethod = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                                      CStdType.Void,
                                      primePumpStage + uniqueID,
                                      JFormalParameter.EMPTY,
                                      CClassType.EMPTY,
                                      statements,
                                      null,
                                      null);
        return primePumpMethod;
    }
    
    /**
     * Return the code that will call the work work function once.
     * It will either be the entire function inlined or a function call.
     * depends on global INLINE_WORK.
     * 
     * @param filter The filter content for this filter.
     * @return The code to execute the work function once.
     */
    private JStatement getWorkFunctionCall(FilterContent filter) 
    {
        if (INLINE_WORK)    
            return (JBlock)ObjectDeepCloner.deepCopy(filter.getWork().getBody());
        else 
            return new JExpressionStatement(null, 
                                            new JMethodCallExpression(null,
                                                                      new JThisExpression(null),
                                                                      filter.getWork().getName(),
                                                                      new JExpression[0]),
                                            null);
    }

    /**
     * Calculate and return the method that implements the init stage 
     * computation for this filter.  It should be called only once in the 
     * generated code.
     * <p>
     * This does not include the call to the init function of the filter.
     * That is done in {@link RawComputeCodeStore#addInitFunctionCall}. 
     * 
     * @return The method that implements the init stage for this filter.
     */
    public JMethodDeclaration getInitStageMethod() 
    {
        JBlock statements = new JBlock(null, new JStatement[0], null);
        FilterContent filter = filterInfo.filter;

        //add the calls for the work function in the initialization stage
        statements.addStatement(generateInitWorkLoop(filter));
        
        return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                                      CStdType.Void,
                                      initStage + uniqueID,
                                      JFormalParameter.EMPTY,
                                      CClassType.EMPTY,
                                      statements,
                                      null,
                                      null);
    }
    /**
     * Return an array of methods for any helper methods that we create
     * or that were present in this filter.  They need to be added to the
     * methods of the entire tile.
     * 
     * @return helper methods that need to be placed in the tile's code.
     */
    public JMethodDeclaration[] getHelperMethods() 
    {
        Vector<JMethodDeclaration> methods = new Vector<JMethodDeclaration>();
        /*
        //add all helper methods, except work function
        for (int i = 0; i < filterInfo.filter.getMethods().length; i++) 
        if (!(filterInfo.filter.getMethods()[i].equals(filterInfo.filter.getWork())))
        methods.add(filterInfo.filter.getMethods()[i]);
        */
        for (int i = 0; i < filterInfo.filter.getMethods().length; i++) {
            //don't generate code for the work function if we are inlining!
            if (INLINE_WORK && 
                    filterInfo.filter.getMethods()[i] == filterInfo.filter.getWork())
                continue;
            methods.add(filterInfo.filter.getMethods()[i]);
        }
    
        return methods.toArray(new JMethodDeclaration[0]);    
    }
    
    
    /**
     * Return the variables that are generated by this pass and that
     * need to be added to the fields of the tile.
     * 
     * @return The variables that are generated by this pass.
     */
    public JFieldDeclaration[] getVarDecls() 
    {
        Vector<JFieldDeclaration> decls = new Vector<JFieldDeclaration>();
        FilterContent filter = filterInfo.filter;

        for (int i = 0; i < filter.getFields().length; i++) 
            decls.add(filter.getFields()[i]);
    
   
        //index variable for certain for loops
        if (exeIndex1  != null) {
            decls.add(new JFieldDeclaration(null, exeIndex1, null, null));
        }

        return decls.toArray(new JFieldDeclaration[decls.size()]);
    }
    
    
    /** 
     * Return the block to call the work function in the steady state
     */
    public JBlock getSteadyBlock() 
    {
        return getWorkFunctionBlock(filterInfo.steadyMult);
    }

    /**
     * Generate code to call the work function mult times.
     * 
     * @param mult the work function will be called this many times.
     * 
     * @return code to call the work function mult times.
     **/
    private JBlock getWorkFunctionBlock(int mult)
    {
        JBlock block = new JBlock(null, new JStatement[0], null);
        FilterContent filter = filterInfo.filter;
        JBlock workBlock = new JBlock(null, new JStatement[0], null);
        
        workBlock.addStatement(getWorkFunctionCall(filter));
            
        // create the for loop that will execute the work function
        // local variable for the work loop
        JVariableDefinition loopCounter = new JVariableDefinition(
                null,
                0,
                CStdType.Integer,
                workCounter,
                null);
        
        JStatement loop = 
            Utils.makeForLoopLocalIndex(workBlock, loopCounter, new JIntLiteral(mult));
    
        block.addStatement(new JVariableDeclarationStatement(null,
                                                             loopCounter,
                                                             null));
        block.addStatement(loop);
        return block;
    }
    /**
     * Generate the loop for the work function firings in the 
     * initialization schedule.  This does not include receiving the
     * necessary items for the first firing.  This is handled in  
     * {@link #getInitStageMethod}.
     * This block will generate code to receive items for all subsequent 
     * calls of the work function in the init stage plus the class themselves. 
     *
     * @param filter The filter 
     * @param generatedVariables The vars to use.
     * 
     * @return The code to fire the work function in the init stage.
     */
    JStatement generateInitWorkLoop(FilterContent filter)
    {
        JBlock block = new JBlock(null, new JStatement[0], null);

        //clone the work function and inline it
        JStatement workBlock = 
            getWorkFunctionCall(filter);
    
   
        block.addStatement(workBlock);
    
        if (exeIndex1  == null) {
            exeIndex1 = new JVariableDefinition(null, 
                    0, 
                    CStdType.Integer,
                    exeIndex1Name + uniqueID,
                    null);
        }
        //return the for loop that executes the block init - 1
        //times
        return Utils.makeForLoopFieldIndex(block, exeIndex1, 
                           new JIntLiteral(filterInfo.initMult));
    }
}
