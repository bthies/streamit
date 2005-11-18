package at.dms.kjc.cluster;

import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRPredefinedFilter;
import at.dms.kjc.CType;
import at.dms.kjc.common.CommonUtils;

class ClusterUtils {

    // the the name of a work function given a filter and a unique id.
    // 
    // If not a predefined filter then the work method should have a useful
    // unique name. 
    //
    //  If a predefined filter, the work method may be called
    // "UNINITIALIZED DUMMY METHOD" (A Kopi2Sir bug?) so give it a reasonable
    // name.  (The raw backend does not seem to have this problem, but I 
    // don't know exactly how they handle pre-defined filters.)

    public static String getWorkName(SIRFilter f, int id) {
		if (f instanceof SIRPredefinedFilter) {
			return f.getName() + "__work__" + id;
		} else {
			return f.getWork().getName() + "__" + id;
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
    
}
