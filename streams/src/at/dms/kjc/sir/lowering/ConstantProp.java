package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
//import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.common.ArrayCopy;
//import at.dms.kjc.lir.*;
//import at.dms.compiler.JavaStyleComment;
/**
 * This class propagates constants and unrolls loops.  Currently only
 * works for init functions.
 * 
 * (Bill Thies &amp; Allyn Dimock: 2005-12-22  now works for all SIRStreams.)
 */
public class ConstantProp {

    private ConstantProp(boolean removeDeadFields) {
        this.removeDeadFields = removeDeadFields;
    }

    private boolean removeDeadFields;
    
    /**
     * Propagates constants as far as possible in <str> and also
     * unrolls loops.
     *
     * @param str a SIRStream.  Propagation occurs in this and all reachable.
     */
    public static void propagateAndUnroll(SIRStream str) {
        // start at the outermost loop with an empty set of constants
        new ConstantProp(false).propagateAndUnroll(str, new Hashtable());
    }

    /**
     * Propagates constants as far as possible in <str> and also
     * unrolls loops.
     *
     *  @param str a SIRStream.  Propagation occurs in this and all reachable.
     *  @param removeDeadFields whether FieldProp should remove fully-propagated fields.
     */  
    public static void propagateAndUnroll(SIRStream str, boolean removeDeadFields) {
        // start at the outermost loop with an empty set of constants
        new ConstantProp(removeDeadFields).propagateAndUnroll(str, new Hashtable());
    }
    /**
     * Does the work on <str>, given that <constants> maps from
     * a JLocalVariable to a JLiteral for all constants that are known.
     */
    private void propagateAndUnroll(SIRStream str, Hashtable constants) {
        Unroller unroller;
        do {
            if(KjcOptions.struct&&!Flattener.INIT_STATEMENTS_RESOLVED) {
                StructDestroyer dest=new StructDestroyer();
                JMethodDeclaration[] methods=str.getMethods();
                for(int i=0;i<methods.length;i++)
                    methods[i].accept(dest);
                dest.addFields(str);
            }
            FieldProp.doPropagateNotRecursive(str, false, removeDeadFields);
            // make a propagator
            Propagator propagator = new Propagator(constants);
            // propagate constants within methods of <str>
            JMethodDeclaration[] methods=str.getMethods();
            for(int i=0; i<methods.length; i++) {
                methods[i].accept(propagator);
                methods[i].accept(new VarDeclRaiser());
            }
            // propagate into fields of <str>
            propagateFields(propagator, str);
            // unroll loops within init function of <str>
            unroller = new Unroller(constants);
            // only unroll maximally for containers
            boolean oldInit=unroller.getContainerInit();
            unroller.setContainerInit(str instanceof SIRContainer
                                      || str instanceof SIRGlobal);
            str.getInit().accept(unroller);
            unroller.setContainerInit(oldInit);
            // patch list of children for splitjoins and pipelines
            if (unroller.hasUnrolled()) {
                if (str instanceof SIRPipeline) {
                    ((SIRPipeline)str).setChildren(GetChildren.
                                                   getChildren(str));
                } else if (str instanceof SIRSplitJoin) {
                    ((SIRSplitJoin)str).setParallelStreams(GetChildren.
                                                           getChildren(str));
                }
            }
            // iterate until nothing unrolls
        } while (unroller.hasUnrolled());   
        // if <str> is a container, recurse from it to its sub-streams
        if (str instanceof SIRContainer) {
            recurseFrom((SIRContainer)str, constants);
        }
    }

