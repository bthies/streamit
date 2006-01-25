package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * This represents a StreamIT filter that just pushes 1 for int type and 1.0f for float.
 */
public class SIRDummySource extends SIRPredefinedFilter implements Cloneable, Constants {

    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    private SIRDummySource() {
        super();
    }

    public SIRDummySource(CType type) 
    {
        super(null,
              "DummySource",
              /* fields */ JFieldDeclaration.EMPTY(), 
              /* methods */ JMethodDeclaration.EMPTY(),
              new JIntLiteral(0), new JIntLiteral(0), new JIntLiteral(1),
              /* input type */ CStdType.Void,
              /* output type */ type);
        setup(type);
    }
    
    /**
     * Set the input type and output type to t
     * also sets the work function and init function
     */
    private void setup(CType t) {
        assert t.isFloatingPoint() || t.isOrdinal() :
            "Constructing a SIRDummySource with a non-scalar type.";
    
        this.setPeek(new JIntLiteral(0));
        this.setPop(new JIntLiteral(0));
        this.setPush(new JIntLiteral(1));
    
        this.setInputType(CStdType.Void);
        this.setOutputType(t);
    
        // work function
        JStatement work1body[] = new JStatement[1];
        work1body[0] =  
            new JExpressionStatement
            (null, new SIRPushExpression
             (t.isFloatingPoint() ? (JExpression)new JIntLiteral(1) : (JExpression)new JFloatLiteral(1), t),
             null);

    
        JBlock work1block = new JBlock(/* tokref   */ null,
                                       /* body     */ work1body,
                                       /* comments */ null);    
    
        JMethodDeclaration workfn =  new JMethodDeclaration( /* tokref     */ null,
                                                             /* modifiers  */ at.dms.kjc.
                                                             Constants.ACC_PUBLIC,
                                                             /* returntype */ CStdType.Void,
                                                             /* identifier */ "work",
                                                             /* parameters */ JFormalParameter.EMPTY,
                                                             /* exceptions */ CClassType.EMPTY,
                                                             /* body       */ work1block,
                                                             /* javadoc    */ null,
                                                             /* comments   */ null);
        setWork(workfn);

        // init function
        JBlock initblock = new JBlock(/* tokref   */ null,
                                      /* body     */ new JStatement[0],
                                      /* comments */ null);
        setInit(SIRStream.makeEmptyInit());
    }
    
}


