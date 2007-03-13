package at.dms.kjc.backendSupport;

import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBooleanLiteral;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JStatement;
import at.dms.kjc.JWhileStatement;
import at.dms.kjc.slicegraph.FilterInfo;

/**
 * A data structure for associating code with each filter at each phase.
 * 
 * As lifted from MGordon's code there is a mutually-recursive type problem:
 * ComputeCodeStore and ComputeNode types are extended in parallel, and each refers to the other.
 * 
 * @author mgodon / dimock
 *
 * @param ComputeNodeType 
 */
public class ComputeCodeStore<ComputeNodeType extends ComputeNode<?>> implements at.dms.kjc.sir.SIRCodeUnit {

    /** the name of the main function for each tile */
    public static String mainName = "";
    /* attempt to give a default name but let derived
     * (actually any) class override. */
    static { if ("".equals(mainName)) mainName = "__MAIN__";}
    
    public void addSliceInit(FilterInfo filterInfo, Layout layout) {
        throw new at.dms.util.NotImplementedException();
    }

    public void addSlicePrimePump(FilterInfo filterInfo, Layout layout) {
        throw new at.dms.util.NotImplementedException();
    }

    public void addSliceSteady(FilterInfo filterInfo, Layout layout) {
        throw new at.dms.util.NotImplementedException();
    }

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

    /**
     * Constructor
     * @param parent a ComputeCodeStore or an instance of some subclass.
     */
    public ComputeCodeStore(ComputeNodeType parent) {
        this.parent = parent;
        methods = new JMethodDeclaration[0];
        fields = new JFieldDeclaration[0];
        mainMethod = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                                         CStdType.Void, mainName, JFormalParameter.EMPTY, CClassType.EMPTY,
                                         new JBlock(null, new JStatement[0], null), null, null);

        // add the block for the init stage calls
        initBlock = new JBlock(null, new JStatement[0], null);
        mainMethod.addStatement(initBlock);
        /*
         * //add the call to free the init buffers rawMain.addStatement(new
         * JExpressionStatement (null, new JMethodCallExpression(null, new
         * JThisExpression(null), CommunicateAddrs.freeFunctName, new
         * JExpression[0]), null));
         */
        // create the body of steady state loop
        steadyLoop = new JBlock(null, new JStatement[0], null);
        // add it to the while statement
        mainMethod.addStatement(new JWhileStatement(null, new JBooleanLiteral(
                                                                           null, true), steadyLoop, null));
        addMethod(mainMethod);

    }

    /**
     * @param stmt  statement to add after any other statements in init code.
     * 
     */
     public void addInitStatement(JStatement stmt) {
         initBlock.addStatement(stmt);
     }
    
     /**
      * @param stmt  statement to add after any other statements in init code.
      * 
      */
    public void addSteadyLoopStatement(JStatement stmt) {
         steadyLoop.addStatement(stmt);
     }
     

    
    /**
     * Bill's code adds method <pre>meth</pre> to this, if <pre>meth</pre> is not already
     * registered as a method of this. Requires that <pre>method</pre> is non-null.
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
     * Adds <pre>f</pre> to the fields of this. Does not check for duplicates.
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
     * adds field <pre>field</pre> to this, if <pre>field</pre> is not already registered as a
     * field of this.
     */
    public void addField(JFieldDeclaration field) {
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
     * Adds <pre>m</pre> to the methods of this. Does not check for duplicates.
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
     * declaration of <pre>meth</pre> in its method array.
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

}