    /**
     * Given a propagator <propagator>, this propagates constants
     * through the fields of <str>.
     */
    private void propagateFields(Propagator propagator, SIRStream str) {
        // in actual field declarations, look for array dimensions
        // that refer to fields
        JFieldDeclaration[] fields = str.getFields();
        for (int i=0; i<fields.length; i++) {
            fields[i].accept(propagator);
        }

        if (str instanceof SIRPhasedFilter) {
            propagateFilterFields(propagator, (SIRPhasedFilter)str);
        } else if (str instanceof SIRSplitJoin) {
            // for split-joins, resolve the weights of splitters and
            // joiners
            propagator.visitArgs(((SIRSplitJoin)str).
                                 getJoiner().getInternalWeights());
            propagator.visitArgs(((SIRSplitJoin)str).
                                 getSplitter().getInternalWeights());
        } else if (str instanceof SIRFeedbackLoop) {
            // for feedback loops, resolve the weights of splitters, 
            // joiners, and delay
            SIRFeedbackLoop loop = (SIRFeedbackLoop)str;
            propagator.visitArgs(loop.getJoiner().getInternalWeights());
            propagator.visitArgs(loop.getSplitter().getInternalWeights());
            JExpression newDelay = loop.getDelay();
            if (newDelay != null)
                newDelay = (JExpression)newDelay.accept(propagator);
            if (newDelay!=null && newDelay!=loop.getDelay()) {
                loop.setDelay(newDelay);
            }
        }
    }

    /**
     * Use <propagator> to propagate constants into the fields of <filter>
     */
    private void propagateFilterFields(Propagator propagator, 
                                       SIRPhasedFilter filter) {
        for (int i=0; i<filter.getMethods().length; i++) {
            JMethodDeclaration method = filter.getMethods()[i];
        
            // propagate to pop expression
            JExpression newPop = (JExpression)method.getPop().accept(propagator);
            if (newPop!=null && newPop!=method.getPop()) {
                method.setPop(newPop);
            }
            // propagate to peek expression
            JExpression newPeek = (JExpression)method.getPeek().accept(propagator);
            if (newPeek!=null && newPeek!=method.getPeek()) {
                method.setPeek(newPeek);
            }
            // propagate to push expression
            JExpression newPush = (JExpression)method.getPush().accept(propagator);
            if (newPush!=null && newPush!=method.getPush()) {
                method.setPush(newPush);
            }
        }

        if (filter instanceof SIRPredefinedFilter) {
            // propagate predefined fields
            ((SIRPredefinedFilter)filter).propagatePredefinedFields(propagator);
        }
    }

    /**
     * Recurses from <str> into all its substreams.
     */
    private void recurseFrom(SIRContainer str, Hashtable constants) {
        // if we're at the bottom, we're done
        if (str.getInit()==null) {
            return;
        }
        // recursion method depends on whether or not there are still
        // init statements
        if (Flattener.INIT_STATEMENTS_RESOLVED) {
            //System.err.println("Recurse From 2:"+str);
            for (int i=0; i<str.size(); i++) {
                recurseInto(str.get(i), str.getParams(i), constants);
            }
        } else {
            // iterate through statements of init function, looking for
            // SIRInit's
            //System.err.println("Recurse From 1:"+str+" {");
            InitPropagator prop=new InitPropagator(constants);
            str.getInit().accept(prop);
            constants=prop.getConstants();
            //System.err.println("}");
            //for (int i=0; i<str.size(); i++) {
            //recurseInto(str.get(i), str.getParams(i), constants);
            //}
        }
    }

    class InitPropagator extends Propagator {
        public InitPropagator(Hashtable constants) {
            super(constants);
        }

        public InitPropagator(Hashtable constants,boolean write) {
            super(constants,write);
        }
    
        public Propagator construct(Hashtable constants) {
            return new InitPropagator(constants);
        }
    
        public Propagator construct(Hashtable constants,boolean write) {
            return new InitPropagator(constants,write);
        }
    
        public Object visitInitStatement(SIRInitStatement oldSelf,
                                         SIRStream oldTarget) {
            SIRInitStatement self=(SIRInitStatement)super.visitInitStatement(oldSelf,oldTarget);
            SIRStream target = self.getTarget();
            // if the target is a recursive stub, then expand it one level
            //System.err.println("Expanding: "+self.getTarget());
            //System.err.println(self.getArgs());
            if (target instanceof SIRRecursiveStub) {
                target=((SIRRecursiveStub)target).expand();
                self.setTarget(target);
            }
            recurseInto(self.getTarget(), self.getArgs(), constants);
            return self;
        }
        /* 
           public Object visitAssignmentExpression(JAssignmentExpression self,
           JExpression left,
           JExpression right) {
           return self;
           }
        */
    }
    
