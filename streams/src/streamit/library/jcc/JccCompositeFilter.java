package streamit.library.jcc;

/**
 * Base class for all composite filters (pipelines, splitjoins, feedbackloops).
 */
public abstract class JccCompositeFilter extends JccStream {

	JccCompositeFilter() {
	}

	public final void run() {
		throw new UnsupportedOperationException(
				"Cannot call run() on composite filter");
	}

	public final void work() {
		throw new UnsupportedOperationException(
				"Cannot call work() on composite filter");
	}

}
