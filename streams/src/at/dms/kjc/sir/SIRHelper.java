package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import java.util.HashMap;
import java.util.*;

/**
 * This class represents a set of helper methods available to all filters.
 */

public class SIRHelper extends SIRStream
{
    private boolean _native;
    public SIRHelper(boolean _native)
    {
        super();
        this._native = _native;
    }

    public boolean isNative() { return _native; }

    public void setWork(JMethodDeclaration newWork)
    {
        at.dms.util.Utils.fail(ident + ": attempt to add a work function to a Helper");
    }
    public void setInit(JMethodDeclaration newInit)
    {
        at.dms.util.Utils.fail(ident + ": attempt to add an init function to a Helper");
    }
    public void setInitWithoutReplacement(JMethodDeclaration newInit)
    {
        at.dms.util.Utils.fail(ident + ": attempt to add an init function to a Helper");
    }
    public int getPushForSchedule(HashMap[] counts)
    {
        at.dms.util.Utils.fail(ident + ": attempt to call getPushForSchedule for Helper");
        return -1;
    }
    public int getPopForSchedule(HashMap[] counts)
    {
        at.dms.util.Utils.fail(ident + ": attempt to call getPopForSchedule for Helper");
        return -1;
    }

    /* Things that we need to implement: */
    public CType getOutputType() { return null; }
    public LIRStreamType getStreamType() { return null; } // (implement?)
    public CType getInputType() { return null; }
    public boolean needsInit() { return false; }
    public boolean needsWork() { return false; }

    //public void setIdent(java.lang.String name) {
    //  System.out.println("Warning: Refuse to rename Helper!");
    //}

    public Object accept(AttributeStreamVisitor v)
    {
        at.dms.util.Utils.fail(ident + ": SIRHelper does not accept AttributeStreamVisitor");
        return null;
        //return v.visitHelper(this, methods);
    }

}
