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

package streamit.frontend.nodes;

/**
 * A message-portal type.  This is a type used to send messages; it
 * appears in the portals list of
 * <code>streamit.frontend.nodes.StreamCreator</code>, and as the
 * receiver object of
 * <code>streamit.frontend.nodes.StmtSendMessage</code>.  A portal
 * corresponds to a single named stream type.  Because of the way
 * <code>FEReplacer</code> works, attempting to keep a pointer to
 * the actual referenced stream type here is useless; we only keep
 * the name.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypePortal.java,v 1.3 2003-10-09 19:51:00 dmaze Exp $
 */
public class TypePortal extends Type
{
    private String name;
    
    /**
     * Creates a new portal type.
     *
     * @param name  name of the target filter type
     */
    public TypePortal(String name)
    {
        this.name = name;
    }
    
    /**
     * Gets the name of the target filter type.  Other code will be
     * needed to resolve this to the actual <code>StreamSpec</code>
     * object.
     *
     * @return  name of the target filter type
     */
    public String getName()
    {
        return name;
    }
}
