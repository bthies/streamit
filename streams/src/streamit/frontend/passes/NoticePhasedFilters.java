/*
 * NoticePhasedFilters.java: convert filters to phased ones where appropriate
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: NoticePhasedFilters.java,v 1.2 2003-01-09 22:47:33 dmaze Exp $
 */

package streamit.frontend.passes;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.List;

/**
 * Front-end visitor pass that replaces StreamSpecs corresponding to
 * filters with StreamSpecs corresponding to phased filters, but only
 * if the work function has no declared I/O rates.
 */
public class NoticePhasedFilters extends FEReplacer
{
    public Object visitStreamSpec(StreamSpec spec)
    {
        if (spec.getType() != StreamSpec.STREAM_FILTER)
            return spec;

        // Find the work function.  There should be exactly one;
        // count on StreamSpec to DTRT.
        FuncWork fw = spec.getWorkFunc();
        
        if (fw.getPeekRate() == null &&
            fw.getPopRate() == null &&
            fw.getPushRate() == null)
        {
            // Check...we have a phased filter now.
            StreamSpec newSpec =
                new StreamSpec(spec.getContext(),
                               StreamSpec.STREAM_PHASEDFILTER,
                               spec.getStreamType(),
                               spec.getName(),
                               spec.getParams(),
                               spec.getVars(),
                               spec.getFuncs());
            return newSpec;
        }

        return spec;
    }
}
