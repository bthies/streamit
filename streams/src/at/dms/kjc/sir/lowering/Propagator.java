package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
//import at.dms.compiler.JavadocComment;
import java.lang.Math;
//import at.dms.compiler.TokenReference;
//import at.dms.util.IRPrinter;    // debugging
/**
 * This class propagates constants and partially evaluates all
 * expressions as much as possible.
 */
public class Propagator extends SLIRReplacingVisitor {
    /**
     * Base for new const. prop temp variables.
     */
    public static final String TEMP_VARIABLE_BASE = "__constpropvar_";
    /**
     * Maximum number of elements in an array that we attempt to constant prop.
     */
    private static final int MAX_ARRAY_SIZE = 1024*1024;
    /**
     * Map of known constants/Overloaded for copy prop (JLocalVariable -> JLiteral/JLocalVariableExpr/Array)
     * When storing information about an array JLiteral/JLocalVariablesExpr are stored in the Array being mapped to
     */
    protected Hashtable<JLocalVariable,Object> constants;

    /**
     * Map of constants changed (JLocalVariable -> Boolean.TRUE)
     */
    private Hashtable<JLocalVariable, Boolean> changed;

    /**
     * If anything was added
     */
    protected boolean added;
    /*
     * Values of fields if known (String -> JLiteral/JLocalVariableExpr/Array)
     * Only meant to work within filter
     * All code for updating knownFields has been commented out...
     */
//    protected Hashtable<String,Object> knownFields;

    /**
     * Determines whether this instance of Propagator writes
     * actual changes or not
     */
    protected boolean write;

    /**
     * Used for naming constprop vars
     */
    private static int propNum=0;

    private static int loopDepth=0;

    /**
     * List of vars mutated so the can be assigned the right
     * value at the end of a block
     */
    private LinkedList<JLocalVariable> mutated;

    /**
     * Constructor, given that <i>constants</i> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.  the Propagator will update code.
     * @param constants
     */
    public Propagator(Hashtable<JLocalVariable,Object> constants) {
        super();
        this.constants = constants;
        changed=new Hashtable<JLocalVariable, Boolean>();
        added=false;
        write=true;
//        knownFields=new Hashtable<String,Object>();
        mutated=new LinkedList<JLocalVariable>();
    }
    
    /**
     * Constructor, given that <i>constants</i> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.  the Propagator will optionally update code
     * based on the value of <i>write</i>.
     * @param constants
     * @param write
     */
   public Propagator(Hashtable<JLocalVariable,Object> constants,boolean write) {
        super();
        this.constants = constants;
        changed=new Hashtable<JLocalVariable, Boolean>();
        added=false;
        this.write=write;
//        knownFields=new Hashtable<String,Object>();
        mutated=new LinkedList<JLocalVariable>();
    }
    
   /**
    * Factory rather than direct call to constructor.
    * @param constants
    * @return a Propagator that will update code.
    */
    public Propagator construct(Hashtable<JLocalVariable,Object> constants) {
        return new Propagator(constants);
    }

    /**
     * Factory rather than direct call to constructor.
     * @param constants
     * @param write
     * @return a Propagator that will optionally update code based on value of <i>write</i>.
     */
    public Propagator construct(Hashtable<JLocalVariable,Object> constants,boolean write) {
        return new Propagator(constants,write);
    }

    /**
     * Get map from JLocalVariable (JVariableDefinition, JFormalParameter etc) to constant.
     * Code seems to save various constants: JLiteral, SIRPortal, java arrays of above,
     * and may contain copy prop info as well as constant prop.
     * @return
     */
    public Hashtable<JLocalVariable,Object> getConstants() {
        return constants;
    }

    /**
     * Get map from JLocalVariable (JVariableDefinition, JFormalParameter etc) to whether has changed
     * @return
     */
    public Hashtable<JLocalVariable, Boolean> getChanged() {
        return changed;
    }

    private int getIntLiteral(JExpression expr)
    {
        if (expr instanceof JShortLiteral)
            return expr.shortValue();
        else
            return expr.intValue();
    }

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    public Object visitRegReceiverStatement(SIRRegReceiverStatement self, JExpression portal, SIRStream receiver, JMethodDeclaration[] methods) {
        // first try to resolve the <portal> from a JLocalVariable to
        // an SIRPortal if it has not been done already
        if (portal instanceof JLocalVariableExpression) {
            Object obj = constants.get(((JLocalVariableExpression)portal).getVariable());
            if (obj instanceof SIRPortal) {
                self.setPortal((SIRPortal)obj);
            }
        }
        // Only register receivers if loop depth is zero.  
        // Reason:
        //   The only place we are allowing registration to a portal
        //   is from within the init function.  In the init function,
        //   all loops will be unrolled.  We do not want to register a
        //   receiver before a loop is unrolled, because the receiver
        //   will be cloned and will not exist in the final graph.
        if (loopDepth == 0) {
            // if we have a hook on an SIRPortal, add the receiver stream to it
            if (self.getPortal() instanceof SIRPortal) {
                ((SIRPortal)self.getPortal()).addReceiver(receiver);
            }
        }
        return self;
    }

    /**
     * Visits a while statement
     */
    public Object visitWhileStatement(JWhileStatement self,
                                      JExpression cond,
                                      JStatement body) {
        loopDepth++;
        if(!write) {
            cond.accept(this);
            body.accept(this);
        } else {
            Set<JLocalVariable> freeVars = getFreeVars(self);
            Propagator newProp=construct(cloneTable(constants, freeVars),false);
       
            cond.accept(newProp);
            body.accept(newProp);
            Enumeration<JLocalVariable> remove=newProp.changed.keys();
            //BUG!!!  visiting with newProp could replace references!
            while(remove.hasMoreElements()) {
                JLocalVariable var=remove.nextElement();
                constants.remove(var);
                changed.put(var,Boolean.TRUE);
            }
            Hashtable<JLocalVariable,Object> saveConstants=constants;
            constants=cloneTable(constants, freeVars);
            JExpression newExp = (JExpression)cond.accept(this);
            // reset if we found a constant
            if (newExp!=cond) {
                self.setCondition(newExp);
            }
            body.accept(this);
            constants=saveConstants;
        }
        loopDepth--;
        return self;
    }


