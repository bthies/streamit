package streamit.library.jcc;

import jcc.lang.Promise;

/**
 * Base class for all non-channel stream objects (analogous to Operator).
 */
abstract class JccOperator extends x10.stream.Filter {

	// General-purpose run counter. This is mainly used by filters but joiners
	// may use it as well.
	int runCount = 0;

	/**
	 * Returns whether the operator is a source (no input). Is overridden by
	 * JccFilter and JccJoiner (the only 2 operators that can be sources).
	 */
	boolean isSource() {
		return false;
	}

	public final void test(Promise[] p) {
		throw new UnsupportedOperationException();
	}

}
