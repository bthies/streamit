package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.raw.ArrayCopy;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This class propagates constants and unrolls loops.  Currently only
 * works for init functions.
 */
public class ConstantProp {

    private ConstantProp() {
    }

    /**
     * Propagates constants as far as possible in <str> and also
     * unrolls loops.
     */
    public static void propagateAndUnroll(SIRStream str) {
	// start at the outermost loop with an empty set of constants
	new ConstantProp().propagateAndUnroll(str, new Hashtable());
    }

    /**
     * Does the work on <str>, given that <constants> maps from
     * a JLocalVariable to a JLiteral for all constants that are known.
     */
    private void propagateAndUnroll(SIRStream str, Hashtable constants) {
	Unroller unroller;
	do {
	    // make a propagator
	    Propagator propagator = new Propagator(constants);
	    // propagate constants within init function of <str>
	    str.getInit().accept(propagator);
	    // propagate into fields of <str>
	    propagateFields(propagator, str);
	    // Copy Arrays
	    //ArrayCopy.acceptInit(str.getInit(),constants);
	    // Raise Vars
	    str.getInit().accept(new VarDeclRaiser());	
	    // propagate constants within work function of <str>
	    JMethodDeclaration work= str.getWork();
	    if(work!=null) {
		work.accept(propagator);
		work.accept(new VarDeclRaiser());
	    }
	    // unroll loops within init function of <str>
	    unroller = new Unroller(constants);
	    // only unroll maximally for containers
	    unroller.setContainerInit(str instanceof SIRContainer);
	    str.getInit().accept(unroller);
	    unroller.setContainerInit(false);
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
	if (str instanceof SIRFilter) {
	    propagateFilterFields(propagator, (SIRFilter)str);
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
				       SIRFilter filter) {
	// propagate to pop expression
	JExpression newPop = (JExpression)filter.getPop().accept(propagator);
	if (newPop!=null && newPop!=filter.getPop()) {
	    filter.setPop(newPop);
	}
	// propagate to peek expression
	JExpression newPeek = (JExpression)filter.getPeek().accept(propagator);
	if (newPeek!=null && newPeek!=filter.getPeek()) {
	    filter.setPeek(newPeek);
	}
	// propagate to push expression
	JExpression newPush = (JExpression)filter.getPush().accept(propagator);
	if (newPush!=null && newPush!=filter.getPush()) {
	    filter.setPush(newPush);
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
	    if (target instanceof SIRRecursiveStub) {
		self.setTarget(((SIRRecursiveStub)target).expand());
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
	// build new constants
	int size=args.size();
	for (int i=0; i<size; i++) {
	    //System.err.println("Arg "+i+"="+args.get(i));
	    // if we are passing an arg to the init function...
	    if (args.get(i) instanceof JLiteral) {
		//System.err.println("!! top finding " + parameters[i].getIdent() + " " + parameters[i].hashCode() + " = " + args.get(i) + " in call to " + str.getIdent());
		// if it's already a literal, record it
		constants.put(parameters[i], (JLiteral)args.get(i));
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
	private LinkedList children;
	/**
	 * The parent stream.
	 */
	private SIRStream parent;
        
	/**
	 * Makes a new one of these.
	 */
	private GetChildren(SIRStream str) {
	    this.children = new LinkedList();
	    this.parent = str;
	}

	/**
	 * Re-inspects the init function of <str> to see who its children
	 * are.
	 */
	public static LinkedList getChildren(SIRStream str) {
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
