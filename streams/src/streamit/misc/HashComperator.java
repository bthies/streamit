package streamit.misc;

/* $Id: HashComperator.java,v 1.1 2003-03-13 23:18:06 karczma Exp $ */

public class HashComperator extends AssertedClass implements Comperator
{
	public boolean isLess(Object left, Object right)
	{
		int leftHash = left.hashCode ();
		int rightHash = right.hashCode ();
		
		ASSERT (left != right || leftHash != rightHash);
		return left.hashCode () < right.hashCode ();
	}
}