package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.LinkedList;

/**
 * This serves to flatten a stream structure by encapsulating the
 * state in a set of closures instead of as a hierarchy.
 */
public class Flattener implements SIRVisitor {

  /* list of the class declarations defined during traversal */
  private LinkedList structs;

  /**
   * Creates a new flattener.
   */
  private Flattener() {
    this.structs = new LinkedList();
  }

  /**
   * Returns an array of class declaration's corresponding to the
   * state structures used within <toplevel>.  Also mutates the
   * stream structure within <toplevel> so that each function within
   * a stream takes its state as the first parameter, and references
   * fields via the state.
   */
  public static JClassDeclaration[] flatten(SIRStream toplevel) {
    Flattener f = new Flattener();
    toplevel.accept(f);
    return (JClassDeclaration[])f.structs.toArray();
  }

  /* visit a pipeline */
  public void visitPipeline(SIRPipeline self,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init) {
  }

  /* visit a splitjoin */
  public void visitSplitJoin(SIRSplitJoin self,
			     JFieldDeclaration[] fields,
			     JMethodDeclaration[] methods,
			     JMethodDeclaration init) {
  }

  /* visit a filter */
  public void visitFilter(SIRFilter self,
			  JFieldDeclaration[] fields,
			  JMethodDeclaration[] methods,
			  JMethodDeclaration init,
			  int peek, int pop, int push,
			  JMethodDeclaration work,
			  CType inputType, CType outputType) {
  }
  
  /* visit a feedbackloop */
  public void visitFeedbackLoop(SIRFeedbackLoop self,
				JFieldDeclaration[] fields,
				JMethodDeclaration[] methods,
				JMethodDeclaration init,
				int delay,
				JMethodDeclaration initPath) {
  }

  /* visit a splitter */
  public void visitSplitter(SIRSplitter self,
			    SIRSplitType type,
			    int[] weights) {
  }

  /* visit a joiner */
  public void visitJoiner(SIRJoiner self,
			  SIRJoinType type,
			  int[] weights) {
  }

}
