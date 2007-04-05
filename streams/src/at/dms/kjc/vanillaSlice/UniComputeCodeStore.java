package at.dms.kjc.vanillaSlice;

import java.util.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;

public class UniComputeCodeStore extends ComputeCodeStore<UniProcessor> {
    protected Map<FilterContent, ExecutionCode> gotCode;

    public UniComputeCodeStore(UniProcessor parent) {
        super(parent);
        gotCode = new HashMap<FilterContent, ExecutionCode>();
    }
    
    /**
     * Add filterInfo's init stage block at the current position of the init
     * stage for this code store.
     * 
     * @param filterInfo The filter.
     * @param layout The layout of the application.
     */
    @Override
    public void addSliceInit(FilterInfo filterInfo, Layout layout) {
        //if (debugging) {System.err.println("addSliceInit " + filterInfo);}
    
        ExecutionCode exeCode;

        // check to see if we have seen this filter already
        if (gotCode.containsKey(filterInfo.filter)) {
            exeCode = gotCode.get(filterInfo.filter);
        } else {
            exeCode = new ExecutionCode(filterInfo);
            addSliceFieldsAndMethods(exeCode, filterInfo);
            gotCode.put(filterInfo.filter, exeCode);
        }

        // get the initialization routine of the phase
        JMethodDeclaration initStage = exeCode.getInitStageMethod();
        if (initStage != null) {
            // add the method
            if (CODE)
                addMethod(initStage);

            // now add a call to the init stage in main at the appropiate index
            // and increment the index
            initBlock.addStatement(new JExpressionStatement(null,
                                                            new JMethodCallExpression(null, new JThisExpression(null),
                                                                                      initStage.getName(), new JExpression[0]), null));
        }
    }


    /**
     * Add filterInfo's prime pump block to the current position of the
     * primepump code for this tile.
     * 
     * @param filterInfo The filter.
     * @param layout The layout of the application.
     */
    @Override
    public void addSlicePrimePump(FilterInfo filterInfo, Layout layout) {
        //if (debugging) {System.err.println("addSlicePrimePump " + filterInfo);}
     
        ExecutionCode exeCode;
        JMethodDeclaration primePump;
        
        // check to see if we have seen this filter already
        if (gotCode.containsKey(filterInfo.filter)) {
            exeCode = gotCode.get(filterInfo.filter);
        } else {
            exeCode = new ExecutionCode(filterInfo);
            addSliceFieldsAndMethods(exeCode, filterInfo);
            gotCode.put(filterInfo.filter, exeCode);
        }
        
        primePump = exeCode.getPrimePumpMethod();
        if (primePump != null && !hasMethod(primePump) && CODE) {
            addMethod(primePump);
        }   
       
        // now add a call to the init stage in main
        initBlock.addStatement(new JExpressionStatement(null,
                new JMethodCallExpression(null, new JThisExpression(null),
                        primePump.getName(), new JExpression[0]), null));
    }

    /**
     * Add the code necessary to execution the filter of filterInfo
     * at the current position in the steady-state code of this store, 
     * given the layout. 
     * 
     * @param filterInfo The filter to add to the steady-state schedule.
     * @param layout The layout of the application.
     */
    @Override
    public void addSliceSteady(FilterInfo filterInfo, Layout layout) {
        ExecutionCode exeCode;

        // check to see if we have seen this filter already
        if (gotCode.containsKey(filterInfo.filter)) {
            exeCode = gotCode.get(filterInfo.filter);
        } else {
            exeCode = new ExecutionCode(filterInfo);
            addSliceFieldsAndMethods(exeCode, filterInfo);
            gotCode.put(filterInfo.filter, exeCode);
        }
        
        // add the steady state
        JBlock steady = exeCode.getSteadyBlock();
        if (CODE)
            steadyLoop.addStatement(steady);
        else
            // add a place holder for debugging
            steadyLoop.addStatement(
                        new JExpressionStatement(null,
                                new JMethodCallExpression(null, 
                                        new JThisExpression(null),
                                        filterInfo.filter.toString(), 
                                        new JExpression[0]),
                                        null));
    }
    /**
     * Called to add filterInfo's fields, helper methods, and init function call
     * as calculated by exeCode to the compute code store for this tile.  
     * 
     * @param exeCode The code to add.
     * @param filterInfo The filter.
     */
    private void addSliceFieldsAndMethods(ExecutionCode exeCode,
                                          FilterInfo filterInfo) {
        // add the fields of the slice
        addFields(exeCode.getVarDecls());
        // add the helper methods
        addMethods(exeCode.getHelperMethods());
        // add the init function
        addInitFunctionCall(filterInfo);
    }

}
