package streamit.library.jcc;

import java.util.ArrayList;
import java.util.List;
import jcc.lang.Promise;
import x10.stream.InStream;
import x10.stream.OutStream;

/**
 * Base class for splitters. Null splitters are not supported.
 */
abstract class JccSplitter extends JccOperator {

	protected JccChannel inChannel;

	protected List<JccChannel> outChannelList;

	protected JccChannel[] outChannels;

	protected OutStream[] outStreams;

	JccSplitter() {
		outChannelList = new ArrayList<JccChannel>();
	}

	void setInChannel(JccChannel channel) {
		inChannel = channel;
		super.setInStream(inChannel.getSinkEnd());
	}

	void addOutChannel(JccChannel channel) {
		outChannelList.add(channel);
	}

	void setOutChannel(int index, JccChannel channel) {
		assert index < outChannelList.size();
		outChannelList.set(index, channel);
	}

	public void init() {
		outChannels = outChannelList.toArray(new JccChannel[0]);
		outStreams = new OutStream[outChannels.length];

		for (int i = 0; i < outChannels.length; i++) {
			outStreams[i] = outChannels[i].getSourceEnd();
		}

		outChannelList = null;

		super.init();
	}

	public final void setInStream(InStream in) {
		throw new UnsupportedOperationException();
	}

	public final void setOutStream(OutStream out) {
		throw new UnsupportedOperationException();
	}

	/**
	 * This class implements a duplicate splitter.
	 */
	static class DuplicateSplitter extends JccSplitter {

		public void work() {
			Promise item = pop();

			for (int i = 0; i < outStreams.length; i++) {
				outStreams[i].push(item);
			}
		}

	}

	/**
	 * This class implements a weighted round-robin splitter. All input/output
	 * channels must be non-null. All output weights must be non-zero.
	 */
	static class RoundRobinSplitter extends JccSplitter {

		protected List<Integer> weightList;

		protected int[] weights;

		// Index of the current output channel
		protected int outputIndex = -1;

		// JCC output stream of the current output channel
		protected OutStream outputStream;

		// Number of items to write to the current output channel before
		// switching to the next one
		protected int outputItems = 0;

		RoundRobinSplitter() {
			weightList = new ArrayList<Integer>();
		}

		void addWeight(int weight) {
			if (weight == 0) {
				throw new UnsupportedOperationException(
						"Round-robin splitter has 0 weight");
			}

			weightList.add(Integer.valueOf(weight));
		}

		public void init() {
			assert weightList.size() == outChannelList.size();

			weights = new int[weightList.size()];

			for (int i = 0; i < weights.length; i++) {
				weights[i] = weightList.get(i).intValue();
			}

			weightList = null;

			super.init();
		}

		public void work() {
			if (outputItems == 0) {
				// Next output channel
				outputIndex = (outputIndex + 1) % outStreams.length;
				outputStream = outStreams[outputIndex];
				outputItems = weights[outputIndex];
			}

			outputStream.push(pop());
			outputItems--;
		}

	}

}
