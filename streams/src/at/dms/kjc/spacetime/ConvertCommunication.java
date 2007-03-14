package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.backendSupport.FilterInfo;
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
 * It uses a circular buffer from which to read the values.
 * 
 * @author mgordon
 *
 */
public class ConvertCommunication extends SLIRReplacingVisitor 
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
     * Create a new object that will convert filterInfo's input communication
     * using the variables generateds.
     * 
     * @param generateds The compiler generated vars.
     * @param fitlerInfo The filter.
     */
    public ConvertCommunication(GeneratedVariables generateds,
                                FilterInfo fitlerInfo) 
    {
        generatedVariables = generateds;
        this.filterInfo = filterInfo;
    }
    
    /**
     * Visit a pop expressions convert to the form:
     * (recvBuffer[++recvBufferIndex % recvBufferSize])
     * 
     * Where the variables are defined in 
     * {@link ConvertCommunication#generatedVariables}.
     * 
     * @return the new expression as a buffer access.
     */
    public Object visitPopExpression(SIRPopExpression self,
                                     CType tapeType) {
      
        //create the increment of the index var
        JPrefixExpression bufferIncrement = 
            new JPrefixExpression(null, 
                                  OPE_PREINC,
                                  new JFieldAccessExpression
                                  (null, new JThisExpression(null), 
                                   generatedVariables.recvBufferIndex.getIdent()));
        
        JBitwiseExpression indexAnd = 
            new JBitwiseExpression(null, 
                                   OPE_BAND,
                                   bufferIncrement, 
                                   new JFieldAccessExpression
                                   (null, new JThisExpression(null),     
                                    generatedVariables.recvBufferBits.getIdent()));
        /*
        //create the modulo expression
        JModuloExpression indexMod = 
        new JModuloExpression(null, bufferIncrement, 
        new JFieldAccessExpression
        (null,
        localVariables.recvBufferSize));
        */
        
        //create the array access expression
        JArrayAccessExpression bufferAccess = 
            new JArrayAccessExpression(null,
                                       new JFieldAccessExpression
                                       (null, new JThisExpression(null),
                                        generatedVariables.recvBuffer.getIdent()),
                                       indexAnd);

        //return the parenthesed expression
        return new JParenthesedExpression(null,
                                          bufferAccess);
    }
    
    /**
     * Convert peek exps into:
     * (recvBuffer[(recvBufferIndex + (arg) + 1) mod recvBufferSize])
     * 
     * Where the variables are defined in 
     * {@link ConvertCommunication#generatedVariables}.
     * 
     * @return The converted expression that will use a buffer access.
     */
    public Object visitPeekExpression(SIRPeekExpression oldSelf,
                                      CType oldTapeType,
                                      JExpression oldArg) {
        // do the super
        SIRPeekExpression self = 
            (SIRPeekExpression)
            super.visitPeekExpression(oldSelf, oldTapeType, oldArg);
        

        //create the index calculation expression
        JAddExpression argIncrement = 
            new JAddExpression(null, self.getArg(), new JIntLiteral(1));
        JAddExpression index = 
            new JAddExpression(null,
                               new JFieldAccessExpression
                               (null, new JThisExpression(null),
                                generatedVariables.recvBufferIndex.getIdent()),
                               argIncrement);
        
        JBitwiseExpression indexAnd = 
            new JBitwiseExpression(null, 
                                   OPE_BAND,
                                   index, 
                                   new JFieldAccessExpression
                                   (null, new JThisExpression(null),
                                    generatedVariables.recvBufferBits.getIdent()));
     
        /*
        //create the mod expression
        JModuloExpression indexMod = 
        new JModuloExpression(null, index,
        new JFieldAccessExpression
        (null,
        localVariables.recvBufferSize));
        */

        //create the array access expression
        JArrayAccessExpression bufferAccess = 
            new JArrayAccessExpression(null,
                                       new JFieldAccessExpression
                                       (null, new JThisExpression(null),
                                        generatedVariables.recvBuffer.getIdent()),
                                       indexAnd);

        //return the parenthesed expression
        return new JParenthesedExpression(null,
                                          bufferAccess);
    }
    
}
