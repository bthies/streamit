package at.dms.kjc.sir;

import java.io.*;
import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.compiler.*;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.common.CodeGenerator;

/**
 * Dump an SIR tree into a StreamIt program.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SIRToStreamIt.java,v 1.37 2006-09-25 13:54:41 dimock Exp $
 */
public class SIRToStreamIt
    implements Constants, SLIRVisitor, AttributeStreamVisitor, CodeGenerator
{

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    //protected boolean         forInit;    // is on a for init
    protected String  className;
    protected boolean isStruct;
    protected boolean toplevel;

    private CodegenPrintWriter p;
    protected boolean nl = true;
    protected boolean declOnly = false;

    protected int portalCount;
    protected Map<SIRPortal, String> portalNames;
    protected SIROperator theStream;
    /**
     * >0 when in a for loop header.
     */
    private int forLoopHeader;

    // ------------------------------------------------------------------------
    // METHODS
    // -----------------------------------------------------------------------

    public CodegenPrintWriter getPrinter() { return p; }
    
    /**
     * Top-level entry point if you are processing SIR that
     * has not been compiled at all.
     *
     * -- i.e., the stream graph has not
     * been unrolled.  This includes the stream graph unrolling as
     * part of its processing.
     * 
     * If you are using 'static' sections and have not yet run StaticProp
     * you may get error messages about non-constant parameters etc.
     */
    public static void runBeforeCompiler(SIRStream str,
                                         JInterfaceDeclaration[] interfaces,
                                         SIRInterfaceTable[] interfaceTables,
                                         SIRStructure[] structs)
    {
        // Rename top-level object.
        str.setIdent(str.getIdent() + "_c");
        
        // Flattener code: run constant prop and such.
        ConstantProp.propagateAndUnroll(str);
        ConstructSIRTree.doit(str);
        FieldProp.doPropagate(str);
        new BlockFlattener().flattenBlocks(str);
        run(str, interfaces, interfaceTables, structs);
    }
    
    /**
     * Top-level entry point.  
     *
     * For use on stream graphs
     * that have already been expanded in the compiler.
     */
    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[] structs) {
        SIRToStreamIt s2s = new SIRToStreamIt();

        // allow null structs for simplicity
        if (structs!=null) {
            for (int i = 0; i < structs.length; i++)
                {
                    assert structs[i] != null;
                    if (!(structs[i].getIdent().equals("Complex")))
                        structs[i].accept(s2s);
                }
        }

        // allow null interfaces for simplicity
        if (interfaces!=null) {
            for (int i = 0; i < interfaces.length; i++)
                interfaces[i].accept(s2s);
        }

        s2s.visitAnyStream(str);
        System.err.println(s2s.getPrinter().getString());
        s2s.close();
    }

    /**
     * Entry point for a stream without interfaces or structs.
     */
    public static void run(SIRStream str) {
        run(str, null, null, null);
    }

    /**
     * Top-level entry point for running on a kopi phylum.
     */
    public static void run(JPhylum phylum) {
        SIRToStreamIt s2s = new SIRToStreamIt();
        phylum.accept(s2s);
        System.err.println(s2s.getPrinter().getString());
        s2s.close();
    }

    // Version using globals
    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[] structs,
                           SIRGlobal[] globals) {

        run(str, interfaces, interfaceTables, structs);
        for (int i = 0; i < globals.length; i++) {
            runOnGlobal(globals[i]);
        }
    }
    
    public static void runOnGlobal(SIRGlobal global) {
        SIRToStreamIt s2s = new SIRToStreamIt();
        s2s.processGlobal(global);
        System.err.println(s2s.getPrinter().getString());
        s2s.close();
    }
    
    public static void runNonRecursive(SIRStream str) {
        SIRToStreamIt s2s = new SIRToStreamIt();
        str.accept(s2s);
        System.err.println(s2s.getPrinter().getString());
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
        this.p = new CodegenPrintWriter();
    }

    /**
     * Generates code for <flatClass> and sends to System.err.
     */
    public static void generateCode(JClassDeclaration flat) {
        SIRToStreamIt s2s = new SIRToStreamIt(new CodegenPrintWriter(new PrintWriter(System.out)));

        // Print all of the portals.
        for (int i = 0; i < SIRPortal.getPortals().length; i++) {

            s2s.getPrinter().print("portal ");
            SIRPortal.getPortals()[i].accept(s2s);
            s2s.getPrinter().print(";");
            s2s.getPrinter().newLine();
        }

        flat.accept(s2s);
        s2s.close();
    }

    /**
     * construct a pretty printer object for java code
     * @param   fileName        the file into the code is generated
     */
    private SIRToStreamIt(CodegenPrintWriter p) {
        this.p = p;
        this.toplevel = false;
        this.portalCount = 0;
        this.portalNames = new java.util.HashMap<SIRPortal, String>();
        this.seenStreams = new java.util.HashSet<SIROperator>();
        this.streamQueue = new java.util.LinkedList<SIROperator>();
    }

    /**
     * Close the stream at the end
     */
    public void close() {
        p.close();
    }

    public String getString() {
        return p.getString();
    }

    // ----------------------------------------------------------------------
    // VISIT STREAMS
    // ----------------------------------------------------------------------

    private Set<SIROperator> seenStreams;
    private List<SIROperator> streamQueue;

    public void visitAnyStream(SIROperator op)
    {
        streamQueue.add(op);
        while (!streamQueue.isEmpty())
            {
                SIROperator o = streamQueue.get(0);
                streamQueue.remove(0);
                // Do nothing if we've seen it; print it otherwise
                if (!seenStreams.contains(o))
                    {
                        seenStreams.add(o);
                        // Avoid Recursive stubs and avoid builtins
                        if (o instanceof SIRRecursiveStub) {
                            p.println("// Found unexpanded stub: " + o.getIdent() + " / " + o.getName());
                        } else if (!(o instanceof SIRPredefinedFilter))  {
                            theStream = o;
                            o.accept(this);
                            p.newLine();
                        }
                    }   
            }
    }

    private void printHeader(SIRStream self, String type)
    {
        CType inType = null;
        try {
            inType= self.getInputType();
        } catch (RuntimeException x) {
            // ignore errors thrown when dealing with a Recursive Stub
        }
        CType outType = null;
        try {
            outType = self.getOutputType();
        } catch (RuntimeException x) {
            // ignore errors thrown when dealing with a Recursive Stub
        }
 
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
                printType(inType);
                p.print("->");
                printType(outType);
                p.print(" ");
            }
        p.print(type);
        if (self.getIdent() != null)
            {
                // if the parent is null, this is the top-level stream;
                // there is only one of it, so print its [non-unique]
                // ident rather than its [unique] name so the class name
                // matches the filename
                if (self.getParent() == null)
                    p.print(" " + self.getIdent());
                else
                    p.print(" " + self.getName());
            }

        // In SIR, streams don't have parameter lists, but their
        // init functions do.  Print a parameter list.
        JMethodDeclaration init = self.getInit();
        JFormalParameter[] parameters = init.getParameters();
        if (parameters.length > 0)
            {
                p.print("(");
                boolean first = true;
            
                for (int i = 0; i < parameters.length; i++) {
                    if (!first)
                        p.print(", ");
                    first = false;
                    parameters[i].accept(this);
                }
                p.print(")");
            }
        
        p.newLine();
    }

    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields)
    {
        p.print("struct " + self.getIdent());
        p.newLine();
        p.print("{");
        p.indent();
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
        p.newLine();
        p.newLine();
        return null;
    }

    // Global sections are not part of the stream graph and
    // thus can not be visited from the root of the stream graph.
  
    public void processGlobal(SIRGlobal self) {
        JFieldDeclaration[] fields = self.getFields();
        JMethodDeclaration[] methods = self.getMethods();

        p.print("static /*" + self.getIdent() + "*/ {");
        p.newLine();
        p.indent();
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        for (int i = 0; i < methods.length; i++)
            methods[i].accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
        p.newLine();
    }
    
    /* visit a filter */
    private boolean inInit = false;
    private boolean inWork = false;
    private boolean inInitWork = false;
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
        printHeader(self, "filter");
        p.print("{");
        p.newLine();
        p.indent();
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        for (int i=0; i < methods.length; i++) {
            // small hack to detect init, initwork, work functions.
            // Can't go just based on name because they are sometimes
            // renamed.
            if (methods[i]==init) {
                inInit = true;
            } else if (methods[i] == work) {
                inWork = true;
            } else if (self instanceof SIRTwoStageFilter &&
                       methods[i]==((SIRTwoStageFilter)self).getInitWork()) {
                inInitWork = true;
            }
            methods[i].accept(this);
            inInit = false;
            inWork = false;
            inInitWork = false;
        }
        p.outdent();
        p.newLine();
        p.print("}");
        p.newLine();
        return null;
    }
  
    /* visit a phased filter */
    public Object visitPhasedFilter(SIRPhasedFilter self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration work,
                                    JMethodDeclaration[] initPhases,
                                    JMethodDeclaration[] phases,
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
            p.print("split duplicate;");
        else if (type.isNull())
            p.print("split roundrobin(0);");
        else if (type.isRoundRobin())
            {
                p.print("split roundrobin(");

                // Check (the hard way) for a uniform round-robin.
                boolean uniform = false;
                if (weights.length > 0) {
                    uniform = true;
                    JExpression e = self.getWeightNoChecking(0);
                    if (e instanceof JIntLiteral) {
                        int w = self.getWeight(0);
                        for (int i = 1; i < weights.length; i++) {
                            e = self.getWeightNoChecking(i);
                            if (e instanceof JIntLiteral) {
                                if (self.getWeight(i) != w) {
                                    uniform = false;
                                }
                            } else {uniform = false;}
                        }
                    } else {uniform = false;}
                }
                if (uniform)
                    p.print(self.getWeight(0));
                else
                    {
                        boolean first = true;
                        for (int i = 0; i < weights.length; i++)
                            {
                                assert weights[i] != null;
                                if (!first)
                                    p.print(", ");
                                first = false;
                                weights[i].accept(this);
                            }
                    }
            
                p.print(");");
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
            p.print("join roundrobin(0);");
        else if (type.isRoundRobin())
            {
                p.print("join roundrobin(");

                // Check (the hard way) for a uniform round-robin.
                boolean uniform = false;            

                if (weights.length > 0) {
                    uniform = true;
                    JExpression e = self.getWeightNoChecking(0);
                    if (e instanceof JIntLiteral) {
                        int w = self.getWeight(0);
                        for (int i = 1; i < weights.length; i++) {
                            e = self.getWeightNoChecking(i);
                            if (e instanceof JIntLiteral) {
                                if (self.getWeight(i) != w) {
                                    uniform = false;
                                }
                            } else {
                                uniform = false;
                            }
                        }
                    } else {
                        uniform = false;
                    }
                }
                if (uniform)
                    p.print(self.getWeight(0));
                else
                    {
                        boolean first = true;
                        for (int i = 0; i < weights.length; i++)
                            {
                                assert weights[i] != null;
                                if (!first)
                                    p.print(", ");
                                first = false;
                                weights[i].accept(this);
                            }
                    }
            
                p.print(");");
            }
        else
            assert false : self;
        return null;
    }
    
    /* pre-visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
                                JFieldDeclaration[] fields,
                                JMethodDeclaration[] methods,
                                JMethodDeclaration init)
    {
        printHeader(self, "pipeline");
        p.newLine();

        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        p.newLine();

        p.indent();
        toplevel = true;
        init.getBody().accept(this);
        p.outdent();
        p.newLine();
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
        p.newLine();

        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        p.newLine();

        p.indent();
        toplevel = true;
        init.getBody().accept(this);
        p.outdent();
        p.newLine();
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
        p.newLine();

        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        p.newLine();

        toplevel = true;
        init.getBody().accept(this);
        p.newLine();
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
                p.newLine();
            }
        }

        for (int i = 0; i < importedPackages.length ; i++) {
            if (!importedPackages[i].getName().equals("java/lang")) {
                importedPackages[i].accept(this);
                p.newLine();
            }
        }

        for (int i = 0; i < importedClasses.length ; i++) {
            importedClasses[i].accept(this);
            p.newLine();
        }

        for (int i = 0; i < typeDeclarations.length ; i++) {
            p.newLine();
            typeDeclarations[i].accept(this);
            p.newLine();
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

        p.newLine();
        p.print("typedef struct " + className + " {");
        p.indent();
        if (body != null) {
            for (int i = 0; i < body.length ; i++) {
                if (body[i] instanceof JFieldDeclaration)
                    body[i].accept(this);
            }
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i].accept(this);
        }    

        p.outdent();
        p.newLine();
        if (isStruct)
            p.print("} " + className + ";");
        else
            p.print("} _" + className + ", *" + className + ";");

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
        p.print(" {");
        p.indent();
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
        p.outdent();
        p.newLine();
        p.print("}");
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
            p.newLine();
            p.print("typedef struct " + pname + " {");
            p.indent();
            for (int j = 0; j < params.length; j++) {
                p.newLine();
                printType(params[j]);
                p.print(" p" + j + ";");
            }
            p.outdent();
            p.newLine();
            p.print("} _" + pname + ", *" + pname + ";");

            // And now print a wrapper for send_message().
            p.newLine();
            p.print("void send_" + name + "(portal p, latency l");
            for (int j = 0; j < params.length; j++) {
                p.print(", ");
                printType(params[j]);
                p.print(" p" + j);
            }
            p.print(") {");
            p.indent();
            p.newLine();
            p.print(pname + " q = malloc(sizeof(_" + pname + "));");
            for (int j = 0; j < params.length; j++) {
                p.newLine();
                p.print("q->p" + j + " = p" + j + ";");
            }
            p.newLine();
            p.print("send_message(p, " + i + ", l, q);");
            p.outdent();
            p.newLine();
            p.print("}");
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
        p.newLine();
        printType(type);
        p.print(" ");
        p.print(ident);
        if (expr != null) {
            p.print(" = ");
            expr.accept (this);
        }
        p.print(";");
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
        p.newLine();
        // TODO: figure out what kind of function this is
        // (phase, handler, helper are special).

        // Treat init and work functions specially.
        if (inInit) {
            p.print("init");
        } else if (inWork) {
            p.print("work");
        } else if (inInitWork) {
            p.print("prework");
        } else {
            // p.print(CModifier.toString(modifiers));
            printType(returnType);
            p.print(" ");
            p.print(ident);
            p.print("(");
            int count = 0;
            
            for (int i = 0; i < parameters.length; i++) {
                if (count != 0) {
                    p.print(", ");
                }
                
                // if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
                count++;
                // }
            }
            p.print(")");
        }

        // Print I/O rates if they're available
        if (self.doesIO()) {
            p.print(" pop ");
            self.getPop().accept(this);
            p.print(" peek ");
            self.getPeek().accept(this);
            p.print(" push ");
            self.getPush().accept(this);
        }

        if (declOnly)
            {
                p.print(";");
                return;
            }

        p.newLine();
        if (body != null) {
            body.accept(this);
        } else {
            p.print(";");
        }
        p.newLine();
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
        p.newLine();
        p.print(CModifier.toString(modifiers));
        p.print(ident);
        p.print("_");
        p.print(ident);
        p.print("(");
        int count = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                p.print(", ");
            }
            if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
                count++;
            }
        }
        p.print(")");
        /*
          for (int i = 0; i < exceptions.length; i++) {
          if (i != 0) {
          p.print(", ");
          } else {
          p.print(" throws ");
          }
          p.print(exceptions[i].toString());
          }
        */
        p.print(" ");
        body.accept(this);
        p.newLine();
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
        p.print("while (");
        cond.accept(this);
        p.print(") ");

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
            p.print("[");
            SIRToStreamIt toC = new SIRToStreamIt(this.p);
            dims[i].accept(toC);
            p.print("]");
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
        // p.print(CModifier.toString(modifiers));
        // Uh, hmm, maybe we should do this conversion globally in
        // SIR.  But in this case we know the type a little better
        // than we do just from Java.
        if (expr != null && expr instanceof JNewArrayExpression)
            {
                assert type instanceof CArrayType;
                printType(((CArrayType)type).getElementType());
                printLocalArrayDecl((JNewArrayExpression)expr);
            }
        else
            printType(type);
        p.print(" ");
        p.print(ident);
        if (expr != null && !(expr instanceof JNewArrayExpression)) {
            p.print(" = ");
            expr.accept(this);
        }
        p.print(";");
    }

    /**
     * prints a try-catch statement
     */
    public void visitTryCatchStatement(JTryCatchStatement self,
                                       JBlock tryClause,
                                       JCatchClause[] catchClauses) {
        // Not legit StreamIt code!
        assert false;
        p.print("try ");
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
        p.print("try ");
        tryClause.accept(this);
        if (finallyClause != null) {
            p.print(" finally ");
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
        p.print("throw ");
        expr.accept(this);
        p.print(";");
    }

    /**
     * prints a synchronized statement
     */
    public void visitSynchronizedStatement(JSynchronizedStatement self,
                                           JExpression cond,
                                           JStatement body) {
        // Not legit StreamIt code!
        assert false;
        p.print("synchronized (");
        cond.accept(this);
        p.print(") ");
        body.accept(this);
    }

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        p.print("switch (");
        expr.accept(this);
        p.print(") {");
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        p.newLine();
        p.print("}");
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        p.print("return");
        if (expr != null) {
            p.print(" ");
            expr.accept(this);
        }
        p.print(";");
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        p.print(label + ":");
        stmt.accept(this);
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {
        p.print("if (");
        cond.accept(this);
        p.print(") ");
        if (!(thenClause instanceof JBlock)) p.indent();
        thenClause.accept(this);
        if (!(thenClause instanceof JBlock)) p.outdent();
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                p.print(" ");
            } else {
                p.newLine();
            }
            p.print("else ");
            if (!(elseClause instanceof JBlock 
                  || elseClause instanceof JIfStatement)) p.indent();
            elseClause.accept(this);
            if (!(elseClause instanceof JBlock 
                  || elseClause instanceof JIfStatement)) p.outdent();
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
        forLoopHeader++;
        p.print("for (");
        //forInit = true;
        if (init != null) {
            init.accept(this);
        } else {
            p.print(";");
        }
        //forInit = false;

        p.print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        p.print("; ");

        if (incr != null) {
            SIRToStreamIt l2c = new SIRToStreamIt();
            incr.accept(l2c);
            // get String
            String str = l2c.getString();
            // leave off the trailing semicolon if there is one
            if (str.endsWith(";")) {
                p.print(str.substring(0, str.length()-1));
            } else { 
                p.print(str);
            }
        }
        forLoopHeader--;
        p.print(")");
        p.newLine();
        p.indent();
        body.accept(this);
        p.outdent();
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
                p.newLine();
            }
            if (body[i] instanceof JReturnStatement && i > 0) {
                p.newLine();
            }

            p.newLine();
            body[i].accept(this);

            if (body[i] instanceof JVariableDeclarationStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JVariableDeclarationStatement)) {
                p.newLine();
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
        p.print(";");
        //}
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
        for (int i = 0; i < expr.length; i++) {
            if (i != 0) {
                p.print(", ");
            }
            expr[i].accept(this);
        }
        p.print(";");
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        //if we are inside a for loop header, we need to print 
        //the ; of an empty statement
        if (forLoopHeader > 0) {
            p.newLine();
            p.print(";");
        }

    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        p.newLine();
        p.print("do ");
        body.accept(this);
        p.print("");
        p.print("while (");
        cond.accept(this);
        p.print(");");
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        p.newLine();
        p.print("continue");
        if (label != null) {
            p.print(" " + label);
        }
        p.print(";");
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        p.newLine();
        p.print("break");
        if (label != null) {
            p.print(" " + label);
        }
        p.print(";");
    }

    /**
     * prints an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        boolean wasToplevel = toplevel;
        toplevel = false;
        
        p.print("{");
        p.indent();
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
                        p.newLine();
                        sj.getSplitter().accept(this);
                        p.newLine();
                        sj.getJoiner().accept(this);
                    }
                if (theStream instanceof SIRFeedbackLoop)
                    {
                        SIRFeedbackLoop fl = (SIRFeedbackLoop)theStream;
                        p.newLine();
                        fl.getJoiner().accept(this);
                        p.newLine();
                        fl.getSplitter().accept(this);
                    }
                if (theStream instanceof SIRContainer)
                    {
                        SIRContainer cont = (SIRContainer)theStream;
                        for (int i = 0; i < cont.size(); i++)
                            {
                                SIRStream child = cont.get(i);
                                //List params = cont.getParams(i);
                                // synthesize an SIRInitStatement, then visit
                                // it to print an add/body/loop statement and
                                // enqueue the stream's code to be printed.
                                p.newLine();
                                new SIRInitStatement(/*params*/new LinkedList(), child).accept(this);
                            }
                    }
            }
        p.outdent();
        p.newLine();
        p.print("}");
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
        p.print("(");
        p.print("+");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
        p.print("(");
        p.print("-");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        p.print("(");
        p.print("~");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        p.print("(");
        p.print("!");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
        p.print("(");
        printType(type);
        p.print(")");
    }

    /**
     * prints a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix) {
        // Not a concept we have in StreamIt code.
        // but if dumping sufficiently early after Kopi2SIR
        // you wil encounter it...
        p.print("this");
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
        p.print("super");
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
        p.print("(");
        left.accept(this);
        if (oper == OPE_SL) {
            p.print(" << ");
        } else if (oper == OPE_SR) {
            p.print(" >> ");
        } else {
            p.print(" >>> ");
        }
        right.accept(this);
        p.print(")");
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
        p.print("(");
        left.accept(this);
        switch (oper) {
        case OPE_LT:
            p.print(" < ");
            break;
        case OPE_LE:
            p.print(" <= ");
            break;
        case OPE_GT:
            p.print(" > ");
            break;
        case OPE_GE:
            p.print(" >= ");
            break;
        default:
            assert false : self;
        }
        right.accept(this);
        p.print(")");
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
            p.print("++");
        } else {
            p.print("--");
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
            p.print("++");
        } else {
            p.print("--");
        }
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
        p.print("(");
        expr.accept(this);
        p.print(")");
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
        p.print(".new " + ident + "(");
        visitArgs(params, 0);
        p.print(")");
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
        /* It appears this only occurs in dead cody ("new Complex()",
         * FFT6) and it doesn't make sense in C, so just removing
         * this.

         prefix.accept(this);
         p.print(".new " + ident + "(");
         visitArgs(params, 0);
         p.print(")");
        */
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public void visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
                                                  CClassType type,
                                                  JExpression[] params,
                                                  JClassDeclaration decl)
    {
        /* It appears this only occurs in dead cody ("new Complex()",
         * FFT6) and it doesn't make sense in C, so just removing
         * this.

         p.print("new " + type + "(");
         visitArgs(params, 0);
         p.print(")");
         // decl.genInnerJavaCode(this);
         */
    }

    /**
     * Prints an unqualified instance creation expression.
     */
    public void visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
                                                 CClassType type,
                                                 JExpression[] params)
    {
        p.print("new " + type + "(");
        visitArgs(params, 0);
        p.print(")");
    }

    /**
     * prints an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
        printType(type);
        for (int i = 0; i < dims.length; i++) {
            p.print("[");
            if (dims[i] == null) {
                p.print("/*NO_DIMENSION*/");
            } else 
                dims[i].accept(this);
        }
        p.print("]");
        if (init != null) {
            init.accept(this);
        }
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
            p.print(".");
        }
        p.print(ident);
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
        p.print("(");
        left.accept(this);
        p.print(" ");
        p.print(oper);
        p.print(" ");
        right.accept(this);
        p.print(")");
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
        p.print(ident);
        p.print("(");
        int i = 0;
        visitArgs(args, i);
        p.print(")");
    }

    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
        p.print(ident);
    }

    /**
     * prints an instanceof expression
     */
    public void visitInstanceofExpression(JInstanceofExpression self,
                                          JExpression expr,
                                          CType dest) {
        expr.accept(this);
        p.print(" instanceof ");
        printType(dest);
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
        p.print("(");
        left.accept(this);
        p.print(equal ? " == " : " != ");
        right.accept(this);
        p.print(")");
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
        p.print("(");
        cond.accept(this);
        p.print(" ? ");
        left.accept(this);
        p.print(" : ");
        right.accept(this);
        p.print(")");
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
            p.print(" *= ");
            break;
        case OPE_SLASH:
            p.print(" /= ");
            break;
        case OPE_PERCENT:
            p.print(" %= ");
            break;
        case OPE_PLUS:
            p.print(" += ");
            break;
        case OPE_MINUS:
            p.print(" -= ");
            break;
        case OPE_SL:
            p.print(" <<= ");
            break;
        case OPE_SR:
            p.print(" >>= ");
            break;
        case OPE_BSR:
            p.print(" >>>= ");
            break;
        case OPE_BAND:
            p.print(" &= ");
            break;
        case OPE_BXOR:
            p.print(" ^= ");
            break;
        case OPE_BOR:
            p.print(" |= ");
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
        int     index = ident.indexOf("_$");
        if (index != -1) {
            p.print(ident.substring(0, index));      // local var
        } else {
            if (!(left instanceof JThisExpression))
                {
                    left.accept(this);
                    p.print(".");
                }
            p.print(ident);
        }
    }

    /**
     * prints a class expression
     */
    public void visitClassExpression(JClassExpression self, CType type) {
        printType(type);
        p.print(".class");
    }

    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type)
    {
        p.print("(");
        p.print("(");
        printType(type);
        p.print(")");
        expr.accept(this);
        p.print(")");
    }

    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
        p.print("(");
        p.print("(");
        printType(type);
        p.print(")");
        p.print("(");
        expr.accept(this);
        p.print(")");
        p.print(")");
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        p.print("(");
        left.accept(this);
        switch (oper) {
        case OPE_BAND:
            p.print(" & ");
            break;
        case OPE_BOR:
            p.print(" | ");
            break;
        case OPE_BXOR:
            p.print(" ^ ");
            break;
        default:
            assert false : self;
        }
        right.accept(this);
        p.print(")");
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
        p.print(" = ");
        right.accept(this);
    }

    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
        assert false : "StreamIt doesn't support Java array.length";
        prefix.accept(this);
        p.print(".length");
    }

    /**
     * prints an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        prefix.accept(this);
        p.print("[");
        accessor.accept(this);
        p.print("]");
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
          StringTokenizer   tok = new StringTokenizer(comment.getText(), "\n");

          if (comment.hadSpaceBefore()) {
          p.newLine();
          }

          if (comment.isLineComment()) {
          p.print("//");
          if (tok.hasMoreTokens())
          p.print(tok.nextToken().trim());
          p.println();
          } else {
          if (p.getLine() > 0) {
          if (!nl) {
          p.newLine();
          }
          p.newLine();
          }
          p.print("/*");
          while (tok.hasMoreTokens()){
          String comm = tok.nextToken().trim();
          if (comm.startsWith("*")) {
          comm = comm.substring(1).trim();
          }
          if (tok.hasMoreTokens() || comm.length() > 0) {
          p.newLine();
          p.print(" * " + comm);
          }
          }
          p.newLine();
          p.print(" * /");
          p.newLine();
          }

          if (comment.hadSpaceAfter()) {
          p.newLine();
          }
        */
    }

    /**
     * prints an array length expression
     */
    public void visitJavadoc(JavadocComment comment) {
        StringTokenizer tok = new StringTokenizer(comment.getText(), "\n");
        boolean     isFirst = true;

        if (!nl) {
            p.newLine();
        }
        p.newLine();
        p.print("/**");
        while (tok.hasMoreTokens()) {
            String  text = tok.nextToken().trim();
            String  type = null;
            boolean param = false;
            int idx = text.indexOf("@param");
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
                p.newLine();
                isFirst = false;
                if (param) {
                    text = text.substring(idx + type.length()).trim();
                    idx = Math.min(text.indexOf(" ") == -1 ? Integer.MAX_VALUE : text.indexOf(" "),
                                   text.indexOf("\t") == -1 ? Integer.MAX_VALUE : text.indexOf("\t"));
                    if (idx == Integer.MAX_VALUE) {
                        idx = 0;
                    }
                    String  before = text.substring(0, idx);
                    p.print(" * " + type);
                    p.setIndentation(p.getIndentation() + 12);
                    p.print(before);
                    p.setIndentation(p.getIndentation() + 20);
                    p.print(text.substring(idx).trim());
                    p.setIndentation(p.getIndentation() - 20);
                    p.setIndentation(p.getIndentation() - 12);
                } else {
                    text = text.substring(idx + type.length()).trim();
                    p.print(" * " + type);
                    p.setIndentation(p.getIndentation() + 12);
                    p.print(text);
                    p.setIndentation(p.getIndentation() - 12);
                }
            } else {
                text = text.substring(text.indexOf("*") + 1);
                if (tok.hasMoreTokens() || text.length() > 0) {
                    p.newLine();
                    p.print(" * ");
                    if (!isFirst) p.setIndentation(p.getIndentation() + 32);
                    p.print(text.trim());
                    if (!isFirst) p.setIndentation(p.getIndentation() - 32);
                }
            }
        }
        p.newLine();
        p.print(" */");
    }

    // ----------------------------------------------------------------------
    // STREAMIT IR HANDLERS
    // ----------------------------------------------------------------------

    public void visitCreatePortalExpression(SIRCreatePortal self) {
        p.print("create_portal()");
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
                    p.print("body ");
                else if (stream == fl.getLoop())
                    p.print("loop ");
                else
                    {
                        assert false : stream;
                        p.print("add ");
                    }
            }
        else
            p.print("add ");

        // Is stream anonymous?
        if (stream.getName() == null)
            stream.accept(this);
        else
            {
                // Certain types of builtins have types and fixed names:
                if (stream instanceof SIRIdentity)
                    p.print("Identity<" + stream.getInputType() + ">");
                else
                    p.print(stream.getName());
                // Dump the parameter list, if any.
                List params = self.getArgs();
                if (!(params.isEmpty()))
                    {
                        p.print("(");
                        JExpression[] args =
                            (JExpression[])params.toArray(new JExpression[0]);
                        visitArgs(args, 0);
                        p.print(")");
                    }
                p.print(";");
                streamQueue.add(stream);
            }
    }

    public void visitInterfaceTable(SIRInterfaceTable self)
    {
        String iname = self.getIface().getIdent();
        JMethodDeclaration[] methods = self.getMethods();
        boolean first = true;
        
        p.print("{ ");
        for (int i = 0; i < methods.length; i++)
            {
                if (!first) p.print(", ");
                first = false;
                p.print(iname + "_" + methods[i].getName());
            }
        p.print("}");
    }
    
    public void visitLatency(SIRLatency self)
    {
        p.print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyMax(SIRLatencyMax self)
    {
        p.print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyRange(SIRLatencyRange self)
    {
        p.print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencySet(SIRLatencySet self)
    {
        p.print("LATENCY_BEST_EFFORT");
    }

    public void visitMessageStatement(SIRMessageStatement self,
                                      JExpression portal,
                                      String iname,
                                      String ident,
                                      JExpression[] params,
                                      SIRLatency latency)
    {
        p.print("send_" + iname + "_" + ident + "(");
        portal.accept(this);
        p.print(", ");
        latency.accept(this);
        if (params != null)
            for (int i = 0; i < params.length; i++)
                if (params[i] != null)
                    {
                        p.print(", ");
                        params[i].accept(this);
                    }
        p.print(");");
    }

    public void visitRangeExpression(SIRRangeExpression self) {
        p.print("[");
        self.getMin().accept(this);
        p.print(",");
        self.getAve().accept(this);
        p.print(",");
        self.getMax().accept(this);
        p.print("]");
    }

    public void visitDynamicToken(SIRDynamicToken self) {
        p.print("*");
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
        p.print("peek(");
        num.accept(this);
        p.print(")");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
        if (self.getNumPop()>1) {
            p.print("pop("+self.getNumPop()+")");
        } else {
            p.print("pop()");
        }
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
        p.print(portalNames.get(self).toString());
    }

    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression exp)
    {
        p.print("println(");
        exp.accept(this);
        p.print(");");
    }
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
        p.print("push(");
        val.accept(this);
        p.print(")");
    }
    
    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
                                          JExpression portal,
                                          SIRStream receiver, 
                                          JMethodDeclaration[] methods)
    {
        p.print("register " + receiver.getName() + " on portal " + portal);
        //        assert false : "TODO: implement SIR messaging";
    }
    
    public void visitRegSenderStatement(SIRRegSenderStatement self,
                                        String fn,
                                        SIRLatency latency)
    {
        p.print("visitRegSenderStatement");
        //        assert false : "TODO: implement SIR messaging";
    }

    public void visitMarker(SIRMarker self) {
        if (self instanceof SIRBeginMarker) {
            p.println("// mark begin: " + ((SIRBeginMarker)self).getName());
        }
        if (self instanceof SIREndMarker) {
            p.println("// mark end: " + ((SIREndMarker)self).getName());
        }
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
        p.print("FEEDBACK_DELAY(");
        data.accept(this);
        p.print(", ");
        streamContext.accept(this);
        p.print(", " + delay + ", ");
        printType(type);
        p.print(", ");
        fp.accept(this);
        p.print(");");
    }
    
    /**
     * Visits an encoder registration node.
     */
    public void visitSetEncode(LIRSetEncode self,
                               JExpression streamContext,
                               LIRFunctionPointer fp)
    {
        p.print("set_encode(");
        streamContext.accept(this);
        p.print(", ");
        fp.accept(this);
        p.print(");");
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
        p.print("set_joiner(");
        streamContext.accept(this);
        p.print(", ");
        p.print(type.toString());
        p.print(", " + String.valueOf(ways));
        if (weights != null)
            {
                for (int i = 0; i < weights.length; i++)
                    p.print(", " + String.valueOf(weights[i]));
            }
        p.print(");");
    }

    /**
     * Visits a peek-rate-setting node.
     */
    public void visitSetPeek(LIRSetPeek self,
                             JExpression streamContext,
                             int peek)
    {
        p.print("set_peek(");
        streamContext.accept(this);
        p.print(", " + peek + ");");
    }
    
    /**
     * Visits a pop-rate-setting node.
     */
    public void visitSetPop(LIRSetPop self,
                            JExpression streamContext,
                            int pop)
    {
        p.print("set_pop(");
        streamContext.accept(this);
        p.print(", " + pop + ");");
    }
    
    /**
     * Visits a push-rate-setting node.
     */
    public void visitSetPush(LIRSetPush self,
                             JExpression streamContext,
                             int push)
    {
        p.print("set_push(");
        streamContext.accept(this);
        p.print(", " + push + ");");
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
        p.print("set_splitter(");
        streamContext.accept(this);
        p.print(", " + type + ", " + String.valueOf(ways));
        if (weights != null)
            {
                for (int i = 0; i < weights.length; i++)
                    p.print(", " + String.valueOf(weights[i]));
            }
        p.print(");");
    }

    /**
     * Visits a stream-type-setting node.
     */
    public void visitSetStreamType(LIRSetStreamType self,
                                   JExpression streamContext,
                                   LIRStreamType streamType)
    {
        p.print("set_stream_type(");
        streamContext.accept(this);
        p.print(", " + streamType + ");");
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
                                  List<JStatement> initStatements)
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
            p.print("create_splitjoin_tape(");
            streamContext.accept(this);
            p.print(", SPLITTER, OUTPUT, " + position + ", ");
            childContext.accept(this);
            p.print(", sizeof(");
            printType(inputType);
            p.print("), " + inputSize + ");");
            p.newLine();
        }
        if (outputSize!=0) {
            p.print("create_splitjoin_tape(");
            streamContext.accept(this);
            p.print(", JOINER, INPUT, " + position + ", ");
            childContext.accept(this);
            p.print(", sizeof(");
            printType(outputType);
            p.print("), " + outputSize + ");");
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
        p.newLine();
        if (expr != null) {
            p.print("case ");
            expr.accept(this);
            p.print(": ");
        } else {
            p.print("default: ");
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
        p.indent();
        for (int i = 0; i < stmts.length; i++) {
            p.newLine();
            stmts[i].accept(this);
        }
        p.outdent();
    }

    /**
     * prints an array length expression
     */
    public void visitCatchClause(JCatchClause self,
                                 JFormalParameter exception,
                                 JBlock body) {
        p.print(" catch (");
        exception.accept(this);
        p.print(") ");
        body.accept(this);
    }

    /**
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
        if (value)
            p.print("true");
        else
            p.print("false");
    }

    /**
     * prints a byte literal
     */
    public void visitByteLiteral(byte value) {
        p.print("((byte)" + value + ")");
    }

    /**
     * prints a character literal
     */
    public void visitCharLiteral(char value) {
        switch (value) {
        case '\b':
            p.print("'\\b'");
            break;
        case '\r':
            p.print("'\\r'");
            break;
        case '\t':
            p.print("'\\t'");
            break;
        case '\n':
            p.print("'\\n'");
            break;
        case '\f':
            p.print("'\\f'");
            break;
        case '\\':
            p.print("'\\\\'");
            break;
        case '\'':
            p.print("'\\''");
            break;
        case '\"':
            p.print("'\\\"'");
            break;
        default:
            p.print("'" + value + "'");
        }
    }

    /**
     * prints a double literal
     */
    public void visitDoubleLiteral(double value) {
        System.err.println("Warning: converting double to float when printing StreamIt version of IR.\n" +
                           "         (StreamIt does not yet have syntax for doubles.)");
        p.print((float)value);
    }

    /**
     * prints a float literal
     */
    public void visitFloatLiteral(float value) {
        p.print(value);
    }

    /**
     * prints a int literal
     */
    public void visitIntLiteral(int value) {
        p.print(value);
    }

    /**
     * prints a long literal
     */
    public void visitLongLiteral(long value) {
        p.print("(" + value + "L)");
    }

    /**
     * prints a short literal
     */
    public void visitShortLiteral(short value) {
        p.print("((short)" + value + ")");
    }

    /**
     * prints a string literal
     */
    public void visitStringLiteral(String value) {
        p.print('"' + value + '"');
    }

    /**
     * prints a null literal
     */
    public void visitNullLiteral() {
        assert false;
        p.print("null");
    }

    /**
     * prints an array length expression
     */
    public void visitPackageName(String name) {
        assert false;
        // p.print("package " + name + ";");
        // p.newLine();
    }

    /**
     * prints an array length expression
     */
    public void visitPackageImport(String name) {
        assert false;
        // p.print("import " + name.replace('/', '.') + ".*;");
    }

    /**
     * prints an array length expression
     */
    public void visitClassImport(String name) {
        assert false;
        // p.print("import " + name.replace('/', '.') + ";");
    }

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) {
        printType(type);
        if (ident.indexOf("$") == -1) {
            p.print(" ");
            p.print(ident);
        }
    }

    /**
     * prints an array length expression
     */
    public void visitArgs(JExpression[] args, int base) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i + base != 0) {
                    p.print(", ");
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
        p.newLine();
        p.print(functorIsThis ? "this" : "super");
        p.print("(");
        visitArgs(params, 0);
        p.print(");");
    }

    /**
     * prints an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {
        p.newLine();
        p.print("{");
        for (int i = 0; i < elems.length; i++) {
            if (i != 0) {
                p.print(", ");
            }
            elems[i].accept(this);
        }
        p.print("}");
    }

    
    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------

    // Special case for CTypes, to map some Java types to C types.
    protected void printType(CType s)
    {
        if (s instanceof CArrayType)
            {
                printType(((CArrayType)s).getBaseType());
                JExpression[] dims = ((CArrayType)s).getDims();
                if (dims != null)
                    for (int i = 0; i < dims.length; i++)
                        {
                            p.print("[");
                            dims[i].accept(this);
                            p.print("]");
                        }
                /*      else
                        {
                        p.print("[");
                        // I suspect this isn't entirely what we want.
                        // In fact, it looks like it prints the number
                        // of array bounds, which is frequently "1".
                        // Eit.
                        p.print(((CArrayType)s).getArrayBound());
                        p.print("]");
                        }
                */
            }
        else if (s.toString().equals("Complex"))
            p.print("complex"); // revert to primitive type
        else if (s.toString().endsWith("Portal"))
            {
                // Rewrite this to be a Portal<foo> type.
                String name = s.toString();
                p.print("Portal<" + name.substring(0, name.length() - 6) + ">");
            }
        else
            p.print(s.toString());
    }

}
