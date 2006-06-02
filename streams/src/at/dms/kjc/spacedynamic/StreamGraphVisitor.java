package at.dms.kjc.spacedynamic;

/** 
 * Visitor interface for static stream sub-graphs of a stream graph 
 **/
public interface StreamGraphVisitor 
{
    public void visitStaticStreamGraph(SpdStaticStreamGraph ssg);
}

