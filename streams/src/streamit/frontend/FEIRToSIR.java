package streamit.frontend;

import java.util.*;
import streamit.frontend.nodes.*;
import streamit.frontend.tojava.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;

public class FEIRToSIR implements FEVisitor {
  private SIRStream topLevel;
  private SIRStream parent;
  private Hashtable classTable;

  public FEIRToSIR() {
    classTable = new Hashtable();
  }
  
  public void debug(String s) {
    System.err.print(s);
  }
  
  public Object visitProgram(Program p) {
    /* We visit each stream and each struct */
    List feirStreams;
    List feirStructs;
    List sirStreams;
    List sirStructs;
    Iterator iter;

    parent = null;

    debug("In visitProgram\n");
    
    feirStructs = p.getStructs();
    sirStructs = new LinkedList();
    for (iter = feirStructs.iterator(); iter.hasNext(); ) {
      sirStructs.add(visitTypeStruct((TypeStruct) iter.next()));
    }
    
    feirStreams = p.getStreams();
    sirStreams = new LinkedList();
    for (iter = feirStreams.iterator(); iter.hasNext(); ) {
      sirStreams.add(((FENode) iter.next()).accept(this));
    }

    return topLevel;
  }

  public Object visitStreamSpec(StreamSpec spec) {
    SIRStream oldParent = parent;
    SIRStream current = null;

    debug("In visitStreamSpec\n");
    
    if (spec.getName() != null) {
      /* Not an anonymous stream */
      StreamType st = spec.getStreamType();
      
      if (st != null
	  && st.getIn() instanceof TypePrimitive
	  && ((TypePrimitive) st.getIn()).getType() == TypePrimitive.TYPE_VOID
	  && st.getOut() instanceof TypePrimitive
	  && ((TypePrimitive) st.getOut()).getType() == TypePrimitive.TYPE_VOID) {
	/* This it the top-level pipeline */
	SIRPipeline result = (SIRPipeline) visitPipelineSpec(spec);
	topLevel = result;
	current = result;
      } else {
	switch (spec.getType()) {
	case StreamSpec.STREAM_FILTER:
	  current = (SIRStream) visitFilterSpec(spec);
	  break;
	case StreamSpec.STREAM_PIPELINE:
	  current = (SIRStream) visitPipelineSpec(spec);
	  break;
	case StreamSpec.STREAM_SPLITJOIN:
	  current = (SIRStream) visitSplitJoinSpec(spec);
	  break;
	case StreamSpec.STREAM_FEEDBACKLOOP:
	  current = (SIRStream) visitFeedbackLoopSpec(spec);
	  break;
	}
      }

      debug("Adding " + spec.getName() + " to symbol table\n");
      classTable.put(spec.getName(), current);
    } else {
      /* Anonymous stream */
      switch (spec.getType()) {
      case StreamSpec.STREAM_FILTER:
	current = (SIRStream) visitFilterSpec(spec);
	break;
      case StreamSpec.STREAM_PIPELINE:
	current = (SIRStream) visitPipelineSpec(spec);
	break;
      case StreamSpec.STREAM_SPLITJOIN:
	current = (SIRStream) visitSplitJoinSpec(spec);
	break;
      case StreamSpec.STREAM_FEEDBACKLOOP:
	current = (SIRStream) visitFeedbackLoopSpec(spec);
	break;
      }
    }

    if (current != null) {
      return current;
    }

    debug("  This should never happen!\n");
    return null;
  }

  public List fieldDeclToJFieldDeclarations(FieldDecl decl) {
    debug("In fieldDeclToJFieldDeclarations\n");
    List result = new ArrayList();
    for (int i = 0; i < decl.getNumFields(); i++)
    {
        JVariableDefinition def = null;
        if (decl.getInit(i) != null) {
            def = new JVariableDefinition(null, // token reference
                                          at.dms.kjc.Constants.ACC_PUBLIC,
                                          feirTypeToSirType(decl.getType(i)),
                                          decl.getName(i),
                                          (JExpression) decl.getInit(i).accept(this));
        } else {
            def = new JVariableDefinition(null, // token reference
                                          at.dms.kjc.Constants.ACC_PUBLIC,
                                          feirTypeToSirType(decl.getType(i)),
                                          decl.getName(i),
                                          null);
        }
        JFieldDeclaration fDecl = new JFieldDeclaration(null, /* token reference */
                                                        def,
                                                        null, /* javadoc */
                                                        null); /* comments */
        result.add(fDecl);
    }
    return result;
  }

