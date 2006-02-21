package streamit.library.jcc;

import streamit.library.Stream;
import x10.stream.InStream;
import x10.stream.OutStream;

/**
 * Base class for all single-input/single-output stream objects (analogous to
 * Stream).
 */
public abstract class JccStream extends JccOperator {

	JccStream() {
	}

	/**
	 * Returns the StreamIt library object that is wrapped by this object.
	 */
	abstract Stream getStreamIt();

	/**
	 * Sets the stream's input channel. This implementation just sets the JCC
	 * input stream to the one that belongs to the channel. Is overridden in all
	 * subclasses.
	 */
	void setInChannel(JccChannel channel) {
		super.setInStream(channel.getSinkEnd());
	}

	void setOutChannel(JccChannel channel) {
		super.setOutStream(channel.getSourceEnd());
	}

	public final void setInStream(InStream in) {
		throw new UnsupportedOperationException();
	}

	public final void setOutStream(OutStream out) {
		throw new UnsupportedOperationException();
	}

}
