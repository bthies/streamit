package at.dms.kjc.spacedynamic;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;

/** 
 * Visitor interface for static stream sub-graphs of a stream graph 
 **/
public interface StreamGraphVisitor 
{
    public void visitStaticStreamGraph(StaticStreamGraph ssg);
}

