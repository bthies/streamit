/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter.Iterator;

/**
 * streamit.scheduler2.constrained.StreamInteraface is an interface for 
 * constrained scheduler. All implementors of this interface assume that
 * no other scheduler objects have been used.
 */

public interface StreamInterface
    extends streamit.scheduler2.hierarchical.StreamInterface
{
    public LatencyNode getBottomLatencyNode ();
    public LatencyNode getTopLatencyNode ();
    
    public void initiateConstrained ();
}
