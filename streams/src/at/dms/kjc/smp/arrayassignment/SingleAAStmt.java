package at.dms.kjc.smp.arrayassignment;

import at.dms.kjc.JStatement;
import at.dms.kjc.smp.Util;

public class SingleAAStmt implements AAStatement, Comparable<SingleAAStmt>{
    String dstBufName;
    String srcBufName;
    int dstIndex;
    int srcIndex;
    String dstOffsetName;
    String srcOffsetName;

    public SingleAAStmt(String dstBufName, String dstOffsetName, int dstIndex, 
            String srcBufName, String srcOffsetName, int srcIndex) {
        this.dstBufName = dstBufName;
        if (dstOffsetName == null)
            dstOffsetName = "";
        this.dstOffsetName = dstOffsetName;
        this.dstIndex = dstIndex;

        this.srcBufName = srcBufName;
        if (srcOffsetName == null)
            srcOffsetName = "";
        this.srcOffsetName = srcOffsetName;

        this.srcIndex = srcIndex;
    }

    /**
     * Convert to a JStatement of a JEmitedTxtExpression
     */
    public JStatement toJStmt() {
        String dstOffset = dstOffsetName.equals("") ? "" : dstOffsetName + " + ";
        String srcOffset = srcOffsetName.equals("") ? "" : srcOffsetName + " + ";

        return Util.toStmt(dstBufName + "[" + dstOffset + dstIndex + "] = " + 
                srcBufName + "[" + srcOffset + srcIndex + "]"); 
    }
    
    public int compareTo(SingleAAStmt other) {
        if (!dstBufName.equals(other.dstBufName)) 
            return dstBufName.compareTo(other.dstBufName);
        else if (!dstOffsetName.equals(other.dstOffsetName))
            return dstOffsetName.compareTo(other.dstOffsetName);
        else if (dstIndex != other.dstIndex)
            return new Integer(dstIndex).compareTo(other.dstIndex);
        else if (!srcBufName.equals(other.srcBufName))
            return srcBufName.compareTo(other.srcBufName);
        else if (!srcOffsetName.equals(other.srcOffsetName))
            return srcOffsetName.compareTo(other.srcOffsetName);
        
        return new Integer(srcIndex).compareTo(other.srcIndex); 
    }
}
