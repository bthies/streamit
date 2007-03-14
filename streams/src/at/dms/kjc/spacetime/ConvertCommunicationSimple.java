package at.dms.kjc.spacetime;

import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;

import java.util.Hashtable;
import java.math.BigInteger;

/**
 * 
 * This class will convert peek and pop statements into reads from a
 * buffer (an array).  This class is used by 
 * {@link at.dms.kjc.spacetime.BufferedCommunication} to convert 
 * the peek's and pop's.   
 * 
 * This class will only work for filters that are deemed 
 * simple by {@link at.dms.kjc.backendSupport.FilterInfo#isSimple()}.
 * 
 * It uses a simple linear buffer from which to read it values.
 * 
 * @author mgordon
 *
 */
public class ConvertCommunicationSimple extends SLIRReplacingVisitor 
{
    /** The generated var that were created by BufferedCommunication
     * and will be used in the conversion.
     */
    GeneratedVariables generatedVariables;
    /**
     * The filter whose input communication we are converting.
     */
    FilterInfo filterInfo;
    
    /**
     * Create a new converter that will is read to convert peek and pop
     * statements for filterInfo using the compiler generated variables 
     * generateds.
     * 
     * @param generateds the compiler generated variables to use.
     * @param filterInfo The filter to convert.
     */
    public ConvertCommunicationSimple(GeneratedVariables generateds,
                                      FilterInfo filterInfo)  
    {
        generatedVariables = generateds;
        this.filterInfo = filterInfo;
    }
    
    /**
     * Convert pop expressions convert to the form
     * (recvBuffer[++simpleIndex]).
     * 
     * @return The converted buffer expression for this pop.
     */
    public Object visitPopExpression(SIRPopExpression oldSelf,
                                     CType oldTapeType) {
      
        // do the super
        SIRPopExpression self = 
            (SIRPopExpression)
            super.visitPopExpression(oldSelf, oldTapeType);

        //create the increment of the index var
        JPrefixExpression increment = 
            new JPrefixExpression(null, 
                                  OPE_PREINC,
                                  new JLocalVariableExpression
                                  (null,generatedVariables.simpleIndex));
        
        //create the array access expression
        JArrayAccessExpression bufferAccess = 
            new JArrayAccessExpression(null,
                                       new JFieldAccessExpression
                                       (null, new JThisExpression(null),
                                        generatedVariables.recvBuffer.getIdent()),
                                       increment);

        //return the parenthesed expression
        return new JParenthesedExpression(null,
                                          bufferAccess);
    }
    
    /** 
     * Convert peek exps into:
     * (recvBuffer[(simplendex + (arg) + 1)])
     *
     * @return the converted buffer expression for this peek. 
     */
    public Object visitPeekExpression(SIRPeekExpression oldSelf,
                                      CType oldTapeType,
                                      JExpression oldArg) {
        // do the super
        SIRPeekExpression self = 
            (SIRPeekExpression)
            super.visitPeekExpression(oldSelf, oldTapeType, oldArg);


        JAddExpression argIncrement = 
            new JAddExpression(null, self.getArg(), new JIntLiteral(1));
        
        JAddExpression index = 
            new JAddExpression(null,
                               new JLocalVariableExpression
                               (null, 
                                generatedVariables.simpleIndex),
                               argIncrement);

        //create the array access expression
        JArrayAccessExpression bufferAccess = 
            new JArrayAccessExpression(null,
                                       new JFieldAccessExpression
                                       (null, new JThisExpression(null), 
                                        generatedVariables.recvBuffer.getIdent()),
                                       index);

        //return the parenthesed expression
        return new JParenthesedExpression(null,
                                          bufferAccess);
        
    }
    
}
    
