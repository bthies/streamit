package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.sir.lowering.*;

/**
 * This class is for building sample representations in the SIR.
 */
public class SIRBuilder {

    public static void main(String args[]) {
        // work on hello6
        SIRStream hello6 = buildHello6();
        Flattener.run(hello6, null, null, null, null, null);
    }

    /**
     * Builds the SIR representation of HelloWorld6.java in the library.
     */
    public static SIRStream buildHello6() {

        SIRPipeline toplevel = new SIRPipeline(null,
                                               "HelloWorld6",
                                               /* fields  */ 
                                               JFieldDeclaration.EMPTY(),
                                               /* methods */ 
                                               JMethodDeclaration.EMPTY());
        /* build filter 1 */

        JVariableDefinition x = 
            new JVariableDefinition(/* tokenref    */ null, 
                                    /* modifiers   */ at.dms.kjc.
                                    Constants.ACC_PUBLIC,
                                    /* type        */ CStdType.Integer,
                                    /* identifier  */ "x",
                                    /* initializer */ null);

        JFieldDeclaration[] fields1 = 
            { new JFieldDeclaration(/* tokenref */ null, 
                                    /* variable */ x, 
                                    /* javadoc  */ null, 
                                    /* comments */ null) };

        JStatement[] emptybody1 = new JStatement[0];
        JBlock emptyblock1 = new JBlock(null, emptybody1, null);

        JStatement[] emptybody2 = new JStatement[0];
        JBlock emptyblock2 = new JBlock(null, emptybody2, null);

        JStatement[] work1body = { 
            new JExpressionStatement(
                                     null,
                                     new SIRPushExpression(/* arg */ 
                                                           new JPostfixExpression ( 
                                                                                   /* tokref */ null,
                                                                                   Constants.OPE_POSTINC,
                                                                                   /* contents */ 
                                                                                   new JFieldAccessExpression(null,
                                                                                                              new JThisExpression(null, (CClass)null),
                                                                                                              "x"))),
                                     null) };
     
        JBlock work1block = new JBlock(/* tokref   */ null,
                                       /* body     */ work1body,
                                       /* comments */ null);

        JMethodDeclaration[] meth1 = 
            {/* init */
                new JMethodDeclaration( /* tokref     */ null,
                                        /* modifiers  */ at.dms.kjc.
                                        Constants.ACC_PUBLIC,
                                        /* returntype */ CStdType.Void,
                                        /* identifier */ "init",
                                        /* parameters */ JFormalParameter.EMPTY,
                                        /* exceptions */ CClassType.EMPTY,
                                        /* body       */ emptyblock1,
                                        /* javadoc    */ null,
                                        /* comments   */ null),
                /* work*/ new JMethodDeclaration( /* tokref     */ null,
                                                  /* modifiers  */ at.dms.kjc.
                                                  Constants.ACC_PUBLIC,
                                                  /* returntype */ CStdType.Void,
                                                  /* identifier */ "work",
                                                  /* parameters */ JFormalParameter.EMPTY,
                                                  /* exceptions */ CClassType.EMPTY,
                                                  /* body       */ work1block,
                                                  /* javadoc    */ null,
                                                  /* comments   */ null)};

        CType type1 = CStdType.Integer;

        SIRFilter f1 = new SIRFilter(toplevel,
                                     "filter name",
                                     /* fields */ fields1,
                                     /* methods */ meth1,
                                     /* peek, pop, push */ 
                                     new JIntLiteral(0), 
                                     new JIntLiteral(0), 
                                     new JIntLiteral(1),
                                     /* work */ meth1[1],
                                     /* i/o type */ type1, type1);
        f1.setInit(meth1[0]);

        /* build filter 2 */

        JStatement[] work2body = { 
            new SIRPrintStatement(
                                  null,
                                  new SIRPopExpression(),
                                  true,
                                  null) };
    
        JBlock work2block = new JBlock(/* tokref   */ null,
                                       /* body     */ work2body,
                                       /* comments */ null);

        JMethodDeclaration[] meth2 = 
            {/* init */
                new JMethodDeclaration( /* tokref     */ null,
                                        /* modifiers  */ at.dms.kjc.
                                        Constants.ACC_PUBLIC,
                                        /* returntype */ CStdType.Void,
                                        /* identifier */ "init",
                                        /* parameters */ JFormalParameter.EMPTY,
                                        /* exceptions */ CClassType.EMPTY,
                                        /* body       */ emptyblock2,
                                        /* javadoc    */ null,
                                        /* comments   */ null),
                /* work*/
                new JMethodDeclaration( /* tokref     */ null,
                                        /* modifiers  */ at.dms.kjc.
                                        Constants.ACC_PUBLIC,
                                        /* returntype */ CStdType.Void,
                                        /* identifier */ "work",
                                        /* parameters */ JFormalParameter.EMPTY,
                                        /* exceptions */ CClassType.EMPTY,
                                        /* body       */ work2block,
                                        /* javadoc    */ null,
                                        /* comments   */ null)};

        CType type2 = CStdType.Integer;

        SIRFilter f2 = new SIRFilter(toplevel,
                                     "filter name",
                                     /* fields */ JFieldDeclaration.EMPTY(),
                                     /* methods */ meth2,
                                     /* peek, pop, push */ 
                                     new JIntLiteral(1), 
                                     new JIntLiteral(1), 
                                     new JIntLiteral(0),
                                     /* work */ meth2[1],
                                     /* i/o type */ type2, type2);
    

        f2.setInit(meth2[0]);

        /* build pipeline and add filters */

        toplevel.add(f1);
        toplevel.add(f2);

        /* set init function to initialize components */
        JStatement[] initStatements = 
            { new SIRInitStatement(f1),
              new SIRInitStatement(f2)};
        toplevel.setInit(new JMethodDeclaration( /* tokref     */ null,
                                                 /* modifiers  */ at.dms.kjc.
                                                 Constants.ACC_PUBLIC,
                                                 /* returntype */ CStdType.Void,
                                                 /* identifier */ "init",
                                                 /* parameters */ JFormalParameter.EMPTY,
                                                 /* exceptions */ CClassType.EMPTY,
                                                 /* body       */ new JBlock(null,
                                                                             initStatements,
                                                                             null),
                                                 /* javadoc    */ null,
                                                 /* comments   */ null));

        /* return toplevel */
        return toplevel;
    }
}
