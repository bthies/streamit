package streamit.frontend.passes;
import streamit.frontend.nodes.*;
import java.util.*;

/**
 * Pass to assign loop types to feedback loops with void input types.
 * {@link streamit.frontend.nodes.StreamType} contains a field for the
 * loop type of a feedback loop, which is the output type of the loop
 * stream, and the type that the <code>enqueue</code> statement uses.
 * This pass looks for feedback loops with void input types, and tries
 * to get the output type of the loop stream.  If this fails, probably
 * because the body and loop streams are both anonymous streams that
 * don't declare I/O types, the program visitor returns null.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: AssignLoopTypes.java,v 1.2 2003-09-03 18:05:47 dmaze Exp $
 */
public class AssignLoopTypes extends FEReplacer
{
    // Map of stream names to stream types
    private Map toplevels;
    // true if there has been a failure
    private boolean hasFailed;
    // Detected loop and body stream type; used by nested visitor classes
    private StreamType theLoop, theBody;
    
    public Object visitProgram(Program prog)
    {
        // init:
        toplevels = new HashMap();
        hasFailed = false;
        
        // build the map of stream types from toplevel named streams
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
        {
            StreamSpec ss = (StreamSpec)iter.next();
            toplevels.put(ss.getName(), ss.getStreamType());
        }

        // recurse
        Object result = super.visitProgram(prog);
        
        // return result if we succeeded, null otherwise
        if (hasFailed)
            return null;
        return result;
    }

    public Object visitStreamSpec(StreamSpec ss)
    {
        // Recurse.
        ss = (StreamSpec)super.visitStreamSpec(ss);
        
        // Is it a feedback loop?
        if (ss.getType() != StreamSpec.STREAM_FEEDBACKLOOP)
            return ss;
        StreamType st = ss.getStreamType();
        if (st != null)
        {
            Type loopType = st.getLoop();
            // Do we think the loop type is void?
            if (!(loopType instanceof TypePrimitive) ||
                ((TypePrimitive)loopType).getType() != TypePrimitive.TYPE_VOID)
                return ss;
        }
        else
        {
            // Untyped anonymous loop.  Let st be a completely
            // random guess, but definitely go on with the detection.
            st = new StreamType(ss.getContext(),
                                new TypePrimitive(TypePrimitive.TYPE_FLOAT),
                                new TypePrimitive(TypePrimitive.TYPE_FLOAT));
        }
        
        // So now we're looking for the loop stream.  Work under the
        // assumption that all loop statements and all body statements
        // have the same stream type.
        theLoop = null;
        theBody = null;
        ss.accept(new FEReplacer() {
                public Object visitStmtBody(StmtBody stmt)
                {
                    noticeChildStream(stmt.getCreator(), false);
                    return stmt; // not recursing
                }
                public Object visitStmtLoop(StmtLoop stmt)
                {
                    noticeChildStream(stmt.getCreator(), true);
                    return stmt; // not recursing
                }
            });
        
        // See what body and loop types we found, if any.
        Type enqType;
        if (theLoop != null)
            enqType = theLoop.getOut();
        else if (theBody != null)
            enqType = theBody.getIn();
        else
        {
            System.err.println(ss.getContext() +
                               ": could not determine enqueue type");
            hasFailed = true;
            return ss;
        }

        // Build the new stream type and stream spec and return that.
        st = new StreamType(st.getContext(), st.getIn(), st.getOut(),
                            enqType);
        ss = new StreamSpec(ss.getContext(), ss.getType(), st,
                            ss.getName(), ss.getParams(), ss.getVars(),
                            ss.getFuncs());
        return ss;
    }

    private void noticeChildStream(StreamCreator sc, boolean isLoop)
    {
        // This is either a simple or anonymous creator.
        StreamType st = null;
        if (sc instanceof SCSimple)
        {
            SCSimple scs = (SCSimple)sc;
            st = (StreamType)toplevels.get(scs.getName());
            // If we didn't find that, is this a parameterized stream?
            // If so, assume it's T->T.
            if (st == null)
            {
                List types = scs.getTypes();
                if (!types.isEmpty())
                    st = new StreamType(sc.getContext(),
                                        (Type)types.get(0),
                                        (Type)types.get(0));
            }
        }
        else if (sc instanceof SCAnon)
        {
            StreamSpec ss = ((SCAnon)sc).getSpec();
            st = ss.getStreamType();
        }
        // If st at this point is null, we failed to get useful data.
        // Punt now rather than clobber anything.
        if (st == null)
            return;
        // Otherwise set the body or loop stream type.
        if (isLoop)
            theLoop = st;
        else
            theBody = st;
    }
}
