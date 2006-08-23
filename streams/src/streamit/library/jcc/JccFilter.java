package streamit.library.jcc;

import streamit.library.Filter;
import streamit.library.Stream;

public class JccFilter extends JccStream {

	protected final Filter filter;

	protected final int peekRate;

	protected int itemsNeeded;

	protected boolean runPrework = false;

	protected JccChannel inChannel = null;

	protected JccChannel outChannel = null;

	protected int lastSourceRunCount = 0;

	/**
	 * Creates a JCC filter that wraps around a StreamIt library
	 * Filter. The filter must have work/prework functions that
	 * have static peek rates.
	 * 
	 * @param peekRate
	 *            The peek rate of the filter's work function.
	 * @param hasPrework
	 *            Whether the filter has a prework function. If false,
	 *            preworkPeekRate is ignored.
	 * @param preworkPeekRate
	 *            The peek rate of the filter's prework function.
	 */
	JccFilter(Filter filter, int peekRate, boolean hasPrework,
			int preworkPeekRate) {
		this.filter = filter;
		this.peekRate = peekRate;
		itemsNeeded = (hasPrework ? preworkPeekRate : this.peekRate);
		runPrework = hasPrework;
	}

	/**
	 * Constructor provided for JccDynamicRateFilter.
	 */
	protected JccFilter(Filter filter, boolean hasPrework) {
		this.filter = filter;
		this.peekRate = 0;
		runPrework = hasPrework;
	}

	Stream getStreamIt() {
		return filter;
	}

	void setInChannel(JccChannel channel) {
		inChannel = channel;
		super.setInChannel(inChannel);

		filter.inputChannel = inChannel;
	}

	void setOutChannel(JccChannel channel) {
		outChannel = channel;
		super.setOutChannel(outChannel);

		filter.outputChannel = outChannel;
	}

	public void init() {
		// If not a source, start the JCC model. If a source, do nothing.
		if (inChannel != null) {
			run();
		}
	}

	/**
	 * This work function is called whenever an item becomes available on the
	 * input stream. It pops the item into a private queue and runs the actual
	 * work function if the peek rate can be satisfied.
	 */
	public void work() {
		if (peekRate == 0) {
			// Filter is source, run actual work/prework function once
			runCount++;

			if (runPrework) {
				filter.prework();
				runPrework = false;
			} else {
				filter.work();
			}
		} else {
			// Pop 1 item off the input
			inChannel.updatePeekList();
			itemsNeeded--;

			if (itemsNeeded <= 0) {
				// assert inChannel.peekListSize() == peekRate;

				int peekListSize;

				// Run the actual work/prework function until it needs more
				// input
				do {
					if (runPrework) {
						filter.prework();
						runPrework = false;
					} else {
						filter.work();
					}

					// For sinks, do iterations bookkeeping to avoid using
					// scheduling info
					if ((runCount > 0)
							&& (StreamItToJcc.source.runCount > lastSourceRunCount)) {
						runCount--;
						lastSourceRunCount = StreamItToJcc.source.runCount;

						if (runCount == 0) {
							StreamItToJcc.waitingSinkCount--;
						}
					}

					peekListSize = inChannel.peekListSize();
				} while (peekListSize >= peekRate);

				itemsNeeded = peekRate - peekListSize;
			}
		}
	}

	boolean isSource() {
		return (peekRate == 0);
	}

}
