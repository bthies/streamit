package streamit.misc;

/* $Id: HashComperator.java,v 1.2 2003-05-15 21:21:11 karczma Exp $ */

public class HashComperator extends AssertedClass implements Comperator
{
	public boolean isLess(Object left, Object right)
	{
		int leftHash = left.hashCode ();
		int rightHash = right.hashCode ();
		
		ASSERT (left == right || leftHash != rightHash);
		return left.hashCode () < right.hashCode ();
	}
}