    private void setStreamFields(SIRStream stream, List vars)
    {
        JFieldDeclaration[] fields = new JFieldDeclaration[0];
        List fieldList = new ArrayList();
        for (Iterator iter = vars.iterator(); iter.hasNext(); )
        {
            FieldDecl decl = (FieldDecl)iter.next();
            fieldList.addAll(fieldDeclToJFieldDeclarations(decl));
        }
        fields = (JFieldDeclaration[])fieldList.toArray(fields);
        stream.setFields(fields);
    }

  public SIRStream visitFilterSpec(StreamSpec spec) {
    int i;
    List list;
    SIRFilter result = new SIRFilter();
    SIRStream oldParent = parent;

    parent = result;
    
    debug("In visitFilterSpec\n");

    result.setIdent(spec.getName());
    result.setInputType(feirTypeToSirType(spec.getStreamType().getIn()));
    result.setOutputType(feirTypeToSirType(spec.getStreamType().getOut()));

    setStreamFields(result, spec.getVars());

    Function[] funcs = new Function[spec.getFuncs().size()];
    list = spec.getFuncs();
    for (i = 0; i < funcs.length; i++) {
      funcs[i] = (Function) list.get(i);
    }
    JMethodDeclaration[] methods = new JMethodDeclaration[funcs.length];

    for (i = 0; i < funcs.length; i++) {
      methods[i] = (JMethodDeclaration) visitFunction(funcs[i]);
      if (funcs[i].getCls() == Function.FUNC_WORK) {
	result.setWork(methods[i]);
	if (spec.getWorkFunc().getPopRate() != null) {
	  result.setPop((JExpression) spec.getWorkFunc().getPopRate().accept(this));
	} else {
	  result.setPop(0);
	}
	if (spec.getWorkFunc().getPeekRate() != null) {
	  result.setPeek((JExpression) spec.getWorkFunc().getPeekRate().accept(this));
	} else {
	  result.setPeek(result.getPop());
	}
	if (spec.getWorkFunc().getPushRate() != null) {
	  result.setPush((JExpression) spec.getWorkFunc().getPushRate().accept(this));
	} else {
	  result.setPush(0);
	}
      }
      if (funcs[i].getCls() == Function.FUNC_INIT) {
	result.setInit(methods[i]);
      }
    }
    result.setMethods(methods);

    parent = oldParent;

    return result;
  }

  public SIRStream visitPipelineSpec(StreamSpec spec) {
    int i;
    List list;
    SIRPipeline result = new SIRPipeline(spec.getName());
    SIRStream oldParent = parent;

    debug("In visitPipelineSpec\n");

    parent = result;
    
    setStreamFields(result, spec.getVars());

    Function[] funcs = new Function[spec.getFuncs().size()];
    list = spec.getFuncs();
    for (i = 0; i < funcs.length; i++) {
      funcs[i] = (Function) list.get(i);
    }
    JMethodDeclaration[] methods = new JMethodDeclaration[funcs.length];

    for (i = 0; i < funcs.length; i++) {
      methods[i] = (JMethodDeclaration) visitFunction(funcs[i]);
      if (funcs[i].getCls() == Function.FUNC_INIT) {
	result.setInit(methods[i]);
      }
    }
    result.setMethods(methods);

    parent = oldParent;

    return result;
  }