    /**
     * Recurses into <str> given that it is instantiated with
     * arguments <args>, and <constants> were built for the parent.
     */
    private void recurseInto(SIRStream str, List args, Hashtable constants) {
        JMethodDeclaration initMethod = str.getInit();
        // if there is no init function, we're done
        //System.err.println("   Recursing into:"+str);
        if (initMethod==null) {
            return;
        }
        //List argCopy=(List)args.clone();
        JFormalParameter[] parameters = initMethod.getParameters();
        //System.err.println("Recursing into "+str+" "+args+" "+parameters.length);
        // build new constants

        // note, this is tricky!  for the case of predefined filters,
        // the parameters might be shorter than the argument list.
        // e.g, for filereaders, we passed the name of the file but
        // the parameter does not appear in <init>.  so take the
        // shorter list.
        int size=parameters.length;
        for (int i=0; i<size; i++) {
            //System.err.println("Arg "+i+"="+args.get(i));
            // if we are passing an arg to the init function...
            if (args.get(i) instanceof JLiteral) {
                //System.err.println("!! top finding " + parameters[i].getIdent() + " " + parameters[i].hashCode() + " = " + args.get(i) + " in call to " + str.getIdent());
                // if it's already a literal, record it
                constants.put(parameters[i], (JLiteral)args.get(i));
            } else if (args.get(i) instanceof SIRPortal ||
                       (args.get(i) instanceof JLocalVariableExpression &&
                        ((JLocalVariableExpression)args.get(i)).getType() instanceof CClassType &&
                        ((JLocalVariableExpression)args.get(i)).getType().getCClass().getIdent().endsWith("Portal"))) {
                // similarly.
                constants.put(parameters[i], args.get(i));
            } else if ((args.get(i) instanceof JLocalVariableExpression)&&constants.get(((JLocalVariableExpression)args.get(i)).getVariable())!=null) {
                // otherwise if it's associated w/ a literal, then
                // record that and set the actual argument to be a literal
                Object constant = constants.get(((JLocalVariableExpression)args.get(i)).getVariable());
                //System.err.println("!! bottom finding " + parameters[i].getIdent() + " " + parameters[i].hashCode() + " = " + constant + " in call to " + str.getIdent());
                constants.put(parameters[i],constant);
                if(constant instanceof JExpression)
                    args.set(i, constant);
                //args.remove(i);
                //i--;
                //size--;
            }
        }
        ArrayCopy.acceptInit(str.getInit(),constants); 
        // recurse into sub-stream
        propagateAndUnroll(str, constants);
    }

    /**
     * This class is for rebuilding the list of children in a parent
     * stream following unrolling that could have modified the stream
     * structure.
     */
    static class GetChildren extends SLIREmptyVisitor {
        /**
         * List of children of parent stream.
         */
        private LinkedList<Object> children;
        /**
         * The parent stream.
         */
        private SIRStream parent;
        
        /**
         * Makes a new one of these.
         */
        private GetChildren(SIRStream str) {
            this.children = new LinkedList<Object>();
            this.parent = str;
        }

        /**
         * Re-inspects the init function of <str> to see who its children
         * are.
         */
        public static LinkedList<Object> getChildren(SIRStream str) {
            GetChildren gc = new GetChildren(str);
            if (str.getInit()!=null) {
                str.getInit().accept(gc);
            }
            return gc.children;
        }

        /**
         * Visits an init statement -- adds <target> to list of children.
         */
        public void visitInitStatement(SIRInitStatement self,
                                       SIRStream target) {
            // remember <target> as a child
            children.add(target);
            // reset parent of <target> 
            target.setParent((SIRContainer)parent);
        }
    }

}
