/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend;

import java.util.*;
import streamit.frontend.nodes.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;

public class FEIRToSIR implements FEVisitor, Constants {
  private SIRStream topLevel;
  private SIRStream parent;
  private Hashtable classTable;
    private Map cclassTable;
    private SymbolTable symtab;
    private boolean wantSplitter;
    private Program theProgram;
    private Set searchList;

  public FEIRToSIR() {
    classTable = new Hashtable();
    cclassTable = new HashMap();
    searchList = new HashSet();
  }
  
  public void debug(String s) {
      // System.err.print(s);
  }
  
    private Type getType(Expression expr)
    {
        // To think about: should we cache GetExprType objects?
        return (Type)expr.accept(new GetExprType(symtab, null)); // streamType
    }
    private TokenReference contextToReference(FEContext ctx)
    {
        if (ctx != null)
            return new TokenReference(ctx.getFileName(), ctx.getLineNumber());
        return TokenReference.NO_REF;
    }
    
    private TokenReference contextToReference(FENode node)
    {
        return contextToReference(node.getContext());
    }

  public Object visitProgram(Program p) {
    /* We visit each stream and each struct */
    List feirStreams;
    List feirStructs;
    List sirStreams;
    List sirStructs;
    Iterator iter;

    theProgram = p;
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
        StreamSpec spec = (StreamSpec)iter.next();
        if (!(classTable.containsKey(spec.getName())))
            sirStreams.add(spec.accept(this));
    }