  public CType feirTypeToSirType(Type type) {
    debug("In feirTypeToSirType\n");
    if (type instanceof TypePrimitive) {
      TypePrimitive tp = (TypePrimitive) type;
      switch (tp.getType()) {
      case TypePrimitive.TYPE_BIT:
      case TypePrimitive.TYPE_INT:
	return CStdType.Integer;
      case TypePrimitive.TYPE_FLOAT:
	return CStdType.Float;
      case TypePrimitive.TYPE_DOUBLE:
	return CStdType.Double;
      case TypePrimitive.TYPE_COMPLEX:
        return new CClassNameType("Complex");
      case TypePrimitive.TYPE_VOID:
	return CStdType.Void;
      }
    } else if (type instanceof TypeArray) {
      TypeArray ta = (TypeArray) type;
      /* BUG: The length of the FEIR array is real and integral,
	 but not necessarily constant -- the variables in it
	 must be resolvable by constant propagation.  What to do? */
      return new CArrayType(feirTypeToSirType(ta.getBase()),
			    1);
    } else if (type instanceof TypeStruct) {
        return new CClassNameType(((TypeStruct)type).getName());
    } else if (type instanceof TypeStructRef) {
        return new CClassNameType(((TypeStructRef)type).getName());
    }
    /* This shouldn't happen */
    debug("  UNIMPLEMENTED - shouldn't happen\n");
    return null;
  }

  public SIRStructure visitTypeStruct(TypeStruct ts) {
    JFieldDeclaration[] fields = new JFieldDeclaration[ts.getNumFields()];
    int i;
    for (i = 0; i < fields.length; i++) {
      String name = ts.getField(i);
      Type type = ts.getType(name);
      JVariableDefinition def = new JVariableDefinition(null,
							at.dms.kjc.Constants.ACC_PUBLIC,
							feirTypeToSirType(type),
							name,
							null);
      fields[i] = new JFieldDeclaration(null,
					def,
					null,
					null);
    }
    debug("Adding " + ts.getName() + " to symbol table\n");
    SIRStructure struct = new SIRStructure(null,
					   ts.getName(),
					   fields);
    classTable.put(ts.getName(), struct);
    return struct;
  }

  public Object visitExprArray(ExprArray exp) {
    debug("In visitExprArray\n");
    return new JArrayAccessExpression(null,
				      (JExpression) exp.getBase().accept(this),
				      (JExpression) exp.getOffset().accept(this));
  }