    /**
     * Visits a do statement
     */
    public Object visitDoStatement(JDoStatement self,
                                   JExpression cond,
                                   JStatement body) {
        loopDepth++;
        if(!write) {
            cond.accept(this);
            body.accept(this);
        } else {

            Set<JLocalVariable> freeVars = getFreeVars(self);
            Propagator newProp=construct(cloneTable(constants, freeVars),false);
       
            cond.accept(newProp);
            body.accept(newProp);
            Enumeration<JLocalVariable> remove=newProp.changed.keys();
            //BUG!!!  visiting with newProp could replace references!
            while(remove.hasMoreElements()) {
                JLocalVariable var=(JLocalVariable)remove.nextElement();
                constants.remove(var);
                changed.put(var,Boolean.TRUE);
            }
            Hashtable<JLocalVariable,Object> saveConstants=constants;
            constants=cloneTable(constants, freeVars);
            JExpression newExp = (JExpression)cond.accept(this);
            // reset if we found a constant
            if (newExp!=cond) {
                self.setCondition(newExp);
            }
            body.accept(this);
            constants=saveConstants;
        }
        loopDepth--;
        return self;
    }

    
    /**
     * Returns a list of local variables that were used within
     * <phylum> without being defined in <phylum>.
     */
    private Set<JLocalVariable> getFreeVars(JPhylum phylum) {
        if (phylum == null) {
            return new TreeSet<JLocalVariable>();
        }

        // we want to use a treeset just for efficiency, but that
        // requires a comparator, so just compare based on hashcodes.
        Comparator<JLocalVariable> hashCodeComparator = new Comparator<JLocalVariable>() {
                public int compare(JLocalVariable o1, JLocalVariable o2) { 
                    int h1 = o1.hashCode();
                    int h2 = o2.hashCode();
                    if (h1 < h2) {
                        return -1;
                    } else if (h1 > h2) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
                public boolean equals(Object o1, Object o2) { 
                    return o1.hashCode()==o2.hashCode(); 
                }
            };

        // collect used and defined lists
        final TreeSet<JLocalVariable> defined = new TreeSet<JLocalVariable>(hashCodeComparator);
        final TreeSet<JLocalVariable> used = new TreeSet<JLocalVariable>(hashCodeComparator);
        phylum.accept(new SLIREmptyVisitor() {
                public void visitFormalParameters(JFormalParameter self,
                                                  boolean isFinal,
                                                  CType type,
                                                  String ident) {
                    super.visitFormalParameters(self, isFinal, type, ident);
                    defined.add(self);
                }

                public void visitVariableDefinition(JVariableDefinition self,
                                                    int modifiers,
                                                    CType type,
                                                    String ident,
                                                    JExpression expr) {
                    super.visitVariableDefinition(self, modifiers, type, ident, expr);
                    defined.add(self);
                }

                public void visitLocalVariableExpression(JLocalVariableExpression self,
                                                         String ident) {
                    super.visitLocalVariableExpression(self, ident);
                    used.add(self.getVariable());
                }
            });

        // subtract defined from used to get free vars
        used.removeAll(defined);
        return used;
    }
    
    /**
     * Visits a variable declaration statement
     */
    public Object visitVariableDefinition(JVariableDefinition self,
                                          int modifiers,
                                          CType type,
                                          String ident,
                                          JExpression expr) {
        // visit static array dimensions
        if (type.isArrayType()) {
            propagateIntoArrayType((CArrayType)type);
            JExpression[] dims = ((CArrayType)type).getDims();

            // ripped out of original propagator, looks like it's
            // initializing array entries to zero
            changed.put(self, Boolean.TRUE);
            if (dims.length == 1) {
                if (dims[0] instanceof JIntLiteral) {
                    int dim1 = ((JIntLiteral)dims[0]).intValue();
                    // only propagate arrays if they are under a threshold size
                    if (dim1 < MAX_ARRAY_SIZE) {
                        Object[] array = new Object[dim1];
                        constants.put(self, array);
                        added = true;
                    } else
                        constants.remove(self);
                } else
                    constants.remove(self);
            } else if (dims.length == 2) {
                if ((dims[0] instanceof JIntLiteral) && (dims[1] instanceof JIntLiteral)) {
                    int dim1 = ((JIntLiteral)dims[0]).intValue();
                    int dim2 = ((JIntLiteral)dims[1]).intValue();
                    int numElements = dim1 * dim2;

                    // only propagate arrays if they are under a threshold size
                    if (numElements < MAX_ARRAY_SIZE) {
                        Object[][] array = new Object[dim1][dim2];
                        constants.put(self, array);
                        added = true;
                    } else
                        constants.remove(self);
                } else
                    constants.remove(self);
            } else
                constants.remove(self);
        }

        // inspect initializer
        if (expr != null) {
            JExpression newExp = (JExpression)expr.accept(this);
            // if we have a constant AND it's a final variable...
            if (newExp.isConstant()) {
                if (write)
                    self.setExpression(newExp);
                constants.put(self, ((JLiteral)newExp).convertType(self.getType(), null));
                added = true;
                changed.put(self, Boolean.TRUE);
            } else if (newExp!=expr) /*&& CModifier.contains(modifiers, ACC_FINAL)*/ {
                // reset the value
                if (write)
                    self.setExpression(newExp);
                // remember the value for the duration of our visiting
                constants.put(self, newExp);
                added = true;
                changed.put(self, Boolean.TRUE);
            } else if (newExp instanceof JLocalVariableExpression) {
                if (write)
                    self.setExpression(newExp);
                constants.put(self, newExp);
                added = true;
                changed.put(self, Boolean.TRUE);
            } else if (newExp instanceof SIRCreatePortal) {
                if (!constants.containsKey(self)) {
                    // System.out.println("\n[new Exp]Adding portal to constants, variable is: " +
                    //                    self.getIdent() + " constants: " + constants + " at: " +
                    //                    self.getTokenReference());
                    constants.put(self, new SIRPortal(type));
                }
            } else if (newExp instanceof JArrayInitializer) {
                recordArrayInit((JArrayInitializer)newExp, self);
            }
        }
        return self;
    }
    
    /**
     * update ay constant dimensions in array type.
     * @param type
     */
    private void propagateIntoArrayType(CArrayType type) {
        JExpression[] dims = type.getDims();
        for (int i=0; i<dims.length; i++) {
            JExpression newExp = (JExpression)dims[i].accept(this);
            if (newExp !=null && newExp!=dims[i]) {
                dims[i] = newExp;
            }
        }
    }
    
    /*** create structures for array initializers in the hash table, 
         piggy back on Jasp's code for array propagation
         So, create an object array that will represent the array and populate it
         with the values of the array initializer, then add it to the hash table
    **/
    private void recordArrayInit(JArrayInitializer arrInit, 
                                 JVariableDefinition self) 
    {
        if (arrInit.getElems().length == 0)
            return;
    
        if (write)
            self.setExpression(arrInit);

    
        //NOTE: only handle 1 or 2 dimensional rectangular arrays right now
        if (!(arrInit.getElems()[0] instanceof JArrayInitializer)) {
        
            int dim1 = arrInit.getElems().length;
        
            // only propagate arrays if they are under a threshold size
            if (dim1 < MAX_ARRAY_SIZE) {

                Object[] array = new Object[dim1];
        
                for (int i = 0; i < arrInit.getElems().length; i++) {
                    if (!(arrInit.getElems()[i] instanceof JLiteral)) {
                        System.err.println("WARNING: Only rectangular one or two dimensional array" + 
                                           "initializers of literals supported in constant prop... ");
                        return;
                    }
            
                    array[i] = arrInit.getElems()[i];
                }
                constants.put(self, array);
                return;
            }
        }  //now look for 2 dimensional rectangular arrays
        else if (arrInit.getElems()[0] instanceof JArrayInitializer &&
                 ((JArrayInitializer)arrInit.getElems()[0]).getElems().length > 0 &&
                 !(((JArrayInitializer)arrInit.getElems()[0]).getElems()[0] instanceof JArrayInitializer)) {

            int dim1 = arrInit.getElems().length;
            int dim2 = ((JArrayInitializer)arrInit.getElems()[0]).getElems().length;
            int numElements = dim1*dim2;

            // only propagate arrays if they are under a threshold size
            if (numElements < MAX_ARRAY_SIZE) {
        
                Object[][] array = new Object[dim1][dim2];
        
                for (int i = 0; i < array.length; i++) {
                    //check each element of the 1st dim to make sure it is an array init and it has 
                    //the same number of elements...
                    if (!(arrInit.getElems()[i] instanceof JArrayInitializer) ||
                        ((JArrayInitializer)arrInit.getElems()[i]).getElems().length != array[0].length) {
                        System.err.println("WARNING: Only rectangular one or two dimensional array" + 
                                           "initializers of literals supported in constant prop... ");
                        return;
                    }   
                    for (int j = 0; j < array[0].length; j++) {
                        //now place each initial val in the array we are placing in the hash table
                        array[i][j] = ((JArrayInitializer)arrInit.getElems()[i]).getElems()[j];
                    }
                }
        
                constants.put(self, array);
                return;
            }
        }
        System.err.println("WARNING: Only rectangular one or two dimensional array" + 
                           "initializers of literals supported in constant prop... ");
    }
    

    /**
     * Visits a switch statement
     */
    public Object visitSwitchStatement(JSwitchStatement self,
                                       JExpression expr,
                                       JSwitchGroup[] body) {
        if(!write) {
            expr.accept(this);
            for(int i = 0; i < body.length; i++) {
                body[i].accept(this);
            }
        } else {
            JExpression newExp = (JExpression)expr.accept(this);
            // reset if constant
            if (newExp!=expr) {
                self.setExpression(newExp);
            }
            Propagator[] propagators=new Propagator[body.length];
            for (int i = 0; i < body.length; i++) {
                Set<JLocalVariable> varsToClone = getFreeVars(body[i]);
                // include free var from <expr> for proper comparison with <origConstants> below
                varsToClone.addAll(getFreeVars(expr));
                Propagator prop=construct(cloneTable(constants, varsToClone),true);
                propagators[i]=prop;
                body[i].accept(prop);
            }
            if(body.length>0) {
                Hashtable<JLocalVariable,Object> newConstants=propagators[0].constants; //Shadow the main constants
                //Remove if value is not same in all switch bodies
                for(int i=1;i<propagators.length;i++) {
                    Propagator prop=propagators[i];
                    LinkedList<JLocalVariable> remove=new LinkedList<JLocalVariable>();
                    Enumeration<JLocalVariable> eNum=newConstants.keys();
                    while(eNum.hasMoreElements()) {
                        JLocalVariable key=eNum.nextElement();
                        if(!(prop.constants.get(key).equals(newConstants.get(key))))
                            remove.add(key);
                        if((prop.constants.get(key) instanceof Object[])&&(newConstants.get(key) instanceof Object[])) {
                            Object[] array1=(Object[])prop.constants.get(key);
                            Object[] array2=(Object[])newConstants.get(key);
                            if(array1.length!=array2.length)
                                remove.add(key);
                            else
                                for(int j=0;j<array1.length;j++)
                                    if(((array1[j]!=array2[j])&&(array1[j]!=null)&&(!array1[j].equals(array2[j]))))
                                        remove.add(key);
                        }
                    }
                    for(int j=0;j<remove.size();j++) {
                        newConstants.remove(remove.get(j));
                    }
                }
                // mark anything that's in <newConstants> but not in
                // <constants> as <changed>
                Hashtable<JLocalVariable,Object> origConstants = cloneTable(constants, getFreeVars(self));
                for (Enumeration<JLocalVariable> e=origConstants.keys(); e.hasMoreElements(); ) {
                    JLocalVariable key=e.nextElement();
                    if (!newConstants.containsKey(key)) {
                        changed.put(key, Boolean.TRUE);
                        constants.remove(key);
                    } else {
                        constants.put(key, newConstants.get(key));
                    }
                }
            }
        }
        return self;
    }

    /**
     * Visits a return statement
     */
    public Object visitReturnStatement(JReturnStatement self,
                                       JExpression expr) {
        if (expr != null) {
            JExpression newExp = (JExpression)expr.accept(this);
            if (write&&(expr!=newExp)) {
                self.setExpression(newExp);
            }
        }
        return self;
    }

    /**
     * Visits a if statement
     */
    public Object visitIfStatement(JIfStatement self,
                                   JExpression cond,
                                   JStatement thenClause,
                                   JStatement elseClause) {
        if(!write) {
            cond.accept(this);
            //Hashtable saveConstants=(Hashtable)constants.clone();
            thenClause.accept(this);
            if(elseClause!=null)
                elseClause.accept(this);
            //constants=saveConstants;
        } else {
            /*
              {
              IRPrinter printer = new IRPrinter();
              System.err.println("analyzing the expression");
              self.accept(printer);
              printer.close();
              System.out.println();
              }
            */   

            // propagate through (simplify) conditional
            JExpression newExp = (JExpression)cond.accept(this);
            if (newExp!=cond) {
                self.setCondition(newExp);
            }
            // if conditional simplifies to boolean constant
            // return only used branch
            if (newExp instanceof JBooleanLiteral)
                {
                    JBooleanLiteral bval = (JBooleanLiteral)newExp;
                    if (bval.booleanValue())
                        return thenClause.accept(this);
                    else if (elseClause != null)
                        return elseClause.accept(this);
                    else
                        return new JEmptyStatement(self.getTokenReference(), null);
                }
            // propagate through then and else
            Propagator thenProp=construct(cloneTable(constants, getFreeVars(thenClause)),true);
            Propagator elseProp=construct(cloneTable(constants, getFreeVars(elseClause)),true);
            // then clause for any if
            thenClause.accept(thenProp);
            // else clause for two-branched if, and simplify "else {}" to one-branched
            if (elseClause != null) {
                elseClause.accept(elseProp);
                if((elseClause instanceof JBlock)&&(((JBlock)elseClause).size()==0))
                    self.setElseClause(null);
            }
            if ((self.getThenClause() == null)
                || ((self.getThenClause() instanceof JBlock) && (((JBlock) self
                                                                  .getThenClause()).size() == 0))) {
                // if (exp) {} ...
                if ((self.getElseClause() == null)
                    || ((self.getElseClause() instanceof JBlock) && (((JBlock) self
                                                                      .getElseClause()).size() == 0))) {
                    // "if (exp) {} {}" simplifies to "exp;" (exp not a boolean
                    // constant here, may have side effects)
                    return new JExpressionStatement(self.getTokenReference(),
                                                    newExp, null);
                } else {
                    // "if (exp) {} {...}" simplifies to "if (!(exp)) {...}"
                    thenClause = self.getElseClause();
                    elseClause = self.getThenClause();
                    self.setThenClause(thenClause);
                    self.setElseClause(elseClause);
                    newExp = new JLogicalComplementExpression(cond
                                                              .getTokenReference(), newExp);
                    self.setCondition(newExp);
                    if ((elseClause instanceof JBlock)
                        && (((JBlock) elseClause).size() == 0))
                        self.setElseClause(null);
                }
            }
        
            //if(self.getThenClause()==null)
            //if(self.getElseClause()==null)
            //return newExp;
            // reconstruct constants as those that are the same in
            // both <then> and <else>
            Hashtable<JLocalVariable,Object> newConstants = new Hashtable<JLocalVariable,Object>();
            for (Enumeration<JLocalVariable> e = thenProp.constants.keys(); e.hasMoreElements(); ) {
                JLocalVariable thenKey = e.nextElement();
                Object thenVal = thenProp.constants.get(thenKey);
                Object elseVal = elseProp.constants.get(thenKey);
                if((thenVal instanceof Object[])&&(elseVal instanceof Object[])) {
                    Object[] newArray=new Object[((Object[])thenVal).length];
                    if(((Object[])thenVal).length==((Object[])elseVal).length) {
                        for(int i=0;i<((Object[])thenVal).length;i++) {
                            Object thenObj=((Object[])thenVal)[i];
                            Object elseObj=((Object[])elseVal)[i];
                            if((thenObj!=null)&&(elseObj!=null)&&(thenObj.equals(elseObj)))
                                newArray[i]=thenObj;
                        }
                        newConstants.put(thenKey,newArray);
                    }
                } else if (thenVal.equals(elseVal)) {
                    newConstants.put(thenKey, thenVal);
                }
            }
            // integrate the update to overall <constants>
            Hashtable<JLocalVariable,Object> origConstants = cloneTable(constants, getFreeVars(self));
            for (Enumeration<JLocalVariable> e = origConstants.keys(); e.hasMoreElements(); ) {
                JLocalVariable key = e.nextElement();
                if (newConstants.containsKey(key)) {
                    changed.put(key, Boolean.TRUE);
                    constants.put(key, newConstants.get(key));
                } else {
                    constants.remove(key);
                }
            }
        }
        return self;
    }

    // returns a clone of <table> that contains only the keys in
    //<varsToClone>.  Handles deep-cloning of arrays correctly.
    private Hashtable<JLocalVariable,Object> cloneTable(Hashtable<JLocalVariable,Object> table, Set<JLocalVariable> varsToClone) {
        Hashtable<JLocalVariable,Object> out=new Hashtable<JLocalVariable,Object>();
        Iterator<JLocalVariable> keys=varsToClone.iterator();
        while(keys.hasNext()) {
            JLocalVariable key=keys.next();
            if (table.containsKey(key)) {
                Object val=table.get(key);
                if(val instanceof Object[]) {
                    Object[] array=(Object[])val;
                    Object[] newArray=new Object[array.length];
                    System.arraycopy(array,0,newArray,0,array.length);
                    out.put(key,newArray);
                } else {
                    out.put(key, val);
                }
            }
        }
        return out;
    }

//    // returns a clone of <table>.  Handles deep-cloning of arrays
//    // correctly.    
//    private Hashtable<JLocalVariable, Object[]> cloneTable(Hashtable<JLocalVariable,Object[]> table) {
//        Hashtable<JLocalVariable, Object[]> out=new Hashtable<JLocalVariable, Object[]>(table);
//        Enumeration<JLocalVariable> keys=table.keys();
//        while(keys.hasMoreElements()) {
//            JLocalVariable key=keys.nextElement();
//            Object val=table.get(key);
//            if(val instanceof Object[]) {
//                Object[] array=(Object[])val;
//                Object[] newArray=new Object[array.length];
//                System.arraycopy(array,0,newArray,0,array.length);
//                out.put(key,newArray);
//            }
//        }
//        return out;
//    }

    /**
     * Visits a for statement
     */
    public Object visitForStatement(JForStatement self,
                                    JStatement init,
                                    JExpression cond,
                                    JStatement incr,
                                    JStatement body) {
        //Saving constants to restore them after loop analyzed
        //Hashtable saveConstants=(Hashtable)constants.clone();
        //Recurse first to see if variables are assigned in the loop
        loopDepth++;
        if(!write) {
            init.accept(this);
            incr.accept(this);
            cond.accept(this);
            body.accept(this);
        } else {
            JStatement newInit = (JStatement)init.accept(this);
            if (newInit!=null && newInit!=init) {
                self.setInit(newInit);
            }
            Propagator newProp=construct(cloneTable(constants, getFreeVars(self)),false);
            //init.accept(newProp);
            incr.accept(newProp);
            cond.accept(newProp);
            body.accept(newProp);
            Enumeration<JLocalVariable> remove=newProp.changed.keys();
            while(remove.hasMoreElements()) {
                JLocalVariable var=(JLocalVariable)remove.nextElement();
                constants.remove(var);
                changed.put(var,Boolean.TRUE);
            }
            Hashtable<JLocalVariable,Object> saveConstants=constants;
            constants=cloneTable(constants, getFreeVars(self));

            // recurse into cond
            JExpression newExp = (JExpression)cond.accept(this);
            if (newExp!=null && newExp!=cond) {
                self.setCond(newExp);
            }
        
            // recurse into body
            JStatement newBody = (JStatement)body.accept(this);
            if (newBody!=null && newBody!=body) {
                self.setBody(newBody);
            }

            //write=false;
            // recurse into incr
            JStatement newIncr = (JStatement)incr.accept(this);
            if (newIncr!=null && newIncr!=incr)
                self.setIncr(newIncr);
            //write=true;
        
            constants=saveConstants;
        }
        loopDepth--;
        return self;
    }

    /**
     * Visits an expression statement
     */
    public Object visitExpressionStatement(JExpressionStatement self,
                                           JExpression expr) {
        JExpression newExp = (JExpression)expr.accept(this);
        if (write&&(newExp!=expr)) {
            self.setExpression(newExp);
        }
        return self;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------
    
    /*public Object visitPeekExpression(SIRPeekExpression self,
      emacs=)             CType oldTapeType,
      JExpression oldArg) {
      JExpression newArg=(JExpression)oldArg.accept(this);
      if(newArg.isConstant()) {
      self.setArg(newArg);
      }
      return self;
      }*/

    /**
     * Visits a print statement.
     */
    public Object visitPrintStatement(SIRPrintStatement self,
                                      JExpression arg) {
        JExpression newExp = (JExpression)arg.accept(this);
        if (write&&newExp!=null && newExp!=arg) {
            self.setArg(newExp);
        }
    
        return self;
    }

    /**
     * Visits a peek expression.
     */
    public Object visitPeekExpression(SIRPeekExpression self,
                                      CType tapeType,
                                      JExpression arg) {
        JExpression newExp = (JExpression)arg.accept(this);
        if (write&&newExp!=null && newExp!=arg) {
            self.setArg(newExp);
        }
    
        return self;
    }

    /**
     * Visits a push expression.
     */
    public Object visitPushExpression(SIRPushExpression self,
                                      CType tapeType,
                                      JExpression arg) {
        JExpression newExp = (JExpression)arg.accept(this);
        if (write&&newExp!=null && newExp!=arg) {
            self.setArg(newExp);
        }
    
        return self;
    }
    
    public Object visitPostfixExpression(JPostfixExpression self,
                                         int oper,
                                         JExpression expr) {
        //System.out.println("Operand: "+expr);
        if(expr instanceof JLocalVariableExpression) {
            JLocalVariable var=((JLocalVariableExpression)expr).getVariable();
            changed.put(var,Boolean.TRUE);
            if(write) {
                Object val=constants.get(var);
                if(val instanceof JLiteral) {
                    JLiteral lit=(JLiteral)val;
                    if(lit!=null)
                        if(lit instanceof JIntLiteral) {
                            mutated.add(var);
                            constants.put(var,new JIntLiteral(lit.getTokenReference(),((JIntLiteral)lit).intValue()+((self.getOper()==OPE_POSTINC) ? 1 : -1)));
                            added=true;
                            return lit;
                        }
                }
            }
            constants.remove(var);
        } //else
        //System.err.println("WARNING: Postfix of nonvariable: "+expr);
        return self;
    }
    
    public Object visitPrefixExpression(JPrefixExpression self,
                                        int oper,
                                        JExpression expr) {
        if(expr instanceof JLocalVariableExpression) {
            JLocalVariable var=((JLocalVariableExpression)expr).getVariable();
            changed.put(var,Boolean.TRUE);
            if(write) {
                Object val=constants.get(var);
                if(val instanceof JLiteral) {
                    JLiteral lit=(JLiteral)val;
                    if(lit!=null)
                        if(lit instanceof JIntLiteral) {
                            mutated.add(var);
                            JIntLiteral out=new JIntLiteral(lit.getTokenReference(),((JIntLiteral)lit).intValue()+((self.getOper()==OPE_PREINC) ? 1 : -1));
                            constants.put(var,out);
                            added=true;
                            return out;
                        }
                }
            }
            constants.remove(var);
        } /*else
            System.err.println("WARNING: Prefix of nonvariable: "+expr);*/
        return self;
    }

    public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                    int oper,
                                                    JExpression left,
                                                    JExpression right) {
        // need to clone <left> when converting a compound assignment
        // into an assignment, as otherwise this object appears on
        // both the LHS and RHS and can be accidentally visited twice
        // by standard visitors
        JExpression leftCopy = (JExpression)ObjectDeepCloner.deepCopy(left);
        switch(oper) {
        case OPE_SR:
        case OPE_SL:
        case OPE_BSR:
            return new JAssignmentExpression(null,left,new JShiftExpression(null,oper,leftCopy,right)).accept(this);
        case OPE_BAND:
        case OPE_BXOR:
        case OPE_BOR:
            return new JAssignmentExpression(null,left,new JBitwiseExpression(null,oper,leftCopy,right)).accept(this);
        case OPE_PLUS:
            return new JAssignmentExpression(null,left,new JAddExpression(null,leftCopy,right)).accept(this);
        case OPE_MINUS:
            return new JAssignmentExpression(null,left,new JMinusExpression(null,leftCopy,right)).accept(this);
        case OPE_STAR:
            return new JAssignmentExpression(null,left,new JMultExpression(null,leftCopy,right)).accept(this);
        case OPE_SLASH:
            return new JAssignmentExpression(null,left,new JDivideExpression(null,leftCopy,right)).accept(this);
        case OPE_PERCENT:
            return new JAssignmentExpression(null,left,new JModuloExpression(null,leftCopy,right)).accept(this);
        default:
            throw new InconsistencyException("unexpected operator " + oper);
        }
    }

    public Object visitFieldExpression(JFieldAccessExpression self,
      JExpression left,
      String ident)
      {
      JExpression newExp = (JExpression)left.accept(this);
      if (newExp!=null && newExp!=left) {
      self.setPrefix(newExp);
      }
      if (self.getType() != null && self.getType().isArrayType()) {
          propagateIntoArrayType((CArrayType)(self.getType()));
      }
//      Object val=knownFields.get(ident);
//      if(val!=null)
//      return val;
//      else
      return self;
      }

    /**
     * Visits an assignment expression
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                            JExpression left,
                                            JExpression right)
    {
//        if (left instanceof JFieldAccessExpression && ((JFieldAccessExpression)left).getIdent().equals("gains")) {
//            System.err.println("Propagate gains = " + right);
//        }
        
        JExpression newLeft = (JExpression)left.accept(this);
        JExpression newRight = (JExpression)right.accept(this);
        if(write&&(newRight!=null)) {
            self.setRight(newRight);
        }
        /*if(left instanceof JFieldAccessExpression) {
          JLocalVariable var=null;
          Object val=null;
          if(newRight instanceof JLocalVariableExpression)
          var=((JLocalVariableExpression)newRight).getVariable();
          if(var!=null)
          val=constants.get(var);
          if(val!=null)
          knownFields.put(((JFieldAccessExpression)left).getIdent(),val);
          else
          knownFields.remove(((JFieldAccessExpression)left).getIdent());
          }*/
        if (newRight.isConstant()) {
            //System.out.println("Assign: "+left+" "+newRight);
            if((left instanceof JLocalVariableExpression)&&!propVar(left)) {
                JLocalVariable var=((JLocalVariableExpression)left).getVariable();
                //constants.remove(var);
                constants.put(var,((JLiteral)newRight).convertType(var.getType(),null));
                added=true;
                changed.put(var,Boolean.TRUE);
            } else if(left instanceof JArrayAccessExpression) {
                JExpression expr=((JArrayAccessExpression)left).getPrefix();
                if(expr instanceof JLocalVariableExpression) {
                    JLocalVariable var=((JLocalVariableExpression)expr).getVariable();
                    JExpression accessor=((JArrayAccessExpression)left).getAccessor();
                    Object val=constants.get(var);
                    changed.put(var,Boolean.TRUE);
                    if(val instanceof Object[]) {
                        Object[] array=(Object[])val;
                        //System.err.println("Array assigned:"+var+"["+accessor+"]="+newRight);
                        if(array!=null)
                            if(accessor instanceof JIntLiteral) {
                                int idx=((JIntLiteral)accessor).intValue();
                                if(idx>=0&&idx<array.length)
                                    array[idx]=newRight;
                                else {
                                    System.err.println("WARNING: Could not Propagate array "+var+" "+array.length+" "+idx);
                                    constants.remove(var);
                                }
                            } else
                                constants.remove(var);
                        else
                            constants.remove(var);
                    } else
                        constants.remove(var);
                } else if(expr instanceof JArrayAccessExpression) {
                    JExpression pre=((JArrayAccessExpression)expr).getPrefix();
                    if(pre instanceof JLocalVariableExpression) {
                        JLocalVariable var=((JLocalVariableExpression)pre).getVariable();
                        JExpression accessor=((JArrayAccessExpression)expr).getAccessor();
                        JExpression accessor2=((JArrayAccessExpression)left).getAccessor();
                        Object val=constants.get(var);
                        changed.put(var,Boolean.TRUE);
                        if(val instanceof Object[][]) {
                            Object[][] array=(Object[][])val;
                            if(array!=null)
                                if((accessor instanceof JIntLiteral)&&(accessor2 instanceof JIntLiteral)) {
                                    array[((JIntLiteral)accessor).intValue()][((JIntLiteral)accessor2).intValue()]=newRight;
                                } else
                                    constants.remove(var);
                            else
                                constants.remove(var);
                        } else
                            constants.remove(var);
                    } else if(!(pre instanceof JFieldAccessExpression)&&!(pre instanceof JArrayAccessExpression))
                        System.err.println("WARNING:Cannot Propagate Array Prefix "+expr);
                }
            }
        } else if((left instanceof JLocalVariableExpression)) {
            JLocalVariable var=((JLocalVariableExpression)left).getVariable();
            changed.put(var,Boolean.TRUE);
            if(newRight instanceof JLocalVariableExpression) {
                //if(propVar(right)) {
                //if(newRight!=right) {
                Object val=constants.get(((JLocalVariableExpression)newRight).getVariable());
                if(val!=null && val instanceof JLiteral) {
                    constants.put(var,((JLiteral)val).convertType(var.getType(),null));
                    //constants.put(((JLocalVariableExpression)newRight).getVariable(),newRight);
                    added=true;
                } else
                    constants.remove(var);
                //} else
                //constants.put(var,right);
                //} else
                //constants.remove(var);
            } else if(self.getCopyVar()!=null) {
                constants.put(var,self.getCopyVar());
                added=true;
            } else {
                constants.remove(var);
            }
        } else if(left instanceof JArrayAccessExpression) {
            JExpression expr=((JArrayAccessExpression)left).getPrefix();
            // it seems that we have not implemented complete support
            // for propagating 2-D arrays, but at least mark the
            // variable as changed to avoid wrong code
            if (expr instanceof JArrayAccessExpression) {
                JExpression pre=((JArrayAccessExpression)expr).getPrefix();
                if(pre instanceof JLocalVariableExpression) {
                    JLocalVariable var=((JLocalVariableExpression)pre).getVariable();
                    changed.put(var,Boolean.TRUE);
                }
            } else if(expr instanceof JLocalVariableExpression) {
                JLocalVariable var=((JLocalVariableExpression)expr).getVariable();
                JExpression accessor=((JArrayAccessExpression)left).getAccessor();
                changed.put(var,Boolean.TRUE);
                if(constants.get(var) instanceof Object[]) {
                    Object[] array=(Object[])constants.get(var);
                    if(array!=null)
                        if(accessor instanceof JIntLiteral) {
                            /*if(newRight instanceof JLocalVariableExpression) {//&&
                            //propVar(newRight)) {
                            array[((JIntLiteral)accessor).intValue()]=newRight;
                            //System.err.println("Assign:"+var+"["+accessor+"]="+newRight);
                            } else*/
                            int index=((JIntLiteral)accessor).intValue();
                            if(self.getCopyVar()!=null&&index<array.length) {
                                array[index]=self.getCopyVar();
                                /*if(newRight instanceof JLocalVariableExpression) {
                                  constants.put(((JLocalVariableExpression)newRight).getVariable(),newRight);
                                  changed.put(var,Boolean.TRUE);
                                  }*/
                            }
                        } else {
                            constants.remove(var);
                            changed.put(var,Boolean.TRUE);
                        }
                    else {
                        changed.put(var,Boolean.TRUE);
                        constants.remove(var);
                    }
                }
            } else if(!(expr instanceof JFieldAccessExpression)&&!(expr instanceof JArrayAccessExpression))
                System.err.println("WARNING:Cannot Propagate Array Prefix "+expr);
        }

        if (newRight instanceof SIRCreatePortal) {
            /* ...this was commented out before; it looks like it's
               intended to work as an alternative to making portals
               appear "constant".  Still, ick.  --dzm
               JLocalVariableExpression var=(JLocalVariableExpression)left;
               if (!constants.containsKey(var.getVariable())) {
                
               //System.out.println("\n[newRight]Adding portal to constants, variable is: "+var.getVariable().getIdent()+" constants: "+constants+" at: "+self.getTokenReference());
                
               constants.put(var.getVariable(), new SIRPortal(left.getType()));
               }
            */
            // Do cause constant prop to happen to the left-hand
            // side.  This should change it to an SIRPortal.
            if(write&&(newLeft!=null)) {
                self.setLeft(newLeft);
            }
        }

        return self;
    }

