package at.dms.kjc.backendSupport;

//import java.util.*;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBooleanLiteral;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JStatement;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JWhileStatement;
import at.dms.kjc.sir.SIRCodeUnit;
//import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.common.ALocalVariable;

/**
 * A data structure for associating code with each compute node at each phase.
 * 
 * A Code Store implements a SIRCodeUnit so that you can add fields and methods as needed.
 * A ComputeCodeStore also has a main method (of type void->void).  The main method has three
 * predefined parts: 
 * {@link #addInitStatement(JStatement)} can be used to add a statement that
 * is performed before the steady state.  This is normally called to add statments calling (or inlining) 
 * a work method that needs to be run at initialization time to pre-load data needed for peeks.
 * {@link #addSteadyLoopStatement(JStatement)} can be used to add a statement that is performed 
 * in the steady state loop.
 * {@link #addCleanupStatement(JStatement)} can be used to add a statement that is performed 
 * after the steady state loop.
 * It may be useful to call {@link #addInitFunctionCall(JMethodDeclaration)} to add a call to some
 * method to the beginning of the main method.  As the name implies, this is usually used to add calls
 * to the init() functions in filters.
 * 
 * Use constructors without the iterationBound parameter to run the steady state indefinitely (and thus
 * make {@link #addCleanupStatement(JStatement)} useless).
 * Use constructors with the iterationBound parameter to run the steady state for the number of times
 * determined at run time by the value of the iterationBound.
 * 
 * As lifted from MGordon's code there is a mutually-recursive type problem:
 * ComputeCodeStore and ComputeNode types are extended in parallel, and each refers to the other.
 * New constructors allow ComputeCodeStore to be constructed without reference to a ComputeNode.
 * 
 * @author mgordon / dimock
 *
 * @param <ComputeNodeType> nasty recursive type, historical relic.  TODO: get rid of. 
 */
public class ComputeCodeStore<ComputeNodeType extends ComputeNode<?>> implements SIRCodeUnit {

    /** The name of the main function for each tile. 
     * This static field is passed to {@link #setMyMainName(String)} which can use it or
     * ignore it in setting up a local name for the main loop in each ComputeCodeStore.
     * Assuming that there is only a single subclass of ComputeCodeStore in a given back end
     * you could attempt to override this local field by:
     * <pre>
     * static { if ("".equals(mainName)) mainName = "__MAIN__";}
     * </pre>
     * But you would need to understand your class loader to know what the value would be
     * if multiple subclasses of ComputeCodeStore are loaded in a back end.
     * You are generally better off calling {@link #setMyMainName(String)} in your constructor
     * and ignoring this field.
     */
    public static String mainName = "";

    /** name that may be unique per processor. */
    protected String myMainName;

    /** set to false if you do not want to generate
     * the work functions calls or inline them
     * useful for debugging 
     */
    protected static final boolean CODE = true;
    /** The fields of the tile */
    protected JFieldDeclaration[] fields;
    /** the methods of this tile */
    protected JMethodDeclaration[] methods;
    /** this method calls all the initialization routines
     * and the steady state routines
     */
    protected JMethodDeclaration mainMethod;
    /** The ComputeNode this compute code will be place on */
    protected ComputeNodeType parent;
    /** block for the steady-state, as calculated currently */
    protected JBlock steadyLoop;
    /** the block that executes each slicenode's init schedule, as calculated currently */
    protected JBlock initBlock;
    /** the block executed after the steadyLoop */
    protected JBlock cleanupBlock;

    /**
     * Constructor: steady state loops indefinitely, code store has pointer back to a compute node.
     * @param parent a ComputeNode.
     */
    public ComputeCodeStore(ComputeNodeType parent) {
        this.parent = parent;
        constructorCommon();
        addSteadyLoop();
        getMainFunction().addStatement(cleanupBlock);
    }

    /**
     * Constructor: caller will add code to bound number of iterations, code store has pointer back to a compute node.
     * @param parent a ComputeNode.
     * @param iterationBound a variable that will be defined locally in 
     */
    public ComputeCodeStore(ComputeNodeType parent, ALocalVariable iterationBound) {
        this.parent = parent;
        constructorCommon();
        addSteadyLoop(iterationBound);
        getMainFunction().addStatement(cleanupBlock);
    }
    
