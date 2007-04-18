package at.dms.kjc.backendSupport;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.SIRBeginMarker;
import at.dms.kjc.sir.SIREndMarker;
import at.dms.kjc.slicegraph.FilterContent;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.vanillaSlice.UniChannel;

/**
 * For creation of additional code necessary to getting filter / joiner / splitter code hooked
 * up to a ComputeCodeStore.  
 * @author dimock / concept and stolen code from mgordon
 */
public abstract  class CodeStoreHelper extends MinCodeUnit {
    /** possible prefix for functions in initialization */
    public static String initStage = "__INITSTAGE__";
    /** possible prefix for functions in steady state */
    public static String steadyStage = "__STEADYSTAGE__";
    /** possible prefix for functions in prime-pump stage */
    public static String primePumpStage = "__PRIMEPUMP__";
    /** possible prefix for loop counters for iterating work function */
    public static String workCounter = "__WORKCOUNTER__";
    /** Do we want to inline work functions or just call a single copy? */
    public static boolean INLINE_WORK = true;
    
    /** The slice node that we are generating helper code for */
    protected SliceNode sliceNode;
    
    /** a BackEndFactory for getting information about other parts of the back end */
    protected BackEndFactory backEndBits;
    
    //keep a unique integer for each filter in each trace
    //so var names do not clash
    private static int globalID = 0;
    /** a value that should be unique per instance, useful in generating non-clashing variable names. */
    protected int uniqueID;
    /** a way of setting the unique value */
    protected static int getUniqueID() 
    {
        return globalID++;
    }

    protected JMethodDeclaration primePumpMethod = null;
    protected JMethodDeclaration initMethod = null;
    protected JMethodDeclaration preWorkMethod = null;
    protected JMethodDeclaration workMethod = null;
    
    /** General constructor: need to add fields and methods later. */
    public CodeStoreHelper (SliceNode node, BackEndFactory backEndBits) {
        super(new JFieldDeclaration[0], new JMethodDeclaration[0]);
        sliceNode = node;
        this.backEndBits = backEndBits;
        uniqueID = getUniqueID();
    }
    
    /** Constructor from a FilterContent, fills out fields, methods, initMethod, preWorkMethod, workMethod.
     * Note: clones inputs. */
    public CodeStoreHelper(SliceNode node, FilterContent filter, BackEndFactory backEndBits) {
        this(node, backEndBits);
        setFields((JFieldDeclaration[])ObjectDeepCloner.deepCopy(filter.getFields()));
        setMethods((JMethodDeclaration[])ObjectDeepCloner.deepCopy(filter.getMethods()));
        for (int i = 0; i < getMethods().length; i++) {
            if (filter.getMethods()[i] == filter.getInit()) {
                initMethod = getMethods()[i];
            } else if (filter.getMethods()[i] == filter.getWork()) {
                workMethod = getMethods()[i];
            } else if (filter.getMethods()[i] == filter.getInitWork()) {
                preWorkMethod = getMethods()[i];
            }
        }
    }
    
    /** @return get init method, may be null since some SliceNodes may only generate helper methods. */
    public JMethodDeclaration getInitMethod() {
        return initMethod;
    }
    
    /** set init method: please pass it some method already in range of {@link #getMethods()} */
    public void setInitMethod (JMethodDeclaration meth) {
        initMethod = meth;
    }
    
   
    /** @return get preWork (initWork) method, may be null. */
    public JMethodDeclaration getPreWorkMethod() {
        return preWorkMethod;
    }
    
    /** set preWork (initWork) method: please pass it some method already in range of {@link #getMethods()} */
    public void setPreWorkMethod (JMethodDeclaration meth) {
        preWorkMethod = meth;
    }
    
    
   /** @return get work method, may be null since some SliceNodes may only generate helper methods. */
    public JMethodDeclaration getWorkMethod () {
        return workMethod;
    }
    
    /** set work method: please pass it some method already in range of {@link #getMethods()} */
    public void setWorkMethod (JMethodDeclaration meth) {
        workMethod = meth;
    }
    
    
    static private Map<SliceNode,CodeStoreHelper> sliceNodeToHelper = new HashMap<SliceNode,CodeStoreHelper>() ;
    /**
     * Use {@link #findCodeForSlice}, {@link #addCodeForSlice} to keep track of whether a SIRCodeUnit of code has been
     * generated already for a SliceNode.
     * @param s  A SliceNode
     * @return  The CodeStoreHelper added for the SliceNode by {@link #addCodeForSlice}.
     */
    public static CodeStoreHelper findHelperForSliceNode(SliceNode s) {
        return sliceNodeToHelper.get(s);
    }
   
