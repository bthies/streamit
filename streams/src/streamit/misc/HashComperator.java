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

package streamit.misc;

/* $Id: HashComperator.java,v 1.8 2005-02-28 21:57:28 janiss Exp $ */

public class HashComperator implements Comperator
{
	public boolean isLess(Object left, Object right)
	{
		int leftHash = left.hashCode ();
		int rightHash = right.hashCode ();

		/*
		 * this assertion causes SDEP calculation to fail
		 *
		 *assert left == right || leftHash != rightHash : 
		 *  "Hash error on Objects " + left + " and " + right + " of types + " + 
		 *  (left==null ? "null" : ""+left.getClass()) + " (hash=" + leftHash + ") and " + 
		 *  (right==null ? "null" : ""+right.getClass()) + " (hash=" + rightHash + ")";
		 */

		return left.hashCode () < right.hashCode ();
	}
}