    /**
     * Constructor: caller will add code to bound number of iterations, no pointer back to compute node.
     * @param iterationBound a variable that will be defined locally by <code>getMainFunction().addAllStatments(0,stmts);</code>
     */
    public ComputeCodeStore(ALocalVariable iterationBound) {
        constructorCommon();
        addSteadyLoop(iterationBound);
        getMainFunction().addStatement(cleanupBlock);
    }
    
    /**
     * Constructor: steady state loops indefinitely, no pointer back to compute node.
     */
    public ComputeCodeStore() {
        constructorCommon();
        addSteadyLoop();
        getMainFunction().addStatement(cleanupBlock);
    }
    
    private void constructorCommon() {
        methods = new JMethodDeclaration[0];
        fields = new JFieldDeclaration[0];
        mainMethod = new JMethodDeclaration(null,
                at.dms.kjc.Constants.ACC_PUBLIC, CStdType.Void,
                getMyMainName(), JFormalParameter.EMPTY, CClassType.EMPTY,
                new JBlock(null, new JStatement[0], null), null, null);
        this.setMyMainName(mainName);
        // add the block for the init stage calls
        initBlock = new JBlock(null, new JStatement[0], null);
        mainMethod.addStatement(initBlock);
        // create the body of steady state loop
        steadyLoop = new JBlock(null, new JStatement[0], null);
        cleanupBlock = new JBlock(null, new JStatement[0], null);
        addMethod(mainMethod);
    }

    /**
     * Add a way of iterating steadyLoop to the main method.
     * If you do not want the steady state to loop infinitely then
     * you should override this method.
     */
    protected void addSteadyLoop() {
        // add it to the while statement
        mainMethod.addStatement(new JWhileStatement(null, new JBooleanLiteral(
                null, true), steadyLoop, null));
    }
    
    /**
     * Add a way of iterating steadyLoop to the main method.
     * This version takes a local variable (add declaration to mainMethod) that will hold the iteration count.
     * Current implementation is a simple for loop counting up.
     * XXX: The proper behavior is to loop forever if this variable is set to -1, but the
     * current implementation will loop only INT_MAX times.
     * @param iterationBound  the local variable that will hold the iteration count.
     */
    protected void addSteadyLoop(ALocalVariable iterationBound) {
        mainMethod.addStatement(at.dms.util.Utils.makeForLoop(steadyLoop, iterationBound.getRef()));
    }
    
    /** Override to get different MAIN names on different ComputeNode's */
    protected void setMyMainName(String mainName) {
        myMainName = mainName;
        if (mainMethod != null) { mainMethod.setName(mainName); }
    }
    
    /** get name for MAIN method in this code store. 
     * @return name from a JMethodDeclaration */
    public String getMyMainName() {
        if (myMainName == null) {
            setMyMainName(mainName);
        }
        return myMainName;
    }
    
    /**
     * @param stmt  statement to add after any other statements in init code.
     * 
     */
     public void addInitStatement(JStatement stmt) {
         if (stmt != null) initBlock.addStatement(stmt);
     }
     
     public void addInitStatementFirst(JStatement stmt) {
         if (stmt != null) initBlock.addStatementFirst(stmt);
     }

     /**
      * Add a statement to the end of the cleanup code.
      * 
      * Make sure to <pre>getMainFunction().addStatement(getCleanupBlock());</pre>
      * after calling 
      * @param stmt  statement to add after any other statements in cleanup code.
      */
      public void addCleanupStatement(JStatement stmt) {
          if (stmt != null) cleanupBlock.addStatement(stmt);
      }
    
//      /**
//       * @return the block contatining any cleanup code.
//       */
//      public JBlock getCleanupBlock() {
//          return cleanupBlock;
//      }
//      
     /**
      * @param stmt  statement to add after any other statements in steady-state code.
      * 
      */
    public void addSteadyLoopStatement(JStatement stmt) {
        if (stmt != null) steadyLoop.addStatement(stmt);
     }
     
    
    /**
     * Bill's code adds method <b>meth</b> to this, if <b>meth</b> is not already
     * registered as a method of this. Requires that <b>method</b> is non-null.
     */
    public void addMethod(JMethodDeclaration method) {
        assert method != null;
        // see if we already have <method> in this
        for (int i = 0; i < methods.length; i++) {
            if (methods[i] == method) {
                return;
            }
        }
        // otherwise, create new methods array
        JMethodDeclaration[] newMethods = new JMethodDeclaration[methods.length + 1];
        // copy in new method
        newMethods[0] = method;
        // copy in old methods
        for (int i = 0; i < methods.length; i++) {
            newMethods[i + 1] = methods[i];
        }
        // reset old to new
        this.methods = newMethods;
    }

