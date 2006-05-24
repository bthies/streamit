package at.dms.kjc.flatgraph;

/** 
 * Visitor interface for static stream sub-graphs of a stream graph. 
 **/
public interface StreamGraphVisitor 
{
    public void visitStaticStreamGraph(StaticStreamGraph ssg);
}

