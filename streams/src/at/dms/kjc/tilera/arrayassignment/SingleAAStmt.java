package at.dms.kjc.tilera.arrayassignment;

import at.dms.kjc.JStatement;
import at.dms.kjc.tilera.Util;

public class SingleAAStmt implements AAStatement{
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
}