    /**
     * Set methods to m.
     * 
     * @param m The methods to install.
     */
    public void setMethods(JMethodDeclaration[] m) {
        this.methods = m;
    }

    /**
     * Adds <b>f</b> to the fields of this. Does not check for duplicates.
     */
    public void addFields(JFieldDeclaration[] f) {
        JFieldDeclaration[] newFields = new JFieldDeclaration[fields.length
                                                              + f.length];
        for (int i = 0; i < fields.length; i++) {
            newFields[i] = fields[i];
        }
        for (int i = 0; i < f.length; i++) {
            newFields[fields.length + i] = f[i];
        }
        this.fields = newFields;
    }

    /**
     * Adds field <b>field</b> to this, if <b>field</b> is not already registered as a
     * field of this.
     * (existing field is checked for with ==).
     */
    public void addField(JFieldDeclaration field) {
        assert field != null;
        // see if we already have <field> in this
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == field) {
                return;
            }
        }
        // otherwise, create new fields array
        JFieldDeclaration[] newFields = new JFieldDeclaration[fields.length + 1];
        // copy in new field
        newFields[0] = field;
        // copy in old fields
        for (int i = 0; i < fields.length; i++) {
            newFields[i + 1] = fields[i];
        }
        // reset old to new
        this.fields = newFields;
    }

    /**
     * Adds <b>m</b> to the methods of this. Does not check for duplicates.
     */
    public void addMethods(JMethodDeclaration[] m) {
        JMethodDeclaration[] newMethods = new JMethodDeclaration[methods.length
                                                                 + m.length];
        for (int i = 0; i < methods.length; i++) {
            newMethods[i] = methods[i];
        }
        for (int i = 0; i < m.length; i++) {
            newMethods[methods.length + i] = m[i];
        }
        this.methods = newMethods;
    }

    /**
     * @param meth
     * @return Return true if this compute code store already has the
     * declaration of <b>meth</b> in its method array.
     */
    public boolean hasMethod(JMethodDeclaration meth) {
        for (int i = 0; i < methods.length; i++)
            if (meth == methods[i])
                return true;
        return false;
    }

    /**
     * Return the methods of this store.
     * 
     * @return the methods of this store.
     */
    public JMethodDeclaration[] getMethods() {
        return methods;
    }

    /**
     * Return the fields of this store.  
     * 
     * @return the fields of this store.  
     */
    public JFieldDeclaration[] getFields() {
        return fields;
    }

    /**
     * Set the fields of this to f.
     * 
     * @param f The fields to install.
     */
    public void setFields(JFieldDeclaration[] f) {
        this.fields = f;
    }

    /**
     * Return the main function of this store that 
     * will execute the init, primepump, and loop the steady-schedule.
     * 
     * @return the main function.
     */
    public JMethodDeclaration getMainFunction() {
        return mainMethod;
    }

    /**
     * Add the call the init function for a filter as the first statement in "main".
     * 
     * @param init the init function, a call to it will be added.
     */
    public void addInitFunctionCall(JMethodDeclaration init) {
        //JMethodDeclaration init = filterInfo.filter.getInit();
        if (init != null)
            mainMethod.addStatementFirst
            (new JExpressionStatement(null,
                    new JMethodCallExpression(null, new JThisExpression(null),
                            init.getName(), new JExpression[0]),
                            null));
        else
            System.err.println(" ** Warning: Init function is null");

    }

}