    return topLevel;
  }

    public SIRStream findStream(SCSimple creator)
    {
        // Built-in streams need to be checked for with their type.
        String name = creator.getName();
        if (name.equals("Identity"))
        {
            Type ftype = (Type)creator.getTypes().get(0);
            return new SIRIdentity(feirTypeToSirType(ftype));
        }
        return findStream(name);
    }
    

    public SIRStream findStream(String name)
    {
        // Have we already seen it?
        if (classTable.containsKey(name))
            return (SIRStream)AutoCloner.deepCopy((SIRStream)classTable.get(name));
        // Are we looking for it?
        if (searchList.contains(name))
            return new SIRRecursiveStub(name, this);
        // Hmm.  Is it in the top-level Program?
        for (Iterator iter = theProgram.getStreams().iterator();
             iter.hasNext(); )
        {
            StreamSpec spec = (StreamSpec)iter.next();
            if (spec.getName().equals(name))
            {
                searchList.add(name);
                SIRStream oldParent = parent;
                parent = null;
                SIRStream result = (SIRStream)spec.accept(this);
                // also adds to classTable
                parent = oldParent;
                searchList.remove(name);
                return result;
            }
        }
        // Eit.  We lose.
        return null;
    }

  public Object visitStreamSpec(StreamSpec spec) {
    SIRStream oldParent = parent;
    SIRStream current = null;
    SymbolTable oldSymTab = symtab;
    symtab = new SymbolTable(symtab);
    for (Iterator iter = spec.getParams().iterator(); iter.hasNext(); )
    {
        Parameter param = (Parameter)iter.next();
        symtab.registerVar(param.getName(), param.getType(), param,
                           SymbolTable.KIND_STREAM_PARAM);
    }

    debug("In visitStreamSpec\n");
    
    switch (spec.getType())
    {
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

    if (spec.getName() != null)
    {      
        /* Not an anonymous stream */
        debug("Adding " + spec.getName() + " to symbol table\n");
        classTable.put(spec.getName(), current);

        // Is it the top-level stream? (being void->void)
        StreamType st = spec.getStreamType();
        if (st != null &&
            st.getIn() instanceof TypePrimitive &&
            ((TypePrimitive) st.getIn()).getType() ==
            TypePrimitive.TYPE_VOID &&
            st.getOut() instanceof TypePrimitive &&
            ((TypePrimitive) st.getOut()).getType() ==
            TypePrimitive.TYPE_VOID)
        {
            topLevel = current;
        }
    }

    symtab = oldSymTab;
    
    if (current != null) {
      return current;
    }

    debug("  This should never happen!\n");
    return null;
  }

    public List fieldDeclToJFieldDeclarations(FieldDecl decl)
    {
        debug("In fieldDeclToJFieldDeclarations\n");
        List result = new ArrayList();
        TokenReference ref = contextToReference(decl);
        for (int i = 0; i < decl.getNumFields(); i++)
        {
            JVariableDefinition def = null;
            JExpression init = null;
            if (decl.getInit(i) != null)
                init = (JExpression)decl.getInit(i).accept(this);
            else if (decl.getType(i) instanceof TypeArray)
            {
                TypeArray ta = (TypeArray)decl.getType(i);
                JExpression[] dims = new JExpression[1];
                dims[0] = (JExpression)ta.getLength().accept(this);
                init = new JNewArrayExpression(ref,
                                               feirTypeToSirType(ta.getBase()),
                                               dims, null);
            }
            def = new JVariableDefinition(ref,
                                          at.dms.kjc.Constants.ACC_PUBLIC,
                                          feirTypeToSirType(decl.getType(i)),
                                          decl.getName(i), init);
            JFieldDeclaration fDecl = new JFieldDeclaration(ref, def,
                                                            null, // javadoc
                                                            null); // comments
            result.add(fDecl);
        }
        return result;
    }

    private void setStreamFields(SIRStream stream, Hashtable cfields,
                                 List vars)
    {
        JFieldDeclaration[] fields = new JFieldDeclaration[0];
        List fieldList = new ArrayList();
        for (Iterator iter = vars.iterator(); iter.hasNext(); )
        {
            FieldDecl decl = (FieldDecl)iter.next();
            fieldList.addAll(fieldDeclToJFieldDeclarations(decl));
            for (int i = 0; i < decl.getNumFields(); i++)
            {
                symtab.registerVar(decl.getName(i), decl.getType(i),
                                   decl, SymbolTable.KIND_FIELD);
                // TODO: this is a mapping of name to CField,
                // so create a CField here.
                cfields.put(decl.getName(i),
                            feirTypeToSirType(decl.getType(i)));
            }
        }
        fields = (JFieldDeclaration[])fieldList.toArray(fields);
        stream.setFields(fields);
    }

    private CMethod jMethodToCMethod(JMethodDeclaration decl, CClass owner)
    {
        JFormalParameter[] jparams = decl.getParameters();
        // cparams = map (lambda p: p.getType()) jparams;
        CType[] cparams = new CType[jparams.length];
        for (int i = 0; i < jparams.length; i++)
            cparams[i] = jparams[i].getType();
        return new CSourceMethod(owner,
                                 ACC_PUBLIC, // modifiers
                                 decl.getName(),
                                 decl.getReturnType(),
                                 cparams, // paramTypes,
                                 CClassType.EMPTY, // exceptions
                                 false, // deprecated
                                 decl.getBody());
    }

    private CSourceClass specToCClass(StreamSpec spec)
    {
        CClass owner = null;
        String name = spec.getName();
        if (name == null)
        {
            // Anonymous stream.  The name can't actually be null, but
            // "" is acceptable.  We also know the owner, it's our
            // parent's cclass.
            name = "";
            owner = (CClass)cclassTable.get(parent.getIdent());
        }
        CSourceClass cclass = new CSourceClass(owner, // owner
                                               contextToReference(spec),
                                               0, // modifiers
                                               name, name, // qualified
                                               false); // deprecated
        return cclass;
    }

  public SIRStream visitFilterSpec(StreamSpec spec) {
    int i;
    List list;
    SIRFilter result = new SIRFilter();
    SIRStream oldParent = parent;
    CSourceClass cclass = specToCClass(spec);
    Hashtable fields = new Hashtable();
    parent = result;
    
    debug("In visitFilterSpec\n");

    result.setIdent(spec.getName());
    result.setInputType(feirTypeToSirType(spec.getStreamType().getIn()));
    result.setOutputType(feirTypeToSirType(spec.getStreamType().getOut()));

    setStreamFields(result, fields, spec.getVars());

    List meths = new ArrayList();
    List cmeths = new ArrayList();

    for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
    {
        Function func = (Function)iter.next();
        JMethodDeclaration jdecl = (JMethodDeclaration)visitFunction(func);
        if (jdecl == null)
            continue;
        meths.add(jdecl);
        cmeths.add(jMethodToCMethod(jdecl, cclass));
        // To consider: phase functions.  --dzm
        if (func.getCls() == Function.FUNC_WORK) {
            result.setWork(jdecl);
            FuncWork fw = (FuncWork)func;
            if (fw.getPopRate() != null) {
                result.setPop((JExpression) fw.getPopRate().accept(this));
            } else {
                result.setPop(0);
            }
            if (fw.getPeekRate() != null) {
                result.setPeek((JExpression) fw.getPeekRate().accept(this));
            } else {
                result.setPeek(result.getPop());
            }
            if (fw.getPushRate() != null) {
                result.setPush((JExpression) fw.getPushRate().accept(this));
            } else {
                result.setPush(0);
            }
        }
        if (func.getCls() == Function.FUNC_INIT) {
            result.setInit(jdecl);
        }
    }
    JMethodDeclaration[] methods =
        (JMethodDeclaration[])meths.toArray(new JMethodDeclaration[0]);
    result.setMethods(methods);

    CMethod[] cmethods = (CMethod[])cmeths.toArray(new CMethod[0]);
    cclass.close(CClassType.EMPTY, // interfaces
                 null, // superclass
                 fields,
                 cmethods);
    cclassTable.put(spec.getName(), cclass);

    parent = oldParent;

    return result;
  }

  public SIRStream visitPipelineSpec(StreamSpec spec) {
    int i;
    List list;
    SIRPipeline result = new SIRPipeline(spec.getName());
    SIRStream oldParent = parent;
    CSourceClass cclass = specToCClass(spec);
    Hashtable fields = new Hashtable();

    debug("In visitPipelineSpec\n");

    parent = result;
    
    setStreamFields(result, fields, spec.getVars());

    List meths = new ArrayList();
    List cmeths = new ArrayList();

    for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
    {
        Function func = (Function)iter.next();
        JMethodDeclaration jdecl = (JMethodDeclaration)visitFunction(func);
        if (jdecl == null)
            continue;
        meths.add(jdecl);
        cmeths.add(jMethodToCMethod(jdecl, cclass));
        if (func.getCls() == Function.FUNC_INIT) {
            result.setInit(jdecl);
      }
    }
    JMethodDeclaration[] methods =
        (JMethodDeclaration[])meths.toArray(new JMethodDeclaration[0]);
    result.setMethods(methods);

    CMethod[] cmethods = (CMethod[])cmeths.toArray(new CMethod[0]);
    cclass.close(CClassType.EMPTY, // interfaces
                 null, // superclass
                 fields,
                 cmethods);
    cclassTable.put(spec.getName(), cclass);

    parent = oldParent;

    return result;
  }

  public CType feirTypeToSirType(Type type) {
    debug("In feirTypeToSirType\n");
    if (type instanceof TypePrimitive) {
      TypePrimitive tp = (TypePrimitive) type;
      switch (tp.getType()) {
      case TypePrimitive.TYPE_BOOLEAN:
        return CStdType.Boolean;
      case TypePrimitive.TYPE_BIT:
      case TypePrimitive.TYPE_INT:
	return CStdType.Integer;
      case TypePrimitive.TYPE_FLOAT:
	return CStdType.Float;
      case TypePrimitive.TYPE_DOUBLE:
	return CStdType.Double;
      case TypePrimitive.TYPE_COMPLEX:
        return new CClassType((CClass)cclassTable.get("Complex"));
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
        return new CClassType((CClass)cclassTable.get(((TypeStruct)type).getName()));
    } else if (type instanceof TypeStructRef) {
        return new CClassType((CClass)cclassTable.get(((TypeStructRef)type).getName()));
    }
    /* This shouldn't happen */
    debug("  UNIMPLEMENTED - shouldn't happen\n");
    System.err.println("FEIRToSIR: failed to convert type " + type);
    return null;
  }

  public SIRStructure visitTypeStruct(TypeStruct ts) {
    JFieldDeclaration[] fields = new JFieldDeclaration[ts.getNumFields()];
    int i;
    TokenReference tr = contextToReference(ts.getContext());
    CSourceClass cc = new CSourceClass(null, //owner
                                       tr, //where
                                       0, //modifiers
                                       ts.getName(),
                                       ts.getName(),
                                       false); // deprecated
    Hashtable cfields = new Hashtable();
    CMethod[] cmethods = new CMethod[0];
    for (i = 0; i < fields.length; i++) {
      String name = ts.getField(i);
      Type type = ts.getType(name);
      CType ctype = feirTypeToSirType(type);
      JVariableDefinition def = new JVariableDefinition(tr,
                                                        ACC_PUBLIC,
                                                        ctype,
							name,
							null);
      fields[i] = new JFieldDeclaration(tr,
					def,
					null,
					null);
      CSourceField cf = new CSourceField(cc, ACC_PUBLIC, name, ctype, false);
      cfields.put(name, cf);
    }
    debug("Adding " + ts.getName() + " to symbol table\n");
    SIRStructure struct = new SIRStructure(null,
					   ts.getName(),
					   fields);
    classTable.put(ts.getName(), struct);
    cc.close(CClassType.EMPTY, null, cfields, cmethods);
    cclassTable.put(ts.getName(), cc);
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
    Type lhsType = getType(exp.getLeft());
    // ASSERT: this is a structure type.
    TypeStruct ts = (TypeStruct)lhsType;
    CClass ccs = (CClass)cclassTable.get(ts.getName());
    CField cf = ccs.getField(exp.getName());
    return new JFieldAccessExpression(null,
				      (JExpression) exp.getLeft().accept(this),
				      exp.getName(), cf);
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
    int kind = symtab.lookupKind(exp.getName());
    if (kind == SymbolTable.KIND_LOCAL || kind == SymbolTable.KIND_FUNC_PARAM)
    {
        JLocalVariable var =
            (JLocalVariable)symtab.lookupOrigin(exp.getName());
        return new JLocalVariableExpression(contextToReference(exp), var);
    }
    // TODO: attach a CField, assuming that what we have is in fact
    // a field.
    return new JFieldAccessExpression(null,
				      new JThisExpression(null, (CClass) null),
				      exp.getName());
  }

  public Object visitFunction(Function func) {
    debug("In visitFunction\n");
    int i = 0;
    List list;
    SymbolTable oldSymTab = symtab;
    symtab = new SymbolTable(symtab);
    Parameter[] feirParams = new Parameter[func.getParams().size()];
    JFormalParameter[] sirParams = new JFormalParameter[feirParams.length];
    for (Iterator iter = func.getParams().iterator(); iter.hasNext(); )
    {
        Parameter param = (Parameter)iter.next();
        sirParams[i] = (JFormalParameter)visitParameter(param);
        symtab.registerVar(param.getName(), param.getType(), sirParams[i],
                           SymbolTable.KIND_FUNC_PARAM);
        i++;
    }
    JMethodDeclaration result;
    // Slightly different object for constructor vs. otherwise.
    if (parent.getIdent() != null &&
        parent.getIdent().equals(func.getName()))
    {
        // In a constructor.  Ignore.  (Kopi2SIR does some processing
        // but fails to add to the parent stream.)
        result = null;
    }
    else
        result =
            new JMethodDeclaration(contextToReference(func),
                                   ACC_PUBLIC,
                                   feirTypeToSirType(func.getReturnType()),
                                   func.getName(),
                                   sirParams,
                                   CClassType.EMPTY,
                                   statementToJBlock(func.getBody()),
                                   null, /* javadoc */
                                   null); /* comments */
    symtab = oldSymTab;
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
    CSourceClass cclass = specToCClass(spec);
    Hashtable fields = new Hashtable();

    debug("In visitSplitJoinSpec\n");

    parent = result;
    
    setStreamFields(result, fields, spec.getVars());
    
    List meths = new ArrayList();
    List cmeths = new ArrayList();

    for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
    {
        Function func = (Function)iter.next();
        JMethodDeclaration jdecl = (JMethodDeclaration)visitFunction(func);
        if (jdecl == null)
            continue;
        meths.add(jdecl);
        cmeths.add(jMethodToCMethod(jdecl, cclass));
        if (func.getCls() == Function.FUNC_INIT) {
            result.setInit(jdecl);
      }
    }
    JMethodDeclaration[] methods =
        (JMethodDeclaration[])meths.toArray(new JMethodDeclaration[0]);
    result.setMethods(methods);

    CMethod[] cmethods = (CMethod[])cmeths.toArray(new CMethod[0]);
    cclass.close(CClassType.EMPTY, // interfaces
                 null, // superclass
                 fields,
                 cmethods);
    if (spec.getName() != null)
        cclassTable.put(spec.getName(), cclass);

    parent = oldParent;

    return result;
  }

  public Object visitFeedbackLoopSpec(StreamSpec spec) {
    int i;
    List list;
    SIRFeedbackLoop result = new SIRFeedbackLoop((SIRContainer) parent, spec.getName());
    SIRStream oldParent = parent;
    CSourceClass cclass = specToCClass(spec);
    Hashtable fields = new Hashtable();

    debug("In visitFeedbackLoopSpec\n");

    parent = result;

    setStreamFields(result, fields, spec.getVars());
    
    List meths = new ArrayList();
    List cmeths = new ArrayList();

    for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
    {
        Function func = (Function)iter.next();
        JMethodDeclaration jdecl = (JMethodDeclaration)visitFunction(func);
        if (jdecl == null)
            continue;
        meths.add(jdecl);
        cmeths.add(jMethodToCMethod(jdecl, cclass));
        if (func.getCls() == Function.FUNC_INIT) {
            result.setInit(jdecl);
      }
    }
    JMethodDeclaration[] methods =
        (JMethodDeclaration[])meths.toArray(new JMethodDeclaration[0]);
    result.setMethods(methods);

    CMethod[] cmethods = (CMethod[])cmeths.toArray(new CMethod[0]);
    cclass.close(CClassType.EMPTY, // interfaces
                 null, // superclass
                 fields,
                 cmethods);
    cclassTable.put(spec.getName(), cclass);

    parent = oldParent;

    return result;
  }

  public Object visitPhasedFilterSpec(StreamSpec spec) {
    int i;
    List list;
    SIRPhasedFilter result = new SIRPhasedFilter(spec.getName());
    SIRStream oldParent = parent;
    CClass owner = null;
    CSourceClass cclass = new CSourceClass(owner, // owner
                                           contextToReference(spec),
                                           0, // modifiers
                                           spec.getName(),
                                           spec.getName(), // qualified
                                           false); // deprecated
    Hashtable fields = new Hashtable();

    debug("In visitPhasedFilterSpec\n");

    parent = result;
    
    setStreamFields(result, fields, spec.getVars());
    
    List meths = new ArrayList();
    List cmeths = new ArrayList();

    for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
    {
        Function func = (Function)iter.next();
        JMethodDeclaration jdecl = (JMethodDeclaration)visitFunction(func);
        if (jdecl == null)
            continue;
        meths.add(jdecl);
        cmeths.add(jMethodToCMethod(jdecl, cclass));
        if (func.getCls() == Function.FUNC_INIT) {
            result.setInit(jdecl);
      }
    }
    JMethodDeclaration[] methods =
        (JMethodDeclaration[])meths.toArray(new JMethodDeclaration[0]);
    result.setMethods(methods);

    CMethod[] cmethods = (CMethod[])cmeths.toArray(new CMethod[0]);
    cclass.close(CClassType.EMPTY, // interfaces
                 null, // superclass
                 fields,
                 cmethods);
    cclassTable.put(spec.getName(), cclass);

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
    /* Translate the arguments */
    int i;
    List feirP = sc.getParams();
    List sirP = new LinkedList();
    for (i = 0; i < feirP.size(); i++) {
      sirP.add(((Expression) feirP.get(i)).accept(this));
    }
    SIRStream str = findStream(sc);
    if (str == null)
        System.err.println("!!! didn't find stream named " + sc.getName());
    if (parent instanceof SIRContainer) {
      ((SIRContainer) parent).add(str);
    }
    return new SIRInitStatement(sirP, str);
  }

  public Object visitSJDuplicate(SJDuplicate sj) {
    debug("In visitSJDuplicate\n");
    if (wantSplitter)
        return SIRSplitter.create((SIRContainer) parent,
                                  SIRSplitType.DUPLICATE, 2);
    else
        // Not allowed.
        return null;
  }

  public Object visitSJRoundRobin(SJRoundRobin sj) {
    debug("In visitSJRoundRobin\n");
    Expression weight = sj.getWeight();
    JExpression jweight;
    // weight can be null; if so, it's really 1.
    if (weight != null)
        jweight = (JExpression)weight.accept(this);
    else
        jweight = new JIntLiteral(contextToReference(sj), 1);
    if (wantSplitter)
        return SIRSplitter.createUniformRR((SIRContainer) parent, jweight);
    else
        return SIRJoiner.createUniformRR((SIRContainer)parent, jweight);
  }

  public Object visitSJWeightedRR(SJWeightedRR sj) { 
    debug("In visitSJWeightedRR\n");
    List weights = sj.getWeights();
    JExpression[] newWeights = new JExpression[weights.size()];
    int i;

    for (i = 0; i < weights.size(); i++) {
      newWeights[i] = (JExpression) ((Expression) weights.get(i)).accept(this);
    }

    if (wantSplitter)
        return SIRSplitter.createWeightedRR((SIRContainer) parent, newWeights);
    else
        return SIRJoiner.createWeightedRR((SIRContainer)parent, newWeights);
  }

  public Object visitStmtAdd(StmtAdd stmt) {
    debug("In visitStmtAdd\n");
    return stmt.getCreator().accept(this);
  }

  public Object visitStmtAssign(StmtAssign stmt) {
    debug("In visitStmtAssign\n");
    JExpression expr;
    if (stmt.getOp() == 0) {
      expr = new JAssignmentExpression(null,
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
      expr = new JCompoundAssignmentExpression(null,
					       translation[stmt.getOp()],
					       (JExpression) stmt.getLHS().accept(this),
					       (JExpression) stmt.getRHS().accept(this));
    }
    return new JExpressionStatement(contextToReference(stmt), expr, null);
  }

  public Object visitStmtBlock(StmtBlock stmt) {
    debug("In visitStmtBlock\n");
    SymbolTable oldSymTab = symtab;
    symtab = new SymbolTable(symtab);
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
          debug("  Didn't produce a JStatement or JExpression.\n");
          debug("  Was: " + s + "\n");
          debug("  Is: " + x + "\n");
      }
    }
    symtab = oldSymTab;
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
    wantSplitter = false;
    SIRJoiner joiner = (SIRJoiner)stmt.getJoiner().accept(this);
    if (parent instanceof SIRFeedbackLoop) {
        ((SIRFeedbackLoop) parent).setJoiner(joiner);
    } else if (parent instanceof SIRSplitJoin) {
        ((SIRSplitJoin) parent).setJoiner(joiner);
    }
    return joiner;
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
    wantSplitter = true;
    SIRSplitter splitter = (SIRSplitter)stmt.getSplitter().accept(this);
    if (parent instanceof SIRFeedbackLoop) {
        ((SIRFeedbackLoop) parent).setSplitter(splitter);
    } else if (parent instanceof SIRSplitJoin) {
        ((SIRSplitJoin) parent).setSplitter(splitter);
    }
    return splitter;
  }

  public Object visitStmtVarDecl(StmtVarDecl stmt) {
    debug("In visitStmtVarDecl\n");
    List defs = new ArrayList();
    for (int i = 0; i < stmt.getNumVars(); i++)
    {
        symtab.registerVar(stmt.getName(i), stmt.getType(i), stmt,
                           SymbolTable.KIND_LOCAL);
        Expression init = stmt.getInit(i);
        JExpression jinit = null;
        if (init != null)
            jinit = (JExpression)init.accept(this);
        JVariableDefinition def =
            new JVariableDefinition(null,
                                    at.dms.kjc.Constants.ACC_PUBLIC,
                                    feirTypeToSirType(stmt.getType(i)),
                                    stmt.getName(i),
                                    jinit);
        defs.add(def);
        // Repoint the "origin" in the symbol table at the variable
        // definition.
        symtab.registerVar(stmt.getName(i), stmt.getType(i), def,
                           SymbolTable.KIND_LOCAL);
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

  public Object visitOther(FENode node) {
    debug("In visitStmtOther\n");
    debug("  UNIMPLEMENTED\n");
    /* unimplemented */
    debug("  Got: " + node + "\n");
    return null;
  }
}
