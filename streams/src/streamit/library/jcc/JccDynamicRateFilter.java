package streamit.library.jcc;

import streamit.library.Filter;

/**
 * This class implements a filter with a dynamic peek rate. 
 * 
 * The implementation was based on the old (removed from CVS)
 * PhasedFilter implementation. The real work function is repeatedly
 * run in a separate thread which blocks whenever more input data is
 * needed. This essentially tries to implement a pull model on top of
 * JCC's push model, which may not be a good idea.
 * 
 * Because JCC expects everything to be run in the same vat thread, the worker
 * thread blocks whenever a data item is output. The vat thread picks up the
 * item and does the push. This costs 2 thread switches for every new item
 * input/output.
 */
public class JccDynamicRateFilter extends JccFilter {

	protected boolean firstRun = true;

	// Synchronization variable - set to true when the worker thread should be
	// running, false when the JCC vat thread should be running
	boolean working = false;

	JccDynamicRateFilter(Filter filter, boolean hasPrework) {
		super(filter, hasPrework);
	}

	void setInChannel(JccChannel channel) {
		super.setInChannel(channel);
		inChannel.setDynamicRateSink(this);
	}

	void setOutChannel(JccChannel channel) {
		super.setOutChannel(channel);
		outChannel.setDynamicRateSource(this);
	}

	public void init() {
		// Dynamic rate filters are never sources
		assert inChannel != null;
		super.init();
	}

	public void work() {
		inChannel.updatePeekList();

		synchronized (this) {
			while (true) {
				// Tell worker thread it's okay to resume
				working = true;

				if (firstRun) {
					// Create and start worker thread
					firstRun = false;

					Thread worker = new Thread(new Runnable() {

						public void run() {
							// Run the prework function if one exists and then
							// repeatedly run the work function. The blocking is
							// implemented by JccChannel.
							synchronized (JccDynamicRateFilter.this) {
								if (runPrework) {
									filter.prework();
								}

								while (true) {
									filter.work();
								}
							}
						}

					});

					worker.start();
				} else {
					// Resume the worker thread
					notify();
				}

				while (working) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				// The worker thread may have blocked because it 1) needs more
				// input or 2) just did a push. If it just did a push, do a JCC
				// push and resume the worker thread.
				if (!outChannel.processPush()) {
					break;
				}
			}
		}
	}

}
