package at.dms.kjc.spacetime;

import at.dms.kjc.*;

/** Class to hold the variables we generate during raw execution code
 * generated, it is passed around to various classes 
 */
public class GeneratedVariables
{
    /** Variable is peek buffer */
    JVariableDefinition recvBuffer;
    /** Variable seems to be obsolete: eclipse shows single write to this */
    JVariableDefinition recvBufferSize;
    /** Variable is mask for modulo access to peek buffer */
    JVariableDefinition recvBufferBits;
    /** Variable is index into peek buffer */
    JVariableDefinition recvBufferIndex;
    /** Variable is index of end of peek buffer */
    JVariableDefinition recvIndex;
    /** Variable is loop index variable */
    JVariableDefinition exeIndex;
    /** Variable is another loop index variable */
    JVariableDefinition exeIndex1;
    /** Variable is indices for copying (multidimensional) arrays */
    JVariableDefinition[] ARRAY_INDEX;
    /** Variable is unused? */
    JVariableDefinition[] ARRAY_COPY;
    /** Variable is index in peek buffer for ConvertCommunicationSimple */
    JVariableDefinition simpleIndex;
    /** obsolete?? */
    @Deprecated
    JVariableDefinition sendBufferIndex;
    /** obsolete?? */
    @Deprecated
    JVariableDefinition sendBuffer;
}
