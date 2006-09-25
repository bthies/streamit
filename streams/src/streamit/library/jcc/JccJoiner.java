package streamit.library.jcc;

import java.util.ArrayList;
import java.util.List;
import jcc.lang.Promise;
import x10.stream.InStream;
import x10.stream.OutStream;

/**
 * This class implements a generic weighted joiner. The first input channel may
 * be null, in which case the joiner is treated as a source. The output channel
 * may be null. Any weight may be 0.
 */
class JccJoiner extends JccOperator {

	protected List<JccChannel> inChannelList;

	protected JccChannel[] inChannels;

	protected InStream[] inStreams;

	protected JccChannel outChannel = null;

	protected List<Integer> weightList;

	protected int[] weights;

	// Index of the current input channel
	protected int inputIndex;

	// JCC input stream of the current input channel
	protected InStream inputStream;

	// Number of items to be read from the current input channel before moving
	// to the next
	protected int inputItems;

	protected boolean noInput = true;

	JccJoiner() {
		inChannelList = new ArrayList<JccChannel>();
		weightList = new ArrayList<Integer>();
	}

	void addInChannel(JccChannel channel) {
		inChannelList.add(channel);
	}

	void setInChannel(int index, JccChannel channel) {
		assert index < inChannelList.size();
		inChannelList.set(index, channel);
	}

	void setOutChannel(JccChannel channel) {
		outChannel = channel;
		super.setOutStream(outChannel.getSourceEnd());
	}

	void addWeight(int weight) {
		weightList.add(Integer.valueOf(weight));

		if (weight != 0) {
			noInput = false;
		}
	}

	public void init() {
		assert inChannelList.size() == weightList.size();

		weights = new int[weightList.size()];

		for (int i = 0; i < weights.length; i++) {
			weights[i] = weightList.get(i).intValue();
		}

		weightList = null;

		inChannels = inChannelList.toArray(new JccChannel[0]);
		inStreams = new InStream[inChannels.length];

		for (int i = 0; i < inChannels.length; i++) {
			if (inChannels[i] == null) {
				assert weights[i] == 0;
			} else {
				assert weights[i] != 0;
				inStreams[i] = inChannels[i].getSinkEnd();
			}
		}

		inChannelList = null;

		if (noInput) {
			return;
		}

		// Find the first non-zero-weight input channel
		inputIndex = -1;

		do {
			inputIndex = (inputIndex + 1) % inStreams.length;
			inputItems = weights[inputIndex];
		} while (inputItems == 0);

		inputStream = inStreams[inputIndex];

		if (!isSource()) {
			run();
		}
	}

	/**
	 * Overrides standard run() method to hang on the input stream of the
	 * current input channel.
	 */
	public void run() {
		inputStream.runWhenReady(this);
	}

	public void work() {
		// Increment run count in case this is a source
		runCount++;

		Promise item = inputStream.pop();

		if (outChannel != null) {
			push(item);
		}

		inputItems--;

		if (inputItems == 0) {
			// Find the next input channel. This can be greatly improved but
			// probably won't occur often enough to matter.
			do {
				inputIndex = (inputIndex + 1) % inStreams.length;
				inputItems = weights[inputIndex];
			} while (inputItems == 0);

			inputStream = inStreams[inputIndex];
		}
	}

	public boolean closed() {
		return inputStream.closed();
	}

	boolean isSource() {
		if (noInput) {
			return false;
		}

		if (weightList == null) {
			return (weights[0] == 0);
		} else {
			return (weightList.get(0).intValue() == 0);
		}
	}

	public final void setInStream(InStream in) {
		throw new UnsupportedOperationException();
	}

	public final void setOutStream(OutStream out) {
		throw new UnsupportedOperationException();
	}

}
