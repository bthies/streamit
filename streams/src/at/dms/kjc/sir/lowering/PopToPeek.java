package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.util.*;
/**
 * This class converts all pops in a program into peeks. It does not reinvent
 * the wheel to handle control flow, instead it merely replaces each pop expression
 * with peek(index++), and then sticks in a bunch of pops statements at the end
 * of the work function to perform all of the pops at once. We then rely on
 * the constant prop and unroller pass to take care of figuring out compile
 * time constants. Note, there is the potential to make incorrect programs
 * (eg ones that don't pop as much as they claim) correct, but we aren't all that
 * worried at the moment because an incorrect program imples undefined compiler behavior.
 **/
public class PopToPeek extends EmptyStreamVisitor implements Constants {
    static final String PEEK_INDEX_NAME = "__peekIndex";
    /** use removeAllPops instead. **/
    private PopToPeek() {super();}
    
    /** Removes all pops in the work functions of the filters contained within str. **/
    public static void removeAllPops(SIRStream str) {
	IterFactory.createFactory().createIter(str).accept(new PopToPeek());
    }
    
    /** visit a filter and do the transformation on it. **/
    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
	// if the work function is null, our work here is done.
	if (self.getWork() != null) {
	    doIt(self, self.getWork().getBody(), self.getPopInt());
	}
	//if this is a two stage filter, we must do the same for
	//the preWork function
	if (self instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter two = (SIRTwoStageFilter)self;
	    if (two.getInitWork() != null)
		doIt(self, two.getInitWork().getBody(), two.getInitPop());
	}
    }

    private void doIt(SIRFilter self, JBlock workBody, int pops) {
	// create a variable definition for the index counter
	JVariableDefinition indexVarDef = new JVariableDefinition(null, // tokenReference
								  0, // modifiers, 0=none, I hope
								  CStdType.Integer, // type
								  PEEK_INDEX_NAME, // name
								  new JIntLiteral(0)); // initializer
	JVariableDeclarationStatement declStatement = new JVariableDeclarationStatement(null, // token reference
											indexVarDef, // var definition
											null); // comments

	
	// stick in the decl statement at the front of the block
	workBody.addStatementFirst(declStatement);

	// now, make a new visitor with the indexVar and pump it through the work function
	workBody.accept(new PopToPeekVisitor(indexVarDef, self.getInputType()));

	// add the appropriate number of pops on to the end of the work function.
	// we do not need the pops when going thru the raw backend
	if (KjcOptions.raw == -1)
	    workBody.addAllStatements(makePopStatements(pops, self.getInputType()));
    }
    
    /** returns a list of pop statements that are appended on the end of the work function. **/
    public List makePopStatements(int popCount, CType tapeType) {
	LinkedList popList = new LinkedList();
	for (int i=0; i<popCount; i++) {
	    popList.add(new JExpressionStatement(null, new SIRPopExpression(tapeType), null));
	}
	return popList;
    }
    
    /** inner class that actually visits all internal nodes and does the replacing. **/
    class PopToPeekVisitor extends SLIRReplacingVisitor implements Constants {
	/** field to hold the variable used for peek indexes. **/
	JLocalVariable indexVariable;
	/** the type of the input tape. **/
	CType inputTapeType;

	/** create a new PeekToPopVisitor to do the actual replacing. **/
	PopToPeekVisitor(JLocalVariable indexVar, CType inputType) {
	    this.indexVariable = indexVar;
	    this.inputTapeType = inputType;
	}

	/** visit a pop expression and convert it into a peek expression. **/
	public Object visitPopExpression(SIRPopExpression self, CType tapeType) {
	    // the basic scheme -- add a peek at the current index var
	    // and then add a indexvar increment. 

	    JExpression incExpr  = new JPostfixExpression(null, // token reference
							  OPE_POSTINC, // x++
							  new JLocalVariableExpression(null, this.indexVariable));
	    JExpression peekExpr = new SIRPeekExpression(incExpr,
							 inputTapeType);
	    return peekExpr;
	}

	/** Visit a peek expression and change the index to be the current value+index. **/
	public Object visitPeekExpression(SIRPeekExpression self, CType tapeType, JExpression arg) {
	    // push ourselves through the current peek expression's argument
	    JExpression visitedExpr = (JExpression)arg.accept(this);
	    	    
	    // assemble a new add expression with the visited expression
	    // and the indexvar
	    JExpression indexExpr = new JLocalVariableExpression(null, this.indexVariable);
	    JExpression newExpr = new JAddExpression(null, // token reference
						     visitedExpr, // left
						     indexExpr); // right

	    // change the peek expression's argument
	    self.setArg(newExpr);
	    // and we are done
	    return self;
	}
          
	
    }
    
	




}





