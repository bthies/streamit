package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import java.util.HashMap;
import java.util.*;

/**
 * This class represents a data that is available to all filters.
 */

public class SIRGlobal extends SIRStream
{
    public SIRGlobal()
    {
        super();
    }

    public void setWork(JMethodDeclaration newWork)
    {
        at.dms.util.Utils.fail(ident + ": attempt to add a work function to a Global");
    }
    public int getPushForSchedule(HashMap[] counts)
    {
        at.dms.util.Utils.fail(ident + ": attempt to call getPushForSchedule for Global");
        return -1;
    }
    public int getPopForSchedule(HashMap[] counts)
    {
        at.dms.util.Utils.fail(ident + ": attempt to call getPopForSchedule for Global");
        return -1;
    }

    /* Things that we need to implement: */
    public CType getOutputType() { return null; }
    public LIRStreamType getStreamType() { return null; } // (implement?)
    public CType getInputType() { return null; }
    public boolean needsInit() { return true; }
    public boolean needsWork() { return false; }

    public Object accept(AttributeStreamVisitor v)
    {
        at.dms.util.Utils.fail(ident + ": SIRGlobal does not accept AttributeStreamVisitor");
        return null;
        //return v.visitGlobal(this, methods);
    }
}


