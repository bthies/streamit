// $Header
package at.dms.kjc.cluster;

import at.dms.kjc.sir.*;
import at.dms.kjc.CType;
import at.dms.kjc.common.CommonUtils;

class ClusterUtils {

    /** 
     * Return the the name of a work function given a filter and a unique id.
    *<br/>
    * If not a predefined filter then the work method should have a useful
    * unique name. 
    *
     */
    //  If a predefined filter, the work method may be called
    // "UNINITIALIZED DUMMY METHOD" (A Kopi2Sir bug?) so give it a reasonable
    // name.  (The raw backend does not seem to have this problem, but I 
    // don't know exactly how they handle pre-defined filters.)

    public static String getWorkName(SIROperator f, int id) {
        if (f instanceof SIRPredefinedFilter) {
            return f.getName() + "__work__" + id;
        } else if (f instanceof SIRFilter) {
            return ((SIRFilter)f).getWork().getName() + "__" + id;
        } else if (f instanceof SIRSplitter) {
            return "__splitter_" + id + "_work";
        } else {
            assert f instanceof SIRJoiner;
            return "__joiner_"  + id + "_work";
        }
    }
    
    /**
     * Convert a {@see at.dms.kjc.CType} into a string.
     *
     * Use method from CommonUtils to perform conversion.
     * This layer just tells the method in CommonUtils to
     * use 'bool' rather than 'int' for translation of Java 'boolean'
     * 
     * {@use at.dms.kjc.common.CommonUtils#CTypeToString}
     *
     * @param s    a CType 
     * @return     String translation of CType
     */
    public static String CTypeToString(CType s) {
        return CommonUtils.CTypeToString(s, true);
    }
 
    /**
     * Print a declaration for the given type with the given
     * identifier.  Prints int x[10][10] for arrays.
     *
     * Use method from CommonUtils to perform conversion.
     * This layer just tells the method in CommonUtils to
     * use 'bool' rather than 'int' for translation of Java 'boolean'
     * 
     * {@use at.dms.kjc.common.CommonUtils#declToString}
     *
     * @param s      a CType
     * @param ident  identifier for declaration
     * @return       String translation of declaration
     */
    public static String declToString(CType s, String ident) {
        return CommonUtils.declToString(s, ident, true);
    }

    /**
     * Use this to get name of peek function
     *
     * push, peek, and pop functions are used in several different classes
     * when generating code.  Centralize the naming of these functions here.
     *
     * @param selfID   a unique identifier
     * @return         name for push function based on the passed identifier.
     */
    public static String peekName(int selfID) {
        return "__peek__"+selfID;
    }

 
    /**
     * Use this to get name of push function
     *
     * push, peek, and pop functions are used in several different classes
     * when generating code.  Centralize the naming of these functions here.
     *
     * @param selfID   a unique identifier
     * @return         name for pop function based on the passed identifier.
     */
    public static String pushName(int selfID) {
        return "__push__"+selfID;
    }

    /**
     * Use this to get name of pop function
     *
     * push and pop functions are used in several different classes
     * when generating code.  Centralize the naming of these functions here.
     *
     * @param selfID   a unique identifier
     * @return         name for pop function based on the passed identifier.
     */
    public static String popName(int selfID) {
        return "__pop__"+selfID;
    }
}