  public Object visitExprBinary(ExprBinary exp) {
    debug("In visitExprBinary\n");
    switch(exp.getOp()) {
    case ExprBinary.BINOP_ADD:
      return new JAddExpression(null,
				(JExpression) exp.getLeft().accept(this),
				(JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_SUB:
      return new JMinusExpression(null,
				  (JExpression) exp.getLeft().accept(this),
				  (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_MUL:
      return new JMultExpression(null,
				 (JExpression) exp.getLeft().accept(this),
				 (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_DIV:
      return new JDivideExpression(null,
				   (JExpression) exp.getLeft().accept(this),
				   (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_MOD:
      return new JModuloExpression(null,
				   (JExpression) exp.getLeft().accept(this),
				   (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_AND:
      return new JConditionalAndExpression(null,
					   (JExpression) exp.getLeft().accept(this),
					   (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_OR:
      return new JConditionalOrExpression(null,
					  (JExpression) exp.getLeft().accept(this),
					  (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_EQ:
      return new JEqualityExpression(null,
				     true, /* == */
				     (JExpression) exp.getLeft().accept(this),
				     (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_NEQ:
      return new JEqualityExpression(null,
				     false, /* != */
				     (JExpression) exp.getLeft().accept(this),
				     (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_LT:
      return new JRelationalExpression(null,
				       Constants.OPE_LT,
				       (JExpression) exp.getLeft().accept(this),
				       (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_LE:
      return new JRelationalExpression(null,
				       Constants.OPE_LE,
				       (JExpression) exp.getLeft().accept(this),
				       (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_GT:
      return new JRelationalExpression(null,
				       Constants.OPE_GT,
				       (JExpression) exp.getLeft().accept(this),
				       (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_GE:
      return new JRelationalExpression(null,
				       Constants.OPE_GE,
				       (JExpression) exp.getLeft().accept(this),
				       (JExpression) exp.getRight().accept(this));
    case ExprBinary.BINOP_BAND:
      return new JBitwiseExpression(null,
				    Constants.OPE_BAND,
				    (JExpression) exp.getLeft().accept(this),
				    (JExpression) exp.getRight().accept(this)); 
    case ExprBinary.BINOP_BOR:
      return new JBitwiseExpression(null,
				    Constants.OPE_BOR,
				    (JExpression) exp.getLeft().accept(this),
				    (JExpression) exp.getRight().accept(this)); 
    case ExprBinary.BINOP_BXOR:
      return new JBitwiseExpression(null,
				    Constants.OPE_BXOR,
				    (JExpression) exp.getLeft().accept(this),
				    (JExpression) exp.getRight().accept(this));
    }
    /* This shouldn't happen */
    debug("  Unhandled binary expression type\n");
    return null;
  }

  public Object visitExprComplex(ExprComplex exp) {
    debug("In visitExprComplex\n");
    debug("  UNIMPLEMENTED\n");
    /* unimplemented */
    return null;
  }

  public Object visitFieldDecl(FieldDecl decl) {
    debug("In visitFieldDecl\n");
    debug("  UNIMPLEMENTED\n");
    /* unimplemented */
    return null;
  }

  public Object visitExprConstBoolean(ExprConstBoolean exp) {
    debug("In visitExprConstBoolean\n");
    return new JBooleanLiteral(null, exp.getVal());
  }

  public Object visitExprConstChar(ExprConstChar exp) {
    debug("In visitExprConstChar\n");
    return new JCharLiteral(null, exp.getVal());
  }

  public Object visitExprConstFloat(ExprConstFloat exp) {
    debug("In visitExprConstFloat\n");
    return new JDoubleLiteral(null, exp.getVal());
  }

  public Object visitExprConstInt(ExprConstInt exp) {
    debug("In visitExprConstInt\n");
    return new JIntLiteral(exp.getVal());
  }

  public Object visitExprConstStr(ExprConstStr exp) {
    debug("In visitExprConstStr\n");
    return new JStringLiteral(null, exp.getVal());
  }

  public Object visitExprField(ExprField exp) {
    debug("In visitExprField\n");
    return new JFieldAccessExpression(null,
				      (JExpression) exp.getLeft().accept(this),
				      exp.getName());
  }

  public Object visitExprFunCall(ExprFunCall exp) {
    debug("In visitExprFunCall\n");
    List params = exp.getParams();
    JExpression[] args = new JExpression[params.size()];
    int i;
    for (i = 0; i < params.size(); i++) {
      args[i] = (JExpression) ((Expression) params.get(i)).accept(this);
    }
    if (exp.getName().equals("print") || exp.getName().equals("println")) {
      return new SIRPrintStatement(null, args[0], null);
    }
    return new JMethodCallExpression(null, exp.getName(), args);
  }

  public Object visitExprPeek(ExprPeek exp) {
    debug("In visitExprPeek\n");
    return new SIRPeekExpression((JExpression) exp.getExpr().accept(this),
				 parent.getInputType());
  }

  public Object visitExprPop(ExprPop exp) {
    debug("In visitExprPop\n");
    return new SIRPopExpression(parent.getInputType());
  }

  public Object visitExprTernary(ExprTernary exp) {
    debug("In visitExprTernary\n");
    return new JConditionalExpression(null,
				      (JExpression) exp.getA().accept(this),
				      (JExpression) exp.getB().accept(this),
				      (JExpression) exp.getC().accept(this));
  }

  public Object visitExprTypeCast(ExprTypeCast exp) {
    debug("In visitExprTypeCast\n");
    return new JCastExpression(null,
                               (JExpression) exp.getExpr().accept(this),
                               feirTypeToSirType(exp.getType()));
  }

  public Object visitExprUnary(ExprUnary exp) {
    debug("In visitExprUnary\n");
    switch (exp.getOp()) {
    case ExprUnary.UNOP_NOT:
      /* BUG: possibly...  it seems like you can't have a unary not! */
      debug("  Unhandled unary not\n");
      return null;
    case ExprUnary.UNOP_NEG:
      return new JUnaryMinusExpression(null,
				       (JExpression) exp.getExpr().accept(this));
    case ExprUnary.UNOP_PREINC:
      return new JPrefixExpression(null,
				   Constants.OPE_PREINC,
				   (JExpression) exp.getExpr().accept(this));
    case ExprUnary.UNOP_POSTINC:
      return new JPostfixExpression(null,
				    Constants.OPE_POSTINC,
				    (JExpression) exp.getExpr().accept(this));
    case ExprUnary.UNOP_PREDEC:
      return new JPrefixExpression(null,
				   Constants.OPE_PREDEC,
				   (JExpression) exp.getExpr().accept(this));
    case ExprUnary.UNOP_POSTDEC:
      return new JPostfixExpression(null,
				    Constants.OPE_POSTDEC,
				    (JExpression) exp.getExpr().accept(this));
    }
    /* this should never be reached */
    debug("  eep!\n");
    return null;
  }

  public Object visitExprVar(ExprVar exp) {
    debug("In visitExprVar\n");
    return new JFieldAccessExpression(null,
				      new JThisExpression(null, (CClass) null),
				      exp.getName());
  }

  public Object visitFunction(Function func) {
    debug("In visitFunction\n");
    int i;
    List list;
    Parameter[] feirParams = new Parameter[func.getParams().size()];
    list = func.getParams();
    for (i = 0; i < feirParams.length; i++) {
      feirParams[i] = (Parameter) list.get(i);
    }
    JFormalParameter[] sirParams = new JFormalParameter[feirParams.length];
    for (i = 0; i < feirParams.length; i++) {
      sirParams[i] = (JFormalParameter) visitParameter(feirParams[i]);
    }
    JMethodDeclaration result = new JMethodDeclaration(null, /* token reference */
						       at.dms.kjc.Constants.ACC_PUBLIC,
						       feirTypeToSirType(func.getReturnType()),
						       func.getName(),
						       sirParams,
						       CClassType.EMPTY,
						       statementToJBlock(func.getBody()),
						       null, /* javadoc */
						       null); /* comments */
    return result;
  }

  public Object visitParameter(Parameter p) {
    debug("In visitParameter\n");
    return new JFormalParameter(null,
				JLocalVariable.DES_PARAMETER,
				feirTypeToSirType(p.getType()),
				p.getName(),
				false); /* is NOT final */
  }

  public JBlock statementToJBlock(Statement stmt) {
    debug("In statementToJBlock\n");
    if (stmt instanceof StmtBlock) {
      /* If it's a block, return the block that visitStmtBlock returns */
      return (JBlock) visitStmtBlock((StmtBlock) stmt);
    } else {
      /* If it's a single statement, wrap it with a block */
      JBlock result = new JBlock();
      result.addStatement((JStatement) stmt.accept(this));
      return result;
    }
  }

  public Object visitSplitJoinSpec(StreamSpec spec) {
    int i;
    List list;
    SIRSplitJoin result = new SIRSplitJoin((SIRContainer) parent, spec.getName());
    SIRStream oldParent = parent;

    debug("In visitSplitJoinSpec\n");

    parent = result;
    
    setStreamFields(result, spec.getVars());
    
    Function[] funcs = new Function[spec.getFuncs().size()];
    list = spec.getFuncs();
    for (i = 0; i < funcs.length; i++) {
      funcs[i] = (Function) list.get(i);
    }
    JMethodDeclaration[] methods = new JMethodDeclaration[funcs.length];

    for (i = 0; i < funcs.length; i++) {
      methods[i] = (JMethodDeclaration) visitFunction(funcs[i]);
      if (funcs[i].getCls() == Function.FUNC_INIT) {
	result.setInit(methods[i]);
      }
    }
    result.setMethods(methods);

    parent = oldParent;

    return result;
  }

  public Object visitFeedbackLoopSpec(StreamSpec spec) {
    int i;
    List list;
    SIRFeedbackLoop result = new SIRFeedbackLoop((SIRContainer) parent, spec.getName());
    SIRStream oldParent = parent;

    debug("In visitFeedbackLoopSpec\n");

    parent = result;

    setStreamFields(result, spec.getVars());
    
    Function[] funcs = new Function[spec.getFuncs().size()];
    list = spec.getFuncs();
    for (i = 0; i < funcs.length; i++) {
      funcs[i] = (Function) list.get(i);
    }
    JMethodDeclaration[] methods = new JMethodDeclaration[funcs.length];

    for (i = 0; i < funcs.length; i++) {
      methods[i] = (JMethodDeclaration) visitFunction(funcs[i]);
      if (funcs[i].getCls() == Function.FUNC_INIT) {
	result.setInit(methods[i]);
      }
    }
    result.setMethods(methods);

    parent = oldParent;

    return result;
  }

  public Object visitPhasedFilterSpec(StreamSpec spec) {
    int i;
    List list;
    SIRPhasedFilter result = new SIRPhasedFilter(spec.getName());
    SIRStream oldParent = parent;

    debug("In visitPhasedFilterSpec\n");

    parent = result;
    
    setStreamFields(result, spec.getVars());
    
    Function[] funcs = new Function[spec.getFuncs().size()];
    list = spec.getFuncs();
    for (i = 0; i < funcs.length; i++) {
      funcs[i] = (Function) list.get(i);
    }
    JMethodDeclaration[] methods = new JMethodDeclaration[funcs.length];

    for (i = 0; i < funcs.length; i++) {
      methods[i] = (JMethodDeclaration) visitFunction(funcs[i]);
      if (funcs[i].getCls() == Function.FUNC_INIT) {
	result.setInit(methods[i]);
      }
    }
    result.setMethods(methods);

    parent = oldParent;

    return result;
  }

  public Object visitExpression(Expression exp) {
    debug("In visitExpression\n");
    debug("  UNIMPLEMENTED\n");
    debug("  " + exp + "\n");
    /* unimplemented */
    return null;
  }

  public Object visitFuncWork(FuncWork func) {
    debug("In visitFuncWork\n");
    debug("  UNIMPLEMENTED\n");
    /* unimplemented */
    return null;
  }

  public Object visitSCAnon(SCAnon sc) {
    debug("In visitSCAnon\n");
    SIRStream str = (SIRStream) visitStreamSpec(sc.getSpec());
    if (parent instanceof SIRContainer) {
      /* add this stream as a child */
      ((SIRContainer) parent).add(str);
    }
    return new SIRInitStatement(str);
  }

  public Object visitSCSimple(SCSimple sc) {
    debug("In visitSCSimple\n");
    if (! classTable.containsKey(sc.getName())) {
      /* BUG: What to do if they give a bad name? */
      debug("  Missing class in visitSCSimple\n");
      return null;
    }
    /* Translate the arguments */
    int i;
    List feirP = sc.getParams();
    List sirP = new LinkedList();
    for (i = 0; i < feirP.size(); i++) {
      sirP.add(((Expression) feirP.get(i)).accept(this));
    }
    SIRStream str = (SIRStream) classTable.get(sc.getName());
    if (parent instanceof SIRContainer) {
      ((SIRContainer) parent).add(str);
    }
    return new SIRInitStatement(sirP, str);
  }

  public Object visitSJDuplicate(SJDuplicate sj) {
    debug("In visitSJDuplicate\n");
    return SIRSplitter.create((SIRContainer) parent, SIRSplitType.DUPLICATE, 2);
  }

  public Object visitSJRoundRobin(SJRoundRobin sj) {
    debug("In visitSJRoundRobin\n");
    return SIRSplitter.createUniformRR((SIRContainer) parent, (JExpression) sj.getWeight().accept(this));
  }

  public Object visitSJWeightedRR(SJWeightedRR sj) { 
    debug("In visitSJWeightedRR\n");
    List weights = sj.getWeights();
    JExpression[] newWeights = new JExpression[weights.size()];
    int i;

    for (i = 0; i < weights.size(); i++) {
      newWeights[i] = (JExpression) ((Expression) weights.get(i)).accept(this);
    }

    return SIRSplitter.createWeightedRR((SIRContainer) parent, newWeights);
  }

  public Object visitStmtAdd(StmtAdd stmt) {
    debug("In visitStmtAdd\n");
    return stmt.getCreator().accept(this);
  }

  public Object visitStmtAssign(StmtAssign stmt) {
    debug("In visitStmtAssign\n");
    if (stmt.getOp() == 0) {
      return new JAssignmentExpression(null,
				       (JExpression) stmt.getLHS().accept(this),
				       (JExpression) stmt.getRHS().accept(this));
    } else {
      int[] translation = { 0, // no entry
			    Constants.OPE_PLUS, // BINOP_ADD
			    Constants.OPE_MINUS, // BINOP_SUB
			    Constants.OPE_STAR, // BINOP_MUL
			    Constants.OPE_SLASH, // BINOP_DIV
			    Constants.OPE_PERCENT, // BINOP_MOD
			    0, // BINOP_AND can't happen
			    0, // BINOP_OR
			    Constants.OPE_EQ, // BINOP_EQ
			    Constants.OPE_NE, // BINOP_NEQ
			    Constants.OPE_LT, // BINOP_LT
			    Constants.OPE_LE, // BINOP_LE
			    Constants.OPE_GT, // BINOP_GT
			    Constants.OPE_GE, // BINOP_GE
			    Constants.OPE_BAND, // BINOP_BAND
			    Constants.OPE_BOR, // BINOP_BOR
			    Constants.OPE_BXOR }; // BINOP_BXOR
      return new JCompoundAssignmentExpression(null,
					       translation[stmt.getOp()],
					       (JExpression) stmt.getLHS().accept(this),
					       (JExpression) stmt.getRHS().accept(this));
    }
  }

  public Object visitStmtBlock(StmtBlock stmt) {
    debug("In visitStmtBlock\n");
    JBlock result = new JBlock();
    List stmts = stmt.getStmts();
    Iterator i;
    for (i = stmts.iterator(); i.hasNext(); ) {
      Statement s = (Statement) i.next();
      Object x = s.accept(this);
      if (x instanceof JStatement) {
	result.addStatement((JStatement) x);
      } else if (x instanceof JExpression) {
	result.addStatement(new JExpressionStatement(null, (JExpression) x, null));
      } else {
	debug("  Ignoring NULL\n");
      }
    }
    return result;
  }

  public Object visitStmtBody(StmtBody stmt) {
    debug("In visitStmtBody\n");
    debug("  UNIMPLEMENTED\n");
    /* unimplemented */
    return null;
  }

  public Object visitStmtBreak(StmtBreak stmt) {
    debug("In visitStmtBreak\n");
    return new JBreakStatement(null, null, null);
  }

  public Object visitStmtContinue(StmtContinue stmt) {
    debug("In visitStmtContinue\n");
    return new JContinueStatement(null, null, null);
  }

  public Object visitStmtDoWhile(StmtDoWhile stmt) {
    debug("In visitStmtDoWhile\n");
    return new JDoStatement(null,
			    (JExpression) stmt.getCond().accept(this),
			    (JStatement) stmt.getBody().accept(this),
			    null);
  }

  public Object visitStmtEnqueue(StmtEnqueue stmt) {
    debug("In visitStmtEnqueue\n");
    debug("  UNIMPLEMENTED\n");
    /* unimplemented */
    return null;
  }

  public Object visitStmtExpr(StmtExpr stmt) {
    debug("In visitStmtExpr\n");
    Object x = stmt.getExpression().accept(this);
    if (x instanceof JStatement) {
      return x;
    } else if (x instanceof JExpression) {
      return new JExpressionStatement(null, (JExpression) stmt.getExpression().accept(this), null);
    } else {
      /* BUG: Handle this...  but we probably don't need to */
      debug("  Invalid type: " + x + "\n");
      return null;
    }
  }

  public Object visitStmtFor(StmtFor stmt) {
    debug("In visitStmtFor\n");
    JStatement init = null, incr = null, body = null;
    JExpression cond = null;
    if (stmt.getInit() != null) {
      init = (JStatement) stmt.getInit().accept(this);
    }
    if (stmt.getCond() != null) {
      cond = (JExpression) stmt.getCond().accept(this);
    }
    if (stmt.getIncr() != null) {
      incr = (JStatement) stmt.getIncr().accept(this);
    }
    if (stmt.getBody() != null) {
      body = (JStatement) stmt.getBody().accept(this);
    }
    return new JForStatement(null, init, cond, incr, body, null);
  }

  public Object visitStmtIfThen(StmtIfThen stmt) {
    debug("In visitStmtIfThen\n");
    JStatement cons = null;
    if (stmt.getCons() != null) {
      cons = (JStatement) stmt.getCons().accept(this);
    }
    JStatement alt = null;
    if (stmt.getAlt() != null) {
      alt = (JStatement) stmt.getAlt().accept(this);
    }
    return new JIfStatement(null,
			    (JExpression) stmt.getCond().accept(this),
			    cons,
			    alt,
			    null);
  }

  public Object visitStmtJoin(StmtJoin stmt) {
    debug("In visitStmtJoin\n");
    if (parent instanceof SIRFeedbackLoop) {
      ((SIRFeedbackLoop) parent).setJoiner((SIRJoiner) stmt.getJoiner().accept(this));
    } else if (parent instanceof SIRSplitJoin) {
      ((SIRSplitJoin) parent).setJoiner((SIRJoiner) stmt.getJoiner().accept(this));
    }
    return null;
  }

  public Object visitStmtLoop(StmtLoop stmt) {
    debug("In visitStmtLoop\n");
    if (parent instanceof SIRFeedbackLoop) {
      ((SIRFeedbackLoop) parent).setLoop((SIRStream) stmt.getCreator().accept(this));
    }
    return null;
  }

  public Object visitStmtPhase(StmtPhase stmt) {
    debug("In visitStmtPhase\n");
    /* unimplemented */
    return null;
  }

  public Object visitStmtPush(StmtPush stmt) {
    return new SIRPushExpression((JExpression) stmt.getValue().accept(this),
				 parent.getOutputType());
  }

  public Object visitStmtReturn(StmtReturn stmt) {
    debug("In visitStmtReturn\n");
    if (stmt.getValue() == null) {
      return new JReturnStatement(null, null, null);
    } else {
      return new JReturnStatement(null, (JExpression) stmt.getValue().accept(this), null);
    }
  }

    public Object visitStmtSendMessage(StmtSendMessage stmt) 
    {
        debug("In visitStmtSendMessage\n");
        /* unimplemented */
        return null;
    }

  public Object visitStmtSplit(StmtSplit stmt) {
    debug("In visitStmtSplit\n");
    return stmt.getSplitter().accept(this);
  }

  public Object visitStmtVarDecl(StmtVarDecl stmt) {
    debug("In visitStmtVarDecl\n");
    List defs = new ArrayList();
    for (int i = 0; i < stmt.getNumVars(); i++)
    {
        Expression init = stmt.getInit(i);
        JExpression jinit = null;
        if (init != null)
            jinit = (JExpression)init.accept(this);
        defs.add(new JVariableDefinition(null,
                                         at.dms.kjc.Constants.ACC_PUBLIC,
                                         feirTypeToSirType(stmt.getType(i)),
                                         stmt.getName(i),
                                         jinit));
    }
    return new JVariableDeclarationStatement
        (null, // token reference
         (JVariableDefinition[])defs.toArray(new JVariableDefinition[1]),
         null); // comments
  }

  public Object visitStmtWhile(StmtWhile stmt) {
    debug("In visitStmtWhile\n");
    return new JWhileStatement(null,
			       (JExpression) stmt.getCond().accept(this),
			       (JStatement) stmt.getBody().accept(this),
			       null);
  }

  public Object visitStreamType(StreamType type) {
    debug("In visitStmtType\n");
    debug("  UNIMPLEMENTED\n");
    /* unimplemented */
    return null;
  }

  public Object visitStmtIODecl(StmtIODecl io) {
    debug("In visitStmtIODecl\n");
    debug("  UNIMPLEMENTED\n");
    /* Ignore these */
    return null;
  }

  public Object visitOther(FENode node) {
    debug("In visitStmtOther\n");
    debug("  UNIMPLEMENTED\n");
    /* unimplemented */
    if (node instanceof StmtIODecl) {
      return visitStmtIODecl((StmtIODecl) node);
    }
    debug("  Got: " + node + "\n");
    return null;
  }
}