    /**
     * Record a mapping from a SliceNode to a CodeStoreHelper.
     * Used to track out-of-sequence code generation to eliminate duplicates. 
     * @param s a SliceNode
     * @param u a CodeStoreHelper
     */
    public static void addHelperForSliceNode(SliceNode s, CodeStoreHelper u) {
        sliceNodeToHelper.put(s, u);
    }
   
    /**
     * Clean up static data.
     */
    public void reset() {
        sliceNodeToHelper = new HashMap<SliceNode,CodeStoreHelper>() ;
    }
   /** @return all fields that are needed in the ComputeCodeStore: 
     * both those from underlying code and those generated in this class */
    public JFieldDeclaration[] getUsefulFields() {
        return getFields();      
    }
    
    /** @return all methods that are needed in the ComputeCodeStore: 
     * may decide to not return a method if its body will be inlined. */
    public JMethodDeclaration[] getUsefulMethods() {
        Vector<JMethodDeclaration> methods = new Vector<JMethodDeclaration>();

        for (int i = 0; i < getMethods().length; i++) {
            //don't generate code for the work function if we are inlining!
            if (INLINE_WORK && 
                    getMethods()[i] == getWorkMethod())
                continue;
            methods.add(getMethods()[i]);
        }
    
        return methods.toArray(new JMethodDeclaration[methods.size()]);    
    }
    /** 
     * @return the method we should call to execute the init stage.
     */
    public abstract JMethodDeclaration getInitStageMethod();
    /**
     * @return The block we should inline to execute the steady-state
     */
    public abstract JBlock getSteadyBlock();
    /**
     * @return The method to call for one execution of the filter in the
     * prime pump stage.
     */
    public abstract JMethodDeclaration getPrimePumpMethod();

    /**
     * Calculate and return the method that will implement one execution
     * of this filter in the primepump stage.  This method may be called multiple
     * times depending on the number of stages in the primepump stage itself.   
     * 
     * @return The method that implements one stage of the primepump exeuction of this
     * filter. 
     */
    protected JMethodDeclaration getPrimePumpMethodForFilter(FilterInfo filterInfo) 
    {
        if (primePumpMethod != null) {
            return primePumpMethod;
        }
        
        JBlock statements = new JBlock();
        // channel code before work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getPrevious().getEdgeToNext()).beginSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getEdgeToNext()).beginSteadyWrite()) {
                statements.addStatement(stmt);
            }
        }
        // add the calls to the work function for the priming of the pipeline
        statements.addStatement(getWorkFunctionBlock(filterInfo.steadyMult));
        // channel code after work block
        if (backEndBits.sliceHasUpstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getPrevious().getEdgeToNext()).endSteadyRead()) {
                statements.addStatement(stmt);
            }
        }
        if (backEndBits.sliceHasDownstreamChannel(sliceNode.getParent())) {
            for (JStatement stmt : backEndBits.getChannel(
                    sliceNode.getEdgeToNext()).endSteadyWrite()) {
                statements.addStatement(stmt);
            }
        }
        //return the method
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
     * Get some statement that calls the work function multiple times.
     * @param mult multiplicity
     * @return statement causing work function to be executed multiple times.
     */
    protected abstract JBlock getWorkFunctionBlock(int mult);

    /**
       * Return the code that will call the work function once.
       * It will either be the entire function inlined or a function call.
       * Do not call if the CodeStoreHelper does not contain a work function.
       * @see CodeStoreHelper#INLINE_WORK
       * @return The code to execute the work function once.
       */
    protected JStatement getWorkFunctionCall() {
          if (INLINE_WORK) {
              JBlock body = (JBlock)ObjectDeepCloner.deepCopy(this.getWorkMethod().getBody());
              if (!(body.getStatement(0) instanceof SIRBeginMarker)) {
                  body.addStatementFirst(new SIRBeginMarker("inlined " + this.getWorkMethod().getName()));
                  body.addStatement(new SIREndMarker("inlined " + this.getWorkMethod().getName()));
              }
              return body;
          }
          else 
              return new JExpressionStatement(null, 
                                              new JMethodCallExpression(null,
                                                                        new JThisExpression(null),
                                                                        this.getWorkMethod().getName(),
                                                                        new JExpression[0]),
                                              null);
      }
}
