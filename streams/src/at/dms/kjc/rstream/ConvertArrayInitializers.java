package at.dms.kjc.rstream;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Vector;
import at.dms.util.Utils;

/**
 * This class *partially* converts array initializers to a bunch of assignment
 * statements.  First it removes the array intializer from ae declaration and 
 * replaces it with a new array expression of the correct size.  The it
 * remembers the array initializer in a hashmap indexed by the var.
 *
 * @author Michael Gordon
 * 
 */

public class ConvertArrayInitializers extends SLIRReplacingVisitor implements FlatVisitor
{
    /** Hashset mapping String -> JBlock to perform initialization of array **/
    public HashSet fields;
    /** Hashset mapping JLocalVariable -> JBlock to perform initialization of array **/
    public HashMap locals;
    /** current method we are visiting **/
    private JMethodDeclaration method;
    

    /**
     * Create a new object and visit the ir starting at node.  Convert
     * array initializers to new array expressions and remembers
     * a sequence of assignments expressions that will perform the initialization.
     *
     * @param node The top level flat node.
     *
     *
     */
    public ConvertArrayInitializers(FlatNode node) 
    {
	System.out.println("Converting Array Initializers...");
	fields = new HashSet();
	locals = new HashMap();
	node.accept(this, null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()) {
	    SIRFilter filter = (SIRFilter)node.contents;

	    for (int i = 0; i < filter.getMethods().length; i++) {
		filter.getMethods()[i].accept(this);
	    }
	    
	    for (int i = 0; i < filter.getFields().length; i++) {
		filter.getFields()[i].accept(this);
	    }
	}
    }


    public Object visitMethodDeclaration(JMethodDeclaration self,
					 int modifiers,
					 CType returnType,
					 String ident,
					 JFormalParameter[] parameters,
					 CClassType[] exceptions,
					 JBlock body) {
	//remember what method we are currently visiting...
	method = self;
	for (int i = 0; i < parameters.length; i++) {
	    if (!parameters[i].isGenerated()) {
		parameters[i].accept(this);
	    }
	}
	if (body != null) {
	    body.accept(this);
	}
	method = null;
	return self;
    }



     /**
      * Visit a local var def and remove and convert the array initializer.
     */
    public Object visitVariableDefinition(JVariableDefinition self,
					  int modifiers,
					  CType type,
					  String ident,
					  JExpression expr) {
	if (expr != null) {
	    if (expr instanceof JArrayInitializer) {
		if (!locals.containsKey(method))
		    locals.put(method, new JBlock(null, new JStatement[0], null));
		
		JBlock initBlock = (JBlock)locals.get(method);
		arrayInitBlock(self, (JArrayInitializer)expr, 
			       initBlock,
			       new Vector());
		
		//something about method should go here...

		self.setValue(handleArrayInitializer(ident, 
						     (JArrayInitializer)expr, getBaseType(type)));
	    } else {
		JExpression newExp = (JExpression)expr.accept(this);
		if (newExp!=null && newExp!=expr) {
		    self.setValue(newExp);
		}
	    }
	}
	return self;
    }
    
    /**
     * visit a field decl, remove and convert the array initializer
     */
    public Object visitFieldDeclaration(JFieldDeclaration self,
					int modifiers,
					CType type,
					String ident,
					JExpression expr) {
	if (expr != null) {
	    if (expr instanceof JArrayInitializer) {
		//if we have an array initializer
		JBlock initBlock = new JBlock(null, new JStatement[0], null);
		//get the block that will perform the initializers assignments
		arrayInitBlock(ident, (JArrayInitializer)expr, 
			       initBlock,
			       new Vector());
		//remember the block
		fields.add(initBlock);
		//set the initialization expression to a new array expression
		self.setValue(handleArrayInitializer(ident, 
						     (JArrayInitializer)expr, getBaseType(type)));
	    }
	    else {
		JExpression newExp = (JExpression)expr.accept(this);
		if (newExp!=null && newExp!=expr) {
		    self.setValue(newExp);
		}
		
		expr.accept(this);
	    }
	}
	return self;
    }
    
    
    /**
     * generate a new array expressin based on the array initializer
     **/
    private JNewArrayExpression handleArrayInitializer(String ident,
						       JArrayInitializer expr,
						       CType baseType) 
    {
	Vector dims = new Vector();
	//get the number of dims and the bound for each dim by looking 
	//at the jarrayinitializer expressions
	getDims(expr, dims);
	return new JNewArrayExpression(null, baseType, 
				       (JExpression[])dims.toArray(new JExpression[0]),
				       null);
    }

    /**
     * Convert the array initializer into a sequence of assignment statements in a block
     **/
    private void arrayInitBlock(Object varAccess, JArrayInitializer init, JBlock block, 
				Vector indices)
    {
	for (int i = 0; i < init.getElems().length; i++) {
	    //recurse through the array initializer statements, building
	    //the array access indices as we recurse
	    if (init.getElems()[i] instanceof JArrayInitializer) {
		Vector indices1 = new Vector(indices);
		indices1.add(new JIntLiteral(i));
		arrayInitBlock(varAccess, (JArrayInitializer)init.getElems()[i], block,
				    indices1);

	    }
	    else {
		//otherwise we have an expression that is supposed to be assigned
		//to the calculated array element
		JExpression prefix;
		
		assert varAccess instanceof String || varAccess instanceof JLocalVariable;
		
		if (varAccess instanceof String)
		    prefix = new JFieldAccessExpression(null, 
							(String)varAccess);
		else 
		    prefix = new JLocalVariableExpression(null,
							  (JLocalVariable)varAccess);
		
		//build the final array access expression
		JArrayAccessExpression access = 
		    new JArrayAccessExpression(null, 
					       prefix,
					       new JIntLiteral(i));
		//build the remaining, from bottom up
		for (int j = indices.size() - 1; j >= 0; j--) {
		    access = new JArrayAccessExpression(null, 
							access,
							(JExpression)indices.get(j));
		}

		//only handle constant initializers for now
		assert (Utils.passThruParens(init.getElems()[i]) instanceof JLiteral) : 
		    "Error: Currently we only handle constant array initializers.";
			 
		//generate the assignment statement 
		block.addStatement(new JExpressionStatement
				   (null,
				    new JAssignmentExpression(null,
							      access,
							      init.getElems()[i]),
				    null));
	    }
	}
    }
    
    
    /**
     * get the dimensionality and bounds based on the array initializer
     **/
    private void getDims(JExpression expr, Vector dims) 
    {
	if (expr instanceof JArrayInitializer) {
	    JArrayInitializer init = (JArrayInitializer)expr;
	    dims.add(new JIntLiteral(init.getElems().length));
	    if (init.getElems().length > 0)
		getDims(init.getElems()[0], dims);
	}
    }
    
    /**
     * get the element type of an array
     **/
    private CType getBaseType(CType type) 
    {
	if (type.isArrayType()) 
	    return getBaseType(((CArrayType)type).getBaseType());
	return type;
    }
    
    /**
     */
    public Object visitArrayInitializer(JArrayInitializer self,
					JExpression[] elems)
    {
	assert false : "Visitor should not see JArrayInitializer";
	return self;
    }
}