    /**
     * Visits an unary plus expression
     */
    public Object visitUnaryPlusExpression(JUnaryExpression self,
                                           JExpression expr)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp.isConstant()) {
            return new JIntLiteral(newExp.intValue());
        } else {
            if (write&&(newExp!=expr)) {
                self.setExpr(newExp);
            }
            return self;
        }
    }

    /**
     * visits a cast expression
     */
    public Object visitCastExpression(JCastExpression self,
                                      JExpression expr,
                                      CType type) {
        JExpression newExp = (JExpression)expr.accept(this);
        // return a constant if we have it
        if (newExp.isConstant()) {
            if (type!=newExp.getType()) {
                return ((JLiteral)newExp).convertType(type,null);
            } else {
                return newExp;
            }
            //} else {
            //return doPromote(expr,expr.convertType(type));
        } else {
            if (write&&(newExp!=expr)) {
                self.setExpr(newExp);
            }
            return self;
        }
    }

    // RMR { add visitor for unary promote expression;
    // if the new expression is a constant, then
    // promote it to the appropriate type and replace
    // the parent expression (i.e., self)
    /**
     * visits a type promotion expression
     */
    public Object visitUnaryPromoteExpression(JUnaryPromote self,
                                              JExpression expr,
                                              CType type) {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp.isConstant()) {
            if (type!=newExp.getType()) {
                return ((JLiteral)newExp).convertType(type,null);
            } else {
                return newExp;
            }
        } else {
            if (write&&(newExp!=expr)) {
                self.setExpr(newExp);
            }
            return self;
        }
    }
    // } RMR

    /**
     * Visits an unary minus expression
     */
    public Object visitUnaryMinusExpression(JUnaryExpression self,
                                            JExpression expr)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp instanceof JIntLiteral) {
            return new JIntLiteral(newExp.intValue()*-1);
        } else if(newExp instanceof JFloatLiteral) {
            return new JFloatLiteral(null,newExp.floatValue()*-1);
        } else if(newExp instanceof JDoubleLiteral) {
            return new JDoubleLiteral(null,newExp.doubleValue()*-1);
        } else {
            return self;
        }
    }

    /**
     * Visits a bitwise complement expression
     */
    public Object visitBitwiseComplementExpression(JUnaryExpression self,
                                                   JExpression expr)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp.isConstant()) {
            return new JIntLiteral(~newExp.intValue());
        } else {
            if (write&&(expr!=newExp)) {
                self.setExpr(newExp);
            }
            return self;
        }
    }

    /**
     * Visits a logical complement expression
     */
    public Object visitLogicalComplementExpression(JUnaryExpression self,
                                                   JExpression expr)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp.isConstant()) {
            return new JBooleanLiteral(null, !newExp.booleanValue());
        } 
        if(newExp instanceof JRelationalExpression) {
            JRelationalExpression neg=((JRelationalExpression)newExp).complement();
            if(neg!=null) {
                return neg;
            }
        }
        if (write&&(expr!=newExp)) {
            self.setExpr(newExp);
        }
        return self;
    }

    /**
     * Visits a shift expression
     */
    public Object visitShiftExpression(JShiftExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        JExpression newLeft = (JExpression)left.accept(this);
        JExpression newRight = (JExpression)right.accept(this);
        if (newLeft.isConstant() && newRight.isConstant()) {
            switch (oper) {
            case OPE_SL:
                return new JIntLiteral(getIntLiteral(newLeft) << 
                                       getIntLiteral(newRight));
            case OPE_SR:
                return new JIntLiteral(getIntLiteral(newLeft) >>
                                       getIntLiteral(newRight));
            case OPE_BSR:
                return new JIntLiteral(getIntLiteral(newLeft) >>>
                                       getIntLiteral(newRight));
            default:
                throw new InconsistencyException();
            }
        } else {
            if (write&&(left!=newLeft)) {
                self.setLeft(newLeft);
            } 
            if (write&&(right!=newRight)) {
                self.setRight(newRight);
            }
            return self;
        }
    }

    /**
     * Visits an array allocator expression
     */
    public Object visitNewArrayExpression(JNewArrayExpression self,
                                          CType type,
                                          JExpression[] dims,
                                          JArrayInitializer init)
    {
        for (int i = 0; i < dims.length; i++) {
            if (dims[i] != null) {
                JExpression newExp = (JExpression)dims[i].accept(this);
                if (write && newExp!=dims[i]) {
                    dims[i] = newExp;
                }
            }
        }
        if (init != null) {
            init.accept(this);
        }
        return self;
    }

    /**
     * Visits a local variable expression
     */
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                               String ident) {
        // if we know the value of the variable, return a literal.
        // otherwise, just return self
        Object constant = constants.get(self.getVariable());
        if (constant instanceof JLiteral) {
            return constant;
        } /*else if(constant instanceof JLocalVariableExpression) {
          //if(constant.equals(constants.get(((JLocalVariableExpression)constant).getVariable()))) //Constant has been unchanged
          return constant;
          }*/
        if (self.isConstant()) {
            return self.getVariable().getValue();
        } else {
            if (self.getType() != null && self.getType().isArrayType()) {
                propagateIntoArrayType((CArrayType)(self.getType()));
            }
            return self;
        }
    }

    /**
     * Visits a relational expression
     */
    public Object visitRelationalExpression(JRelationalExpression self,
                                            int oper,
                                            JExpression left,
                                            JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * Visits a conditional expression
     */
    public Object visitConditionalExpression(JConditionalExpression self,
                                             JExpression cond,
                                             JExpression left,
                                             JExpression right) {
        JExpression newCond = (JExpression)cond.accept(this);
        JExpression newLeft = (JExpression)left.accept(this);
        JExpression newRight = (JExpression)right.accept(this);
        // promote constants if needed
        newLeft = doPromote(newLeft, newRight);
        newRight = doPromote(newRight, newLeft);
        // set any constants that we have 
        if(write) {
            if (newCond!=cond) {
                self.setCond(newCond);
            }
            if (newLeft!=left) {
                self.setLeft(newLeft);
            }
            if (newRight!=right) {
                self.setRight(newRight);
            }
        }
        // do constant-prop if we have constants
        if (newCond.isConstant()) {
            JBooleanLiteral val = (JBooleanLiteral)newCond.getLiteral();
            if (val.booleanValue()) {
                return newLeft;
            } else {
                return newRight;
            }
        } else {
            return self;
        }
    }

    /**
     * prints an array allocator expression
     */
    public Object visitBinaryExpression(JBinaryExpression self,
                                        String oper,
                                        JExpression left,
                                        JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * Visits a compound assignment expression
     */
    public Object visitBitwiseExpression(JBitwiseExpression self,
                                         int oper,
                                         JExpression left,
                                         JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * For processing BinaryExpressions.  
     */
    private Object doBinaryExpression(JBinaryExpression self,
                                      JExpression left,
                                      JExpression right) {
        JExpression newLeft = (JExpression)left.accept(this);
        JExpression newRight = (JExpression)right.accept(this);
        // promote constants if needed
        newLeft = doPromote(newLeft, newRight);
        newRight = doPromote(newRight, newLeft);
        // set any constants that we have
        if(write) {
            if (newLeft!=left) {
                self.setLeft(newLeft);
            }
            if (newRight!=right) {
                self.setRight(newRight);
            }
        }
        // do constant-prop if we have both as constants
        if (write && newLeft.isConstant() && newRight.isConstant()) {
            return self.constantFolding();
        } else {
            // otherwise, return self
            return self;
        }
    }

    public Object visitEqualityExpression(JEqualityExpression self,
                                          boolean equal,
                                          JExpression left,
                                          JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    private JExpression doPromote(JExpression from, JExpression to)
    {
        if (from instanceof JFloatLiteral && to instanceof JDoubleLiteral)
            return new JDoubleLiteral(from.getTokenReference(),
                                      from.floatValue());
        if (from instanceof JIntLiteral && to instanceof JFloatLiteral)
            return new JFloatLiteral(from.getTokenReference(),
                                     from.intValue());
        if (from instanceof JIntLiteral && to instanceof JDoubleLiteral)
            return new JDoubleLiteral(from.getTokenReference(),
                                      from.intValue());
        return from;
    }

    /**
     * Visits a method call expression.  Simplifies known idempotent
     * functions.
     */
    public Object visitMethodCallExpression(JMethodCallExpression self,
                                            JExpression prefix,
                                            String ident,
                                            JExpression[] args)
    {
        prefix.accept(this);
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                JExpression newArg = (JExpression)args[i].accept(this);
                if (write && newArg != args[i])
                    args[i] = newArg;
            }
        }

    
        // Look for known idempotent functions.
        if (args.length == 1 && args[0].isConstant())
            {
                if (ident.equals("sin") || ident.equals("cos") ||
                    ident.equals("log") || ident.equals("exp") ||
                    ident.equals("atan") || ident.equals("sqrt") ||
                    ident.equals("floor") || ident.equals("ceil")) {
                    JExpression narg = doPromote(args[0],
                                                 new JDoubleLiteral(null, 0.0));
                    double darg = narg.doubleValue();
                    if (ident.equals("sin")) {
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.sin(darg));
                    }
                    if (ident.equals("cos"))
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.cos(darg));
                    if (ident.equals("log"))
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.log(darg));
                    if (ident.equals("exp"))
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.exp(darg));
                    if (ident.equals("atan"))
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.atan(darg));
                    if (ident.equals("sqrt"))
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.sqrt(darg));
                    if (ident.equals("floor"))
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.floor(darg));
                    if (ident.equals("ceil"))
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.ceil(darg));
                }
            }
        // RMR { look for known functions with 2 args
        if (args.length == 2 && args[0].isConstant() && args[1].isConstant())
            {
                if (ident.equals("max")) {
                    JExpression narg1 = doPromote(args[0],
                                                  new JDoubleLiteral(null, 0.0));

                    double darg1 = narg1.doubleValue();
                    
                    JExpression narg2 = doPromote(args[1],
                                                  new JDoubleLiteral(null, 0.0));
                    double darg2 = narg2.doubleValue();

                    if (ident.equals("max"))
                        return new JDoubleLiteral(self.getTokenReference(),
                                                  Math.max(darg1, darg2));
                }
            }
        // } RMR

        return self;
    }

    /**
     * Visits a max latency.
     */
    public Object visitLatencyMax(SIRLatencyMax self) {
        self.setMaxExpression((JExpression)self.getMaxExpression().accept(this));
        return self;
    }

    /**
     * Visits a latency range.
     */
    public Object visitLatencyRange(SIRLatencyRange self) {
        //System.err.println("recursing into latency range, from " + self.getMaxExpression() + " to " + self.getMaxExpression().accept(this));
        self.setMinExpression((JExpression)self.getMinExpression().accept(this));
        self.setMaxExpression((JExpression)self.getMaxExpression().accept(this));
        return self;
    }

    /**
     * Visits an array length access expression
     */
    public Object visitArrayLengthExpression(JArrayLengthExpression self,
                                             JExpression prefix) {
        JExpression newExp = (JExpression)prefix.accept(this);
        if(newExp instanceof JLocalVariableExpression) {
            Object array=constants.get(((JLocalVariableExpression)newExp).getVariable());
            if(array instanceof Object[])
                return new JIntLiteral(null,((Object[])array).length);
        }
        if (newExp!=null && newExp!=prefix) {
            self.setPrefix(newExp);
        }
    
        return self;
    }

    /**
     * Visits an array access expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
                                             JExpression prefix,
                                             JExpression accessor) {
        prefix.accept(this);
        JExpression newExp = (JExpression)accessor.accept(this);
        if(write)
            if (newExp instanceof JIntLiteral) {
                self.setAccessor(newExp);
                if(prefix instanceof JLocalVariableExpression) {
                    JLocalVariable var=((JLocalVariableExpression)prefix).getVariable();
                    Object val2=constants.get(var);
                    if(val2 instanceof Object[]) {
                        Object[] array=(Object[])val2;
                        if(array!=null) {
                            //System.err.println("Trying to access index:"+((JIntLiteral)newExp).intValue()+" "+array.length);
                            int index=((JIntLiteral)newExp).intValue(); //fm produces negative indexes for some reason
                            if(index>=0&&index<array.length) {
                                Object val=array[index];
                                //System.err.println("Accessing:"+var+"["+index+"]="+val);
                                if(val!=null) {
                                    if(val instanceof JLiteral)
                                        return val;
                                    //else if(val instanceof JLocalVariableExpression)
                                    //if(val.equals(constants.get(((JLocalVariableExpression)val).getVariable()))) //Constant has been unchanged
                                    //return val;
                                }
                            }
                        }
                    } else {
                        constants.remove(var);
                        changed.put(var,Boolean.TRUE);
                    }
                } else if(prefix instanceof JArrayAccessExpression) {
                    JExpression pre=((JArrayAccessExpression)prefix).getPrefix();
                    if(pre instanceof JLocalVariableExpression) {
                        JLocalVariable var=((JLocalVariableExpression)pre).getVariable();
                        JExpression accessor1=((JArrayAccessExpression)prefix).getAccessor();
                        JExpression newAccess=(JExpression)accessor1.accept(this);
                        if(newAccess instanceof JIntLiteral) {
                            Object val=constants.get(var);
                            changed.put(var,Boolean.TRUE);
                            if(val instanceof Object[][]) {
                                Object[][] array=(Object[][])val;
                                if(array!=null) {
                                    int index=((JIntLiteral)newAccess).intValue();
                                    int index2=((JIntLiteral)newExp).intValue();
                                    if(index>=0) {
                                        Object val2=array[index][index2];
                                        if(val2!=null)
                                            return val2;
                                    }
                                } else
                                    constants.remove(var);
                            } else
                                constants.remove(var);
                        } else
                            constants.remove(var);
                    } else if(!(pre instanceof JFieldAccessExpression)&&!(pre instanceof JArrayAccessExpression))
                        System.err.println("WARNING:Cannot Propagate Array Prefix "+prefix);
                } else if(!(prefix instanceof JFieldAccessExpression)&&!(prefix instanceof JArrayAccessExpression))
                    System.err.println("WARNING:Cannot Propagate Array Prefix "+prefix);
            }
        return self;
    }

    

    //Breaks up complex assignments
    //Useful for Copy Prop
    public Object visitBlockStatement(JBlock self,JavaStyleComment[] comments) {
//        Hashtable copyMap=new Hashtable();
        if(loopDepth<0)
            System.err.println("Neg Loop Depth!");
        if(loopDepth==0){
            int size=self.size();
            for (int i=0;i<size;i++) {
                JStatement state=(JStatement)self.getStatement(i);
                if(state instanceof JExpressionStatement) {
                    JExpression expr=((JExpressionStatement)state).getExpression();
                    if(expr instanceof JAssignmentExpression) {
                        JExpression left=((JAssignmentExpression)expr).getLeft();
                        JExpression right=((JAssignmentExpression)expr).getRight();
                        CType type = null;
                        //Types worth copying
                        if (right instanceof JFieldAccessExpression) {
                            if(right.getType()!=null)
                                type=right.getType();
                            else
                                type=left.getType();
                        }
                        if ((right instanceof JArrayAccessExpression)&&(((JArrayAccessExpression)right).getAccessor() instanceof JIntLiteral)) {
                            if (right.getType()!=null) {
                                // looks like the type of an array
                                // access expression can either be a
                                // primitive or an array type... I
                                // don't understand exactly how this
                                // works (--bft)
                                if (right.getType() instanceof CArrayType) {
                                    type = ((CArrayType)right.getType()).getBaseType();
                                } else {
                                    type = right.getType();
                                }
                            } else if (left.getType()!=null) { 
                                // I don't know if left type could
                                // ever be null, just extending
                                // jasper's code and trying to be
                                // careful not to change semantics
                                // that were there
                                if (left.getType() instanceof CArrayType) {
                                    type = ((CArrayType)left.getType()).getBaseType();
                                } else {
                                    type = left.getType();
                                }
                            }
                        }
                        if (type!=null) {
                            JVariableDefinition var=new JVariableDefinition(self.getTokenReference(),0,type,propName(),right);
                            JVariableDeclarationStatement newState=new JVariableDeclarationStatement(self.getTokenReference(),var,null);
                            self.addStatement(i++,newState);
                            size++;
                            ((JAssignmentExpression)expr).setCopyVar(new JLocalVariableExpression(self.getTokenReference(),var));
                        }
                    }
                }
                /*while(mutated.size()!=0) {
                  JLocalVariable var=(JLocalVariable)mutated.removeFirst();
                  size++;
                  Object val=constants.get(var);
                  if(val!=null)
                  self.addStatement(i++,new JExpressionStatement(null,new JAssignmentExpression(null,new JLocalVariableExpression(null,var),(JLiteral)val),null));
                  else {
                  System.err.println("WARNING: Unknown Mutated Value For "+var);
                  self.addStatement(i++,new JExpressionStatement(null,new JLocalVariableExpression(null,var),null));
                  }
                  }*/
            }
        }
        int size=self.size();
        for(int i=0;i<size;i++) {
            JStatement oldBody=(JStatement)self.getStatement(i);
            Object newBody = oldBody.accept(this);
            if (!(newBody instanceof JStatement))
                continue;
            if (newBody!=null && newBody!=oldBody) {
                self.setStatement(i,(JStatement)newBody);
            }
            while(mutated.size()!=0) {
                JLocalVariable var=mutated.removeFirst();
                size++;
                Object val=constants.get(var);
                if(val!=null)
                    self.addStatement(i++,new JExpressionStatement(null,new JAssignmentExpression(null,new JLocalVariableExpression(null,var),(JLiteral)val),null));
                else {
                    System.err.println("WARNING: Unknown Mutated Value For "+var);
                    self.addStatement(i++,new JExpressionStatement(null,new JLocalVariableExpression(null,var),null));
                }
            }
        }
        visitComments(comments);
        return self;
    }
    
    //Visit the block starting from index statement
    /*public Object visitBlockStatement(int index,
      JBlock self) {
      for (;index<self.size();index++) {
      JStatement oldBody = self.getStatement(index);
      Object newBody = oldBody.accept(this);
      if (!(newBody instanceof JStatement))
      continue;
      if (newBody!=null && newBody!=oldBody) {
      self.setStatement(index,(JStatement)newBody);
      }
      }
      return self;
      }*/

    private String propName() {
        return TEMP_VARIABLE_BASE+propNum++;
    }

//    private boolean propVarLocal(JLocalVariable var) {
//        return var.getIdent().startsWith(TEMP_VARIABLE_BASE);
//    }

    private boolean propVar(Object var) {
        if(!(var instanceof JExpression))
            System.err.println("WARNING:popVar:"+var);
        if(var instanceof JLocalVariableExpression)
            return ((JLocalVariableExpression)var).getVariable().getIdent().startsWith(TEMP_VARIABLE_BASE);
        return false;
    }
        
    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * Visits an array length expression
     */
    public Object visitSwitchLabel(JSwitchLabel self,
                                   JExpression expr) {
        if (expr != null) {
            JExpression newExp = (JExpression)expr.accept(this);
            if (newExp!=expr) {
                self.setExpression(newExp);
            }
        }
        return self;
    }

    /**
     * Visits a set of arguments
     */
    public Object visitArgs(JExpression[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                JExpression newExp = (JExpression)args[i].accept(this);
                if (newExp!=args[i]) {
                    args[i] = newExp;
                }
            }
        }
        return null;
    }

}
