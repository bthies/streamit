package at.dms.kjc.sir;

import java.io.*;
import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.compiler.*;

/**
 * Dump an SIR tree into a StreamIt program.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SIRToStreamIt.java,v 1.6 2004-02-20 14:39:18 dmaze Exp $
 */
public class SIRToStreamIt
    extends at.dms.util.Utils
    implements Constants, SLIRVisitor, AttributeStreamVisitor
{
    /*
     * Test code: top-level entry point.
     */
    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[] structs)
    {
        System.out.println("*/");
        
        // Rename top-level object.
        str.setIdent(str.getIdent() + "_c");
        
        // Flattener code: run constant prop and such.
        ConstantProp.propagateAndUnroll(str);
        ConstructSIRTree.doit(str);
        FieldProp.doPropagate(str);
        new BlockFlattener().flattenBlocks(str);
        
        SIRToStreamIt s2s = new SIRToStreamIt(new TabbedPrintWriter(new PrintWriter(System.out)));

        for (int i = 0; i < structs.length; i++)
        {
            assert structs[i] != null;
            if (!(structs[i].getIdent().equals("Complex")))
                structs[i].accept(s2s);
        }
        for (int i = 0; i < interfaces.length; i++)
            interfaces[i].accept(s2s);
        s2s.visitAnyStream(str);
        s2s.close();
    }

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * construct a pretty printer object for java code
     */
    private SIRToStreamIt() {
        this(null);
        this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }

    /**
     * Generates code for <flatClass> and sends to System.out.
     */
    public static void generateCode(JClassDeclaration flat) {
	System.out.println("*/");	
        SIRToStreamIt s2s = new SIRToStreamIt(new TabbedPrintWriter(new PrintWriter(System.out)));

        // Print all of the portals.
        for (int i = 0; i < SIRPortal.getPortals().length; i++) {

            s2s.print("portal ");
            SIRPortal.getPortals()[i].accept(s2s);
            s2s.print(";");
            s2s.newLine();
        }

	flat.accept(s2s);
	s2s.close();
    }

    /**
     * construct a pretty printer object for java code
     * @param	fileName		the file into the code is generated
     */
    private SIRToStreamIt(TabbedPrintWriter p) {
        this.p = p;
        this.toplevel = false;
        this.str = null;
        this.pos = 0;
        this.portalCount = 0;
        this.portalNames = new java.util.HashMap();
        this.seenStreams = new java.util.HashSet();
        this.streamQueue = new java.util.LinkedList();
    }

    /**
     * Close the stream at the end
     */
    public void close() {
        p.close();
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getString() {
        if (str != null)
            return str.toString();
        else
            return null;
    }

    // ----------------------------------------------------------------------
    // VISIT STREAMS
    // ----------------------------------------------------------------------

    private Set seenStreams;
    private List streamQueue;

    public void visitAnyStream(SIROperator op)
    {
        streamQueue.add(op);
        while (!streamQueue.isEmpty())
        {
            SIROperator o = (SIROperator)streamQueue.get(0);
            streamQueue.remove(0);
            // Do nothing if we've seen it; print it otherwise
            if (!seenStreams.contains(o))
            {
                seenStreams.add(o);
                // Avoid builtins.
                if (!(o instanceof SIRPredefinedFilter))
                {
                    theStream = o;
                    o.accept(this);
                    newLine();
                }
            }
        }
    }

    private void printHeader(SIRStream self, String type)
    {
        CType inType = self.getInputType();
        CType outType = self.getOutputType();
        // Consider special-case inputs and outputs for feedback loops.
        if (self instanceof SIRFeedbackLoop)
        {
            SIRFeedbackLoop fl = (SIRFeedbackLoop)self;

            SIRJoiner joiner = fl.getJoiner();
            if (joiner.getType() == SIRJoinType.NULL ||
                (joiner.getType() == SIRJoinType.WEIGHTED_RR &&
                 joiner.getWeight(0) == 0))
                inType = CStdType.Void;

            SIRSplitter splitter = fl.getSplitter();
            if (splitter.getType() == SIRSplitType.NULL ||
                (splitter.getType() == SIRSplitType.WEIGHTED_RR &&
                 splitter.getWeight(0) == 0))
                outType = CStdType.Void;
        }
        if (inType != null && outType != null)
        {
            print(inType);
            print("->");
            print(outType);
            print(" ");
        }
        print(type);
        if (self.getIdent() != null)
        {
            // if the parent is null, this is the top-level stream;
            // there is only one of it, so print its [non-unique]
            // ident rather than its [unique] name so the class name
            // matches the filename
            if (self.getParent() == null)
                print(" " + self.getIdent());
            else
                print(" " + self.getName());
        }

        // In SIR, streams don't have parameter lists, but their
        // init functions do.  Print a parameter list.
        JMethodDeclaration init = self.getInit();
        JFormalParameter[] parameters = init.getParameters();
        if (parameters.length > 0)
        {
            print("(");
            boolean first = true;
            
            for (int i = 0; i < parameters.length; i++) {
                if (!first)
                    print(", ");
                first = false;
                parameters[i].accept(this);
            }
            print(")");
        }
        
        newLine();
    }

    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields)
    {
        print("struct " + self.getIdent());
        newLine();
        print("{");
        pos += TAB_SIZE;
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        pos -= TAB_SIZE;
        newLine();
        print("}");
        newLine();
        newLine();
        return null;
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
        printHeader(self, "filter");
        print("{");
        newLine();
        pos += TAB_SIZE;
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        if (init != null)
            init.accept(this);
        if (work != null)
            work.accept(this);
        pos -= TAB_SIZE;
        newLine();
        print("}");
        newLine();
        return null;
    }
  
    /* visit a phased filter */
    public Object visitPhasedFilter(SIRPhasedFilter self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration work,
                                    SIRWorkFunction[] initPhases,
                                    SIRWorkFunction[] phases,
                                    CType inputType, CType outputType)
    {
        assert false : "should implement phased filter support";
        return null;
    }
  
    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] weights)
    {
        if (type.isDuplicate())
            print("split duplicate;");
        else if (type.isNull())
            print("split roundrobin(0);");
        else if (type.isRoundRobin())
        {
            print("split roundrobin(");

            // Check (the hard way) for a uniform round-robin.
            boolean uniform = false;
            if (weights.length > 0)
            {
                uniform = true;
                int w = self.getWeight(0);
                for (int i = 1; i < weights.length; i++)
                    if (self.getWeight(i) != w)
                        uniform = false;
            }

            if (uniform)
                print(self.getWeight(0));
            else
            {
                boolean first = true;
                for (int i = 0; i < weights.length; i++)
                {
                    assert weights[i] != null;
                    if (!first)
                        print(", ");
                    first = false;
                    weights[i].accept(this);
                }
            }
            
            print(");");
        }
        else
            assert false : self;
        return null;
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] weights)
    {
        if (type.isNull())
            print("join roundrobin(0);");
        else if (type.isRoundRobin())
        {
            print("join roundrobin(");

            // Check (the hard way) for a uniform round-robin.
            boolean uniform = false;
            if (weights.length > 0)
            {
                uniform = true;
                int w = self.getWeight(0);
                for (int i = 1; i < weights.length; i++)
                    if (self.getWeight(i) != w)
                        uniform = false;
            }

            if (uniform)
                print(self.getWeight(0));
            else
            {
                boolean first = true;
                for (int i = 0; i < weights.length; i++)
                {
                    assert weights[i] != null;
                    if (!first)
                        print(", ");
                    first = false;
                    weights[i].accept(this);
                }
            }
            
            print(");");
        }
        else
            assert false : self;
        return null;
    }
    
    /* visit a work function */
    public Object visitWorkFunction(SIRWorkFunction self,
                                    JMethodDeclaration work)
    {
        print("work " + work.getName());
        return null;
    }

    /* pre-visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
                                JFieldDeclaration[] fields,
                                JMethodDeclaration[] methods,
                                JMethodDeclaration init)
    {
        printHeader(self, "pipeline");
        toplevel = true;
        init.getBody().accept(this);
        newLine();
        return null;
    }

    /* pre-visit a splitjoin */
    public Object visitSplitJoin(SIRSplitJoin self,
                                 JFieldDeclaration[] fields,
                                 JMethodDeclaration[] methods,
                                 JMethodDeclaration init,
                                 SIRSplitter splitter,
                                 SIRJoiner joiner)
    {
        printHeader(self, "splitjoin");
        toplevel = true;
        init.getBody().accept(this);
        newLine();
        return null;
    }

    /* pre-visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration initPath)
    {
        printHeader(self, "feedbackloop");
        toplevel = true;
        init.getBody().accept(this);
        newLine();
        return null;
    }

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * prints a compilation unit
     */
    public void visitCompilationUnit(JCompilationUnit self,
                                     JPackageName packageName,
                                     JPackageImport[] importedPackages,
                                     JClassImport[] importedClasses,
                                     JTypeDeclaration[] typeDeclarations) {
        if (packageName.getName().length() > 0) {
            packageName.accept(this);
            if (importedPackages.length + importedClasses.length > 0) {
                newLine();
            }
        }

        for (int i = 0; i < importedPackages.length ; i++) {
            if (!importedPackages[i].getName().equals("java/lang")) {
                importedPackages[i].accept(this);
                newLine();
            }
        }

        for (int i = 0; i < importedClasses.length ; i++) {
            importedClasses[i].accept(this);
            newLine();
        }

        for (int i = 0; i < typeDeclarations.length ; i++) {
            newLine();
            typeDeclarations[i].accept(this);
            newLine();
        }
    }

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * prints a class declaration
     */
    public void visitClassDeclaration(JClassDeclaration self,
                                      int modifiers,
                                      String ident,
                                      String superName,
                                      CClassType[] interfaces,
                                      JPhylum[] body,
                                      JFieldDeclaration[] fields,
                                      JMethodDeclaration[] methods,
                                      JTypeDeclaration[] decls) {
        SIRToStreamIt that = new SIRToStreamIt(this.p);
        that.className = ident;
        that.isStruct = ((modifiers & ACC_STATIC) == ACC_STATIC);
        that.visitClassBody(decls, fields, methods, body);
    }

    /**
     *
     */
    public void visitClassBody(JTypeDeclaration[] decls,
                               JFieldDeclaration[] fields,
                               JMethodDeclaration[] methods,
                               JPhylum[] body) {
        // Visit type declarations that happen to be for StreamIt
        // structure types first.
        for (int i = 0; i < decls.length ; i++) {
            if (decls[i] instanceof JClassDeclaration &&
                (decls[i].getModifiers() & ACC_STATIC) == ACC_STATIC)
                decls[i].accept(this);
        }        
        for (int i = 0; i < decls.length ; i++) {
            if (!(decls[i] instanceof JClassDeclaration &&
                  (decls[i].getModifiers() & ACC_STATIC) == ACC_STATIC))
            decls[i].accept(this);
        }
        if (body != null) {
            for (int i = 0; i < body.length ; i++) {
                if (!(body[i] instanceof JFieldDeclaration))
                    body[i].accept(this);
            }
        }

        newLine();
        print("typedef struct " + className + " {");
        pos += TAB_SIZE;
        if (body != null) {
            for (int i = 0; i < body.length ; i++) {
                if (body[i] instanceof JFieldDeclaration)
                    body[i].accept(this);
            }
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i].accept(this);
        }    

        pos -= TAB_SIZE;
        newLine();
        if (isStruct)
            print("} " + className + ";");
        else
            print("} _" + className + ", *" + className + ";");

        // Print function prototypes for each of the methods.
        declOnly = true;
        for (int i = 0; i < methods.length; i++) {
            methods[i].accept(this);
        }

        declOnly = false;
        for (int i = 0; i < methods.length ; i++) {
            methods[i].accept(this);
        }
    }

    /**
     * prints a class declaration
     */
    public void visitInnerClassDeclaration(JClassDeclaration self,
                                           int modifiers,
                                           String ident,
                                           String superName,
                                           CClassType[] interfaces,
                                           JTypeDeclaration[] decls,
                                           JPhylum[] body,
                                           JFieldDeclaration[] fields,
                                           JMethodDeclaration[] methods) {
        print(" {");
        pos += TAB_SIZE;
        for (int i = 0; i < decls.length ; i++) {
            decls[i].accept(this);
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i].accept(this);
        }
        for (int i = 0; i < methods.length ; i++) {
            methods[i].accept(this);
        }
        for (int i = 0; i < body.length ; i++) {
            body[i].accept(this);
        }
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints an interface declaration
     */
    public void visitInterfaceDeclaration(JInterfaceDeclaration self,
                                          int modifiers,
                                          String ident,
                                          CClassType[] interfaces,
                                          JPhylum[] body,
                                          JMethodDeclaration[] methods) {
        /* If an interface declaration gets through this far, it means
         * that there is a portal interface that can have messages sent
         * through it.  We can ignore everything about the interface
         * but the methods.  For each method, we need to generate a
         * parameter structure and a correct send_message() wrapper. */
        for (int i = 0; i < methods.length; i++) {
            CMethod mth = methods[i].getMethod();
            CType[] params = mth.getParameters();
            String name = ident + "_" + methods[i].getName();
            String pname = name + "_params";

            // Print the parameter structure.
            newLine();
            print("typedef struct " + pname + " {");
            pos += TAB_SIZE;
            for (int j = 0; j < params.length; j++) {
                newLine();
                print(params[j]);
                print(" p" + j + ";");
            }
            pos -= TAB_SIZE;
            newLine();
            print("} _" + pname + ", *" + pname + ";");

            // And now print a wrapper for send_message().
            newLine();
            print("void send_" + name + "(portal p, latency l");
            for (int j = 0; j < params.length; j++) {
                print(", ");
                print(params[j]);
                print(" p" + j);
            }
            print(") {");
            pos += TAB_SIZE;
            newLine();
            print(pname + " q = malloc(sizeof(_" + pname + "));");
            for (int j = 0; j < params.length; j++) {
                newLine();
                print("q->p" + j + " = p" + j + ";");
            }
            newLine();
            print("send_message(p, " + i + ", l, q);");
            pos -= TAB_SIZE;
            newLine();
            print("}");
        }        
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
        newLine();
        print (type);
        print (" ");
        print (ident);
        if (expr != null) {
            print (" = ");
            expr.accept (this);
        }
        print(";");
    }

    /**
     * prints a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) {
        newLine();
        // TODO: figure out what kind of function this is
        // (phase, handler, helper are special).

        // Treat init and work functions specially.
        if (ident.equals("init") || ident.equals("work"))
        {
            print(ident);
        }
        else
        {
            // print(CModifier.toString(modifiers));
            print(returnType);
            print(" ");
            print(ident);
            print("(");
            int count = 0;
            
            for (int i = 0; i < parameters.length; i++) {
                if (count != 0) {
                    print(", ");
                }
                
                // if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
                count++;
                // }
            }
            print(")");
        }

        // Print I/O rates, if they're available.
        if (ident.equals("work")) // or a phase function?
        {
            SIRFilter filter = (SIRFilter)theStream;
            if (filter.getPop() != null)
            {
                print(" pop ");
                filter.getPop().accept(this);
            }
            if (filter.getPeek() != null)
            {
                print(" peek ");
                filter.getPeek().accept(this);
            }
            if (filter.getPush() != null)
            {
                print(" push ");
                filter.getPush().accept(this);
            }
        }
        
        if (declOnly)
        {
            print(";");
            return;
        }

        newLine();
        if (body != null) {
            body.accept(this);
        } else {
            print(";");
        }
        newLine();
    }

    /**
     * prints a method declaration
     */
    public void visitConstructorDeclaration(JConstructorDeclaration self,
                                            int modifiers,
                                            String ident,
                                            JFormalParameter[] parameters,
                                            CClassType[] exceptions,
                                            JConstructorBlock body)
    {
        newLine();
        print(CModifier.toString(modifiers));
        print(ident);
        print("_");
        print(ident);
        print("(");
        int count = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                print(", ");
            }
            if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
                count++;
            }
        }
        print(")");
        /*
          for (int i = 0; i < exceptions.length; i++) {
          if (i != 0) {
          print(", ");
          } else {
          print(" throws ");
          }
          print(exceptions[i].toString());
          }
        */
        print(" ");
        body.accept(this);
        newLine();
    }

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
        print("while (");
        cond.accept(this);
        print(") ");

        body.accept(this);
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                  JVariableDefinition[] vars) {
        for (int i = 0; i < vars.length; i++) {
            vars[i].accept(this);
        }
    }

    private void printLocalArrayDecl(JNewArrayExpression expr) 
    {
	JExpression[] dims = expr.getDims();
	for (int i = 0 ; i < dims.length; i++) {
	    print("[");
	    SIRToStreamIt toC = new SIRToStreamIt(this.p);
	    dims[i].accept(toC);
	    print("]");
	}
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        // print(CModifier.toString(modifiers));
        // Uh, hmm, maybe we should do this conversion globally in
        // SIR.  But in this case we know the type a little better
        // than we do just from Java.
        if (expr != null && expr instanceof JNewArrayExpression)
        {
            assert type instanceof CArrayType;
            print(((CArrayType)type).getElementType());
            printLocalArrayDecl((JNewArrayExpression)expr);
        }
        else
            print(type);
        print(" ");
        print(ident);
        if (expr != null && !(expr instanceof JNewArrayExpression)) {
            print(" = ");
            expr.accept(this);
	}
        print(";");
    }

    /**
     * prints a try-catch statement
     */
    public void visitTryCatchStatement(JTryCatchStatement self,
                                       JBlock tryClause,
                                       JCatchClause[] catchClauses) {
        // Not legit StreamIt code!
        assert false;
        print("try ");
        tryClause.accept(this);
        for (int i = 0; i < catchClauses.length; i++) {
            catchClauses[i].accept(this);
        }
    }

    /**
     * prints a try-finally statement
     */
    public void visitTryFinallyStatement(JTryFinallyStatement self,
                                         JBlock tryClause,
                                         JBlock finallyClause) {
        // Not legit StreamIt code!
        assert false;
        print("try ");
        tryClause.accept(this);
        if (finallyClause != null) {
            print(" finally ");
            finallyClause.accept(this);
        }
    }

    /**
     * prints a throw statement
     */
    public void visitThrowStatement(JThrowStatement self,
                                    JExpression expr) {
        // Not legit StreamIt code!
        assert false;
        print("throw ");
        expr.accept(this);
        print(";");
    }

    /**
     * prints a synchronized statement
     */
    public void visitSynchronizedStatement(JSynchronizedStatement self,
                                           JExpression cond,
                                           JStatement body) {
        // Not legit StreamIt code!
        assert false;
        print("synchronized (");
        cond.accept(this);
        print(") ");
        body.accept(this);
    }

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        print("switch (");
        expr.accept(this);
        print(") {");
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        newLine();
        print("}");
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        print("return");
        if (expr != null) {
            print(" ");
            expr.accept(this);
        }
        print(";");
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        print(label + ":");
        stmt.accept(this);
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {
        print("if (");
        cond.accept(this);
        print(") ");
        pos += thenClause instanceof JBlock ? 0 : TAB_SIZE;
        thenClause.accept(this);
        pos -= thenClause instanceof JBlock ? 0 : TAB_SIZE;
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                print(" ");
            } else {
                newLine();
            }
            print("else ");
            pos += elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
            elseClause.accept(this);
            pos -= elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
        }
    }

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
        print("for (");
        //forInit = true;
        if (init != null) {
            init.accept(this);
	} else {
	    print(";");
	}
	//forInit = false;

        print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        print("; ");

        if (incr != null) {
	    SIRToStreamIt l2c = new SIRToStreamIt();
            incr.accept(l2c);
	    // get String
	    String str = l2c.getString();
	    // leave off the trailing semicolon if there is one
	    if (str.endsWith(";")) {
		print(str.substring(0, str.length()-1));
	    } else { 
		print(str);
	    }
        }
        print(")");
        newLine();
        pos += TAB_SIZE;
        body.accept(this);
        pos -= TAB_SIZE;
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) {
        visitCompoundStatement(body);
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            if (body[i] instanceof JIfStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JReturnStatement)) {
                newLine();
            }
            if (body[i] instanceof JReturnStatement && i > 0) {
                newLine();
            }

            newLine();
            body[i].accept(this);

            if (body[i] instanceof JVariableDeclarationStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JVariableDeclarationStatement)) {
                newLine();
            }
        }
    }

    /**
     * prints an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) {
        // Sometimes we want to discard the entire statement to
        // avoid empty statements.
            
        // Some special cases if the expression is an assignment:
        if (expr instanceof JAssignmentExpression)
        {
            JAssignmentExpression assign = (JAssignmentExpression)expr;
            JExpression rhs = assign.getRight();

            // Discard if the right-hand side is "new type[length]".
            if (rhs instanceof JNewArrayExpression)
                return;
            
            // Discard if the right-hand side is "new Complex".
            if (rhs instanceof JUnqualifiedInstanceCreation)
            {
                JUnqualifiedInstanceCreation uic =
                    (JUnqualifiedInstanceCreation)rhs;
                if (uic.getType().getCClass().getType().getIdent().equals("Complex"))
                    return;
            }
        }
        
        expr.accept(this);
        //if (!forInit) {
            print(";");
	    //}
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
        for (int i = 0; i < expr.length; i++) {
            if (i != 0) {
                print(", ");
            }
            expr[i].accept(this);
        }
	print(";");
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        newLine();
        print(";");
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        newLine();
        print("do ");
        body.accept(this);
        print("");
        print("while (");
        cond.accept(this);
        print(");");
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        newLine();
        print("continue");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        newLine();
        print("break");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        boolean wasToplevel = toplevel;
        toplevel = false;
        
        print("{");
        pos += TAB_SIZE;
        visitCompoundStatement(self.getStatementArray());
        if (comments != null) {
            visitComments(comments);
        }
        if (wasToplevel)
        {
            // Print some interesting characteristics of the object,
            // if it's a top-level composite object with said
            // characteristics.
            // (But print them *after* the rest of the code in
            // case they depend on things like variable declarations.)
            if (theStream instanceof SIRSplitJoin)
            {
                SIRSplitJoin sj = (SIRSplitJoin)theStream;
                newLine();
                sj.getSplitter().accept(this);
                newLine();
                sj.getJoiner().accept(this);
            }
            if (theStream instanceof SIRFeedbackLoop)
            {
                SIRFeedbackLoop fl = (SIRFeedbackLoop)theStream;
                newLine();
                fl.getJoiner().accept(this);
                newLine();
                fl.getSplitter().accept(this);
            }
            if (theStream instanceof SIRContainer)
            {
                SIRContainer cont = (SIRContainer)theStream;
                for (int i = 0; i < cont.size(); i++)
                {
                    SIRStream child = cont.get(i);
                    List params = cont.getParams(i);
                    // synthesize an SIRInitStatement, then visit
                    // it to print an add/body/loop statement and
                    // enqueue the stream's code to be printed.
                    newLine();
                    new SIRInitStatement(params, child).accept(this);
                }
            }
        }
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl) {
        decl.accept(this);
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr)
    {
	print("(");
        print("+");
        expr.accept(this);
	print(")");
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
	print("(");
        print("-");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("~");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("!");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
	print("(");
        print(type);
	print(")");
    }

    /**
     * prints a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix) {
        // Not a concept we have in StreamIt code.
        assert false;
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
        print("super");
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
	print("(");
        left.accept(this);
        if (oper == OPE_SL) {
            print(" << ");
        } else if (oper == OPE_SR) {
            print(" >> ");
        } else {
            print(" >>> ");
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
	print("(");
        left.accept(this);
        switch (oper) {
        case OPE_LT:
            print(" < ");
            break;
        case OPE_LE:
            print(" <= ");
            break;
        case OPE_GT:
            print(" > ");
            break;
        case OPE_GE:
            print(" >= ");
            break;
        default:
            assert false : self;
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
        // Maybe should be parenthesized, but exists in very
        // limited contexts.
        if (oper == OPE_PREINC) {
            print("++");
        } else {
            print("--");
        }
        expr.accept(this);
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
        expr.accept(this);
        if (oper == OPE_POSTINC) {
            print("++");
        } else {
            print("--");
        }
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
        print("(");
        expr.accept(this);
        print(")");
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public void visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
                                                JExpression prefix,
                                                String ident,
                                                JExpression[] params,
                                                JClassDeclaration decl)
    {
        prefix.accept(this);
        print(".new " + ident + "(");
        visitArgs(params, 0);
        print(")");
        // decl.genInnerJavaCode(this);
    }

    /**
     * Prints an unqualified instance creation expression.
     */
    public void visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                               JExpression prefix,
                                               String ident,
                                               JExpression[] params)
    {
        prefix.accept(this);
        print(".new " + ident + "(");
        visitArgs(params, 0);
        print(")");
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public void visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
                                                  CClassType type,
                                                  JExpression[] params,
                                                  JClassDeclaration decl)
    {
        print("new " + type + "(");
        visitArgs(params, 0);
        print(")");
        // decl.genInnerJavaCode(this);
    }

    /**
     * Prints an unqualified instance creation expression.
     */
    public void visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
                                                 CClassType type,
                                                 JExpression[] params)
    {
        print("new " + type + "(");
        visitArgs(params, 0);
        print(")");
    }

    /**
     * prints an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
        assert false : "should be handled in variable declaration";
        // We should never actually see one of these, and if we do,
        // the place where the expression appears either isn't convertible
        // to StreamIt or should deal with putting the dimension on the
        // left-hand side.  Can't handle initializers either.
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident)
    {
        // Meaningful?
        if (prefix != null) {
            prefix.accept(this);
            print(".");
        }
        print(ident);
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
	print("(");
        left.accept(this);
        print(" ");
        print(oper);
        print(" ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        /*
          if (ident != null && ident.equals(JAV_INIT)) {
          return; // we do not want generated methods in source code
          }
        */

        // rename enqueue functions
        if (ident.startsWith("enqueue"))
            ident = "enqueue";
        print(ident);
        print("(");
        int i = 0;
        visitArgs(args, i);
        print(")");
    }

    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
        print(ident);
    }

    /**
     * prints an instanceof expression
     */
    public void visitInstanceofExpression(JInstanceofExpression self,
                                          JExpression expr,
                                          CType dest) {
        expr.accept(this);
        print(" instanceof ");
        print(dest);
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
	print("(");
        left.accept(this);
        print(equal ? " == " : " != ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
	print("(");
        cond.accept(this);
        print(" ? ");
        left.accept(this);
        print(" : ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
        // Only legitimate as a statement-level expression, no parens.
        left.accept(this);
        switch (oper) {
        case OPE_STAR:
            print(" *= ");
            break;
        case OPE_SLASH:
            print(" /= ");
            break;
        case OPE_PERCENT:
            print(" %= ");
            break;
        case OPE_PLUS:
            print(" += ");
            break;
        case OPE_MINUS:
            print(" -= ");
            break;
        case OPE_SL:
            print(" <<= ");
            break;
        case OPE_SR:
            print(" >>= ");
            break;
        case OPE_BSR:
            print(" >>>= ");
            break;
        case OPE_BAND:
            print(" &= ");
            break;
        case OPE_BXOR:
            print(" ^= ");
            break;
        case OPE_BOR:
            print(" |= ");
            break;
        }
        right.accept(this);
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
	/*
	System.err.println("!!! self=" + self);
	System.err.println(" left=" + left);
	System.err.println(" ident=" + ident);
	System.err.println(" left.getType()==" + left.getType());
	System.err.println(" getCClass()=" +left.getType().getCClass());
	*/
        if (ident.equals(JAV_OUTER_THIS)) {
            // This identifier is used for the enclosing instance of
            // inner classes; see JLS 8.1.2.
            // We can only recover if there's not a shadowing field,
            // but we could probably get there from Java in the same
            // case.  Assume this is correct.
            // I think that means we print nothing.
            return;
        }
        int		index = ident.indexOf("_$");
        if (index != -1) {
            print(ident.substring(0, index));      // local var
        } else {
            if (!(left instanceof JThisExpression))
            {
                left.accept(this);
                print(".");
            }
            print(ident);
        }
    }

    /**
     * prints a class expression
     */
    public void visitClassExpression(JClassExpression self, CType type) {
        print(type);
        print(".class");
    }

    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type)
    {
	print("(");
        print("(");
        print(type);
        print(")");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
	print("(");
        print("(");
        print(type);
        print(")");
        print("(");
        expr.accept(this);
        print(")");
        print(")");
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        print("(");
        left.accept(this);
        switch (oper) {
        case OPE_BAND:
            print(" & ");
            break;
        case OPE_BOR:
            print(" | ");
            break;
        case OPE_BXOR:
            print(" ^ ");
            break;
        default:
            assert false : self;
        }
        right.accept(this);
        print(")");
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {
        // As an expression, this could be nested in parens.  But
        // since we're generally coming from translated StreamIt code
        // which doesn't allow arbitrary assignments as expressions,
        // we can ignore this.
        left.accept(this);
        print(" = ");
        right.accept(this);
    }

    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
        assert false : "StreamIt doesn't support Java array.length";
        prefix.accept(this);
        print(".length");
    }

    /**
     * prints an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        prefix.accept(this);
        print("[");
        accessor.accept(this);
        print("]");
    }

    /**
     * prints an array length expression
     */
    public void visitComments(JavaStyleComment[] comments) {
        for (int i = 0; i < comments.length; i++) {
            if (comments[i] != null) {
                visitComment(comments[i]);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitComment(JavaStyleComment comment) {
        // We don't care terribly.
        /*
        StringTokenizer	tok = new StringTokenizer(comment.getText(), "\n");

        if (comment.hadSpaceBefore()) {
            newLine();
        }

        if (comment.isLineComment()) {
            print("//");
	    if (tok.hasMoreTokens())
	      print(tok.nextToken().trim());
            p.println();
        } else {
            if (p.getLine() > 0) {
                if (!nl) {
                    newLine();
                }
                newLine();
            }
            print("/*");
            while (tok.hasMoreTokens()){
                String comm = tok.nextToken().trim();
                if (comm.startsWith("*")) {
                    comm = comm.substring(1).trim();
                }
                if (tok.hasMoreTokens() || comm.length() > 0) {
                    newLine();
                    print(" * " + comm);
                }
            }
            newLine();
            print(" * /");
            newLine();
        }

        if (comment.hadSpaceAfter()) {
            newLine();
        }
       */
    }

    /**
     * prints an array length expression
     */
    public void visitJavadoc(JavadocComment comment) {
        StringTokenizer	tok = new StringTokenizer(comment.getText(), "\n");
        boolean		isFirst = true;

        if (!nl) {
            newLine();
        }
        newLine();
        print("/**");
        while (tok.hasMoreTokens()) {
            String	text = tok.nextToken().trim();
            String	type = null;
            boolean	param = false;
            int	idx = text.indexOf("@param");
            if (idx >= 0) {
                type = "@param";
                param = true;
            }
            if (idx < 0) {
                idx = text.indexOf("@exception");
                if (idx >= 0) {
                    type = "@exception";
                    param = true;
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@exception");
                if (idx >= 0) {
                    type = "@exception";
                    param = true;
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@author");
                if (idx >= 0) {
                    type = "@author";
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@see");
                if (idx >= 0) {
                    type = "@see";
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@version");
                if (idx >= 0) {
                    type = "@version";
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@return");
                if (idx >= 0) {
                    type = "@return";
                }
            }
            if (idx < 0) {
                idx = text.indexOf("@deprecated");
                if (idx >= 0) {
                    type = "@deprecated";
                }
            }
            if (idx >= 0) {
                newLine();
                isFirst = false;
                if (param) {
                    text = text.substring(idx + type.length()).trim();
                    idx = Math.min(text.indexOf(" ") == -1 ? Integer.MAX_VALUE : text.indexOf(" "),
                                   text.indexOf("\t") == -1 ? Integer.MAX_VALUE : text.indexOf("\t"));
                    if (idx == Integer.MAX_VALUE) {
                        idx = 0;
                    }
                    String	before = text.substring(0, idx);
                    print(" * " + type);
                    pos += 12;
                    print(before);
                    pos += 20;
                    print(text.substring(idx).trim());
                    pos -= 20;
                    pos -= 12;
                } else {
                    text = text.substring(idx + type.length()).trim();
                    print(" * " + type);
                    pos += 12;
                    print(text);
                    pos -= 12;
                }
            } else {
                text = text.substring(text.indexOf("*") + 1);
                if (tok.hasMoreTokens() || text.length() > 0) {
                    newLine();
                    print(" * ");
                    pos += isFirst ? 0 : 32;
                    print(text.trim());
                    pos -= isFirst ? 0 : 32;
                }
            }
        }
        newLine();
        print(" */");
    }

    // ----------------------------------------------------------------------
    // STREAMIT IR HANDLERS
    // ----------------------------------------------------------------------

    public void visitCreatePortalExpression(SIRCreatePortal self) {
        print("create_portal()");
    }

    public void visitInitStatement(SIRInitStatement self,
                                   SIRStream stream)
    {
        // What keyword do we print?  Always "add", unless we're
        // looking at a feedback loop...
        if (theStream instanceof SIRFeedbackLoop)
        {
            SIRFeedbackLoop fl = (SIRFeedbackLoop)theStream;
            if (stream == fl.getBody())
                print("body ");
            else if (stream == fl.getLoop())
                print("loop ");
            else
            {
                assert false : stream;
                print("add ");
            }
        }
        else
            print("add ");

        // Is stream anonymous?
        if (stream.getName() == null)
            stream.accept(this);
        else
        {
            // Certain types of builtins have types and fixed names:
            if (stream instanceof SIRIdentity)
                print("Identity<" + stream.getInputType() + ">");
            else
                print(stream.getName());
            // Dump the parameter list, if any.
            List params = self.getArgs();
            if (!(params.isEmpty()))
            {
                print("(");
                JExpression[] args =
                    (JExpression[])params.toArray(new JExpression[0]);
                visitArgs(args, 0);
                print(")");
            }
            print(";");
            streamQueue.add(stream);
        }
    }

    public void visitInterfaceTable(SIRInterfaceTable self)
    {
        String iname = self.getIface().getIdent();
        JMethodDeclaration[] methods = self.getMethods();
        boolean first = true;
        
        print("{ ");
        for (int i = 0; i < methods.length; i++)
        {
            if (!first) print(", ");
            first = false;
            print(iname + "_" + methods[i].getName());
        }
        print("}");
    }
    
    public void visitLatency(SIRLatency self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyMax(SIRLatencyMax self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyRange(SIRLatencyRange self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencySet(SIRLatencySet self)
    {
        print("LATENCY_BEST_EFFORT");
    }

    public void visitMessageStatement(SIRMessageStatement self,
                                      JExpression portal,
                                      String iname,
                                      String ident,
                                      JExpression[] params,
                                      SIRLatency latency)
    {
	print("send_" + iname + "_" + ident + "(");
        portal.accept(this);
        print(", ");
        latency.accept(this);
        if (params != null)
            for (int i = 0; i < params.length; i++)
                if (params[i] != null)
                {
                    print(", ");
                    params[i].accept(this);
                }
        print(");");
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
        print("peek(");
        num.accept(this);
        print(")");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
        // LIRToC tests:
	// if (self.getNumPop()>1) {
        //   ...
        // }
        // Maybe we should care.
        print("pop()");
    }
    
    public void visitPortal(SIRPortal self)
    {
        // Have we seen this portal before?
        if (!(portalNames.containsKey(self)))
        {
            portalCount++;
            String theName = "__portal_" + portalCount;
            portalNames.put(self, theName);
        }
        print(portalNames.get(self));
    }

    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression exp)
    {
        print("print(");
        exp.accept(this);
        print(");");
    }
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
        print("push(");
        val.accept(this);
        print(")");
    }
    
    public void visitPhaseInvocation(SIRPhaseInvocation self,
                                     JMethodCallExpression call,
                                     JExpression peek,
                                     JExpression pop,
                                     JExpression push)
    {
        print("/* phase invocation: ");
        call.accept(this);
        print("; */");
    }

    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
                                          JExpression portal,
					  SIRStream receiver, 
					  JMethodDeclaration[] methods)
    {
        assert false : "TODO: implement SIR messaging";
    }
    
    public void visitRegSenderStatement(SIRRegSenderStatement self,
                                        String fn,
                                        SIRLatency latency)
    {
        assert false : "TODO: implement SIR messaging";
    }


    /**
     * Visits a file reader.
     */
    public void visitFileReader(LIRFileReader self) {
        assert false;
    }

    /**
     * Visits a file writer.
     */
    public void visitFileWriter(LIRFileWriter self) {
        assert false;
    }

    /**
     * Visits an identity filter.
     */
    public void visitIdentity(LIRIdentity self) 
    {
        assert false;
    }

    public void visitSetChild(LIRSetChild self,
                              JExpression streamContext,
                              String childType,
                              String childName)
    {
        assert false;
    }
    
    public void visitSetTape(LIRSetTape self,
                             JExpression streamContext,
                             JExpression srcStruct,
                             JExpression dstStruct,
                             CType type,
                             int size)
    {
        assert false;
    }
    
    /**
     * Visits a function pointer.
     */
    public void visitFunctionPointer(LIRFunctionPointer self,
                                     String name)
    {
        assert false;
    }
    
    /**
     * Visits an LIR node.
     */
    public void visitNode(LIRNode self)
    {
        assert false;
    }

    /**
     * Visits an LIR register-receiver statement.
     */
    public void visitRegisterReceiver(LIRRegisterReceiver self,
                                      JExpression streamContext,
                                      SIRPortal portal,
                                      String childName,
                                      SIRInterfaceTable itable)
    {
        // Probably isn't in SIR code.
        assert false;
        // (But do consider messaging eventually.)
    }

    /**
     * Visits a child registration node.
     */
    public void visitSetChild(LIRSetChild self,
                              JExpression streamContext,
                              JExpression childContext)
    {
        assert false;
    }
    
    /**
     * Visits a decoder registration node.
     */
    public void visitSetDecode(LIRSetDecode self,
                               JExpression streamContext,
                               LIRFunctionPointer fp)
    {
        assert false;
    }

    /**
     * Visits a feedback loop delay node.
     */
    public void visitSetDelay(LIRSetDelay self,
                              JExpression data,
                              JExpression streamContext,
                              int delay,
                              CType type,
                              LIRFunctionPointer fp)
    {
        /* This doesn't work quite right yet, but it's closer. */
        print("FEEDBACK_DELAY(");
        data.accept(this);
        print(", ");
        streamContext.accept(this);
        print(", " + delay + ", ");
        print(type);
        print(", ");
        fp.accept(this);
        print(");");
    }
    
    /**
     * Visits an encoder registration node.
     */
    public void visitSetEncode(LIRSetEncode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp)
    {
        print("set_encode(");
        streamContext.accept(this);
        print(", ");
        fp.accept(this);
        print(");");
    }
    
    /**
     * Visits a joiner-setting node.
     */
    public void visitSetJoiner(LIRSetJoiner self,
                               JExpression streamContext,
                               SIRJoinType type,
                               int ways,
                               int[] weights)
    {
        print("set_joiner(");
        streamContext.accept(this);
        print(", ");
        print(type);
        print(", " + String.valueOf(ways));
        if (weights != null)
        {
            for (int i = 0; i < weights.length; i++)
                print(", " + String.valueOf(weights[i]));
        }
        print(");");
    }

    /**
     * Visits a peek-rate-setting node.
     */
    public void visitSetPeek(LIRSetPeek self,
                      JExpression streamContext,
                      int peek)
    {
        print("set_peek(");
        streamContext.accept(this);
        print(", " + peek + ");");
    }
    
    /**
     * Visits a pop-rate-setting node.
     */
    public void visitSetPop(LIRSetPop self,
                     JExpression streamContext,
                     int pop)
    {
        print("set_pop(");
        streamContext.accept(this);
        print(", " + pop + ");");
    }
    
    /**
     * Visits a push-rate-setting node.
     */
    public void visitSetPush(LIRSetPush self,
                      JExpression streamContext,
                      int push)
    {
        print("set_push(");
        streamContext.accept(this);
        print(", " + push + ");");
    }

    /**
     * Visits a splitter-setting node.
     */
    public void visitSetSplitter(LIRSetSplitter self,
                                 JExpression streamContext,
                                 SIRSplitType type,
                                 int ways,
                                 int[] weights)
    {
        print("set_splitter(");
        streamContext.accept(this);
        print(", " + type + ", " + String.valueOf(ways));
        if (weights != null)
        {
            for (int i = 0; i < weights.length; i++)
                print(", " + String.valueOf(weights[i]));
        }
        print(");");
    }

    /**
     * Visits a stream-type-setting node.
     */
    public void visitSetStreamType(LIRSetStreamType self,
                            JExpression streamContext,
                            LIRStreamType streamType)
    {
        print("set_stream_type(");
        streamContext.accept(this);
        print(", " + streamType + ");");
    }
    
    /**
     * Visits a work-function-setting node.
     */
    public void visitSetWork(LIRSetWork self,
                      JExpression streamContext,
                      LIRFunctionPointer fn)
    {
        assert false;
    }

    public void visitMainFunction(LIRMainFunction self,
                                  String typeName,
                                  LIRFunctionPointer init,
				  List initStatements)
    {
        assert false;
    }

    /**
     * Visits a set body of feedback loop.
     */
    public void visitSetBodyOfFeedback(LIRSetBodyOfFeedback self,
				       JExpression streamContext,
                                       JExpression childContext,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
        assert false;
    }

    /**
     * Visits a set loop of feedback loop.
     */
    public void visitSetLoopOfFeedback(LIRSetLoopOfFeedback self,
				       JExpression streamContext,
                                       JExpression childContext,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
        assert false;
    }

    /**
     * Visits a set a parallel stream.
     */
    public void visitSetParallelStream(LIRSetParallelStream self,
				       JExpression streamContext,
                                       JExpression childContext,
				       int position,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
        /* For  split/joins now.  Again, assume registration has
         * already happened; we just need to connect tapes.
         * Use the position'th slot on the splitter output and
         * joiner input. */
	if (inputSize!=0) {
	    print("create_splitjoin_tape(");
	    streamContext.accept(this);
	    print(", SPLITTER, OUTPUT, " + position + ", ");
	    childContext.accept(this);
	    print(", sizeof(");
            print(inputType);
            print("), " + inputSize + ");");
	    newLine();
	}
	if (outputSize!=0) {
	    print("create_splitjoin_tape(");
	    streamContext.accept(this);
	    print(", JOINER, INPUT, " + position + ", ");
	    childContext.accept(this);
	    print(", sizeof(");
            print(outputType);
            print("), " + outputSize + ");");
	}
    }

    /**
     * Visits a work function entry.
     */
    public void visitWorkEntry(LIRWorkEntry self)
    {
        // We have no way to deal with this, really.
        // Possibly not worth asserting, but we've probably gone too
        // far to recover structure.
        assert false;
    }

    /**
     * Visits a work function exit.
     */
    public void visitWorkExit(LIRWorkExit self)
    {
        // Similarly.
        assert false;
    }


    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * prints an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) {
        newLine();
        if (expr != null) {
            print("case ");
            expr.accept(this);
            print(": ");
        } else {
            print("default: ");
        }
    }

    /**
     * prints an array length expression
     */
    public void visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].accept(this);
        }
        pos += TAB_SIZE;
        for (int i = 0; i < stmts.length; i++) {
            newLine();
            stmts[i].accept(this);
        }
        pos -= TAB_SIZE;
    }

    /**
     * prints an array length expression
     */
    public void visitCatchClause(JCatchClause self,
                                 JFormalParameter exception,
                                 JBlock body) {
        print(" catch (");
        exception.accept(this);
        print(") ");
        body.accept(this);
    }

    /**
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
        if (value)
            print("true");
        else
            print("false");
    }

    /**
     * prints a byte literal
     */
    public void visitByteLiteral(byte value) {
        print("((byte)" + value + ")");
    }

    /**
     * prints a character literal
     */
    public void visitCharLiteral(char value) {
        switch (value) {
        case '\b':
            print("'\\b'");
            break;
        case '\r':
            print("'\\r'");
            break;
        case '\t':
            print("'\\t'");
            break;
        case '\n':
            print("'\\n'");
            break;
        case '\f':
            print("'\\f'");
            break;
        case '\\':
            print("'\\\\'");
            break;
        case '\'':
            print("'\\''");
            break;
        case '\"':
            print("'\\\"'");
            break;
        default:
            print("'" + value + "'");
        }
    }

    /**
     * prints a double literal
     */
    public void visitDoubleLiteral(double value) {
        assert false;
        print("((double)" + value + ")");
    }

    /**
     * prints a float literal
     */
    public void visitFloatLiteral(float value) {
        print(value);
    }

    /**
     * prints a int literal
     */
    public void visitIntLiteral(int value) {
        print(value);
    }

    /**
     * prints a long literal
     */
    public void visitLongLiteral(long value) {
        print("(" + value + "L)");
    }

    /**
     * prints a short literal
     */
    public void visitShortLiteral(short value) {
        print("((short)" + value + ")");
    }

    /**
     * prints a string literal
     */
    public void visitStringLiteral(String value) {
        print('"' + value + '"');
    }

    /**
     * prints a null literal
     */
    public void visitNullLiteral() {
        assert false;
        print("null");
    }

    /**
     * prints an array length expression
     */
    public void visitPackageName(String name) {
        assert false;
        // print("package " + name + ";");
        // newLine();
    }

    /**
     * prints an array length expression
     */
    public void visitPackageImport(String name) {
        assert false;
        // print("import " + name.replace('/', '.') + ".*;");
    }

    /**
     * prints an array length expression
     */
    public void visitClassImport(String name) {
        assert false;
        // print("import " + name.replace('/', '.') + ";");
    }

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) {
        print(type);
        if (ident.indexOf("$") == -1) {
            print(" ");
            print(ident);
        }
    }

    /**
     * prints an array length expression
     */
    public void visitArgs(JExpression[] args, int base) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i + base != 0) {
                    print(", ");
                }
                args[i].accept(this);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params)
    {
        newLine();
        print(functorIsThis ? "this" : "super");
        print("(");
        visitArgs(params, 0);
        print(");");
    }

    /**
     * prints an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {
        newLine();
        print("{");
        for (int i = 0; i < elems.length; i++) {
            if (i != 0) {
                print(", ");
            }
            elems[i].accept(this);
        }
        print("}");
    }

    
    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------

    protected void newLine() {
        p.println();
    }

    // Special case for CTypes, to map some Java types to C types.
    protected void print(CType s)
    {
        if (s instanceof CArrayType)
        {
            print(((CArrayType)s).getElementType());
            JExpression[] dims = ((CArrayType)s).getDims();
	    if (dims != null)
		for (int i = 0; i < dims.length; i++)
                {
                    print("[");
                    dims[i].accept(this);
                    print("]");
                }
	    else
            {
                print("[");
                // I suspect this isn't entirely what we want.
                // In fact, it looks like it prints the number
                // of array bounds, which is frequently "1".
                // Eit.
                print(((CArrayType)s).getArrayBound());
                print("]");
            }
        }
        else if (s.toString().equals("Complex"))
            print("complex"); // revert to primitive type
        else if (s.toString().endsWith("Portal"))
        {
            // Rewrite this to be a Portal<foo> type.
            String name = s.toString();
            print("Portal<" + name.substring(0, name.length() - 6) + ">");
        }
	else
            print(s.toString());
    }

    protected void print(Object s) {
        print(s.toString());
    }

    protected void print(String s) {
        p.setPos(pos);
        p.print(s);
    }

    protected void print(boolean s) {
        print("" + s);
    }

    protected void print(int s) {
        print("" + s);
    }

    protected void print(char s) {
        print("" + s);
    }

    protected void print(double s) {
        print("" + s);
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    //protected boolean			forInit;	// is on a for init
    protected int				TAB_SIZE = 2;
    protected int				WIDTH = 80;
    protected int				pos;
    protected String                      className;
    protected boolean isStruct;
    protected boolean                   toplevel;

    protected TabbedPrintWriter		p;
    protected StringWriter                str;
    protected boolean			nl = true;
    protected boolean                   declOnly = false;

    protected int                       portalCount;
    protected Map                       portalNames;
    protected SIROperator               theStream;
}
