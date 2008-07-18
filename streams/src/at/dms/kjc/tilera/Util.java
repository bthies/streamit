package at.dms.kjc.tilera;

import at.dms.kjc.*;

/**
 * Some utility functions
 * 
 * @author mgordon
 *
 */
public class Util {
    public static JStatement toStmt(String txt) {
        return new JExpressionStatement(new JEmittedTextExpression(txt));
    }
    
}
