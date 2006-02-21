package streamit.library.jcc;

import jcc.lang.Promise;
import streamit.library.Channel;
import x10.stream.InStream;
import x10.stream.OutStream;
import x10.stream.OutStream_c;

/**
 * This class implements a StreamIt channel in the JCC model. It subclasses
 * Channel so Stream objects can use this instead of their original ones.
 */
public class JccChannel extends Channel {

	protected final OutStream sourceEnd;

	protected final InStream sinkEnd;

	protected JccDynamicRateFilter dynamicRateSource = null;

	protected JccDynamicRateFilter dynamicRateSink = null;

	// The last item pushed by a dynamic rate filter's worker thread
	protected Promise pushedItem = null;

	JccChannel(Class channelType) {
		super(channelType);

		if ((channelType != Integer.TYPE) && (channelType != Float.TYPE)) {
			throw new UnsupportedOperationException(
					"JCC channels currently only support int and float types");
		}

		// Create a JCC input/output stream
		OutStream_c jccStream = new OutStream_c();
		sourceEnd = jccStream;
		sinkEnd = jccStream.inStream();
	}

	OutStream getSourceEnd() {
		return sourceEnd;
	}

	InStream getSinkEnd() {
		return sinkEnd;
	}

	/**
	 * Pops one item from the JCC input stream and enqueues it in a private
	 * queue. This method is only called from the JCC vat thread.
	 */
	void updatePeekList() {
		if (type == Integer.TYPE) {
			wgqueue_int
					.enqueue(((jcc.lang.Integer) sinkEnd.pop().dereference())
							.intValue());
		} else if (type == Float.TYPE) {
			wgqueue_float
					.enqueue(((jcc.lang.Float) sinkEnd.pop().dereference())
							.floatValue());
		} else {
			throw unsupportedType();
		}
	}

	int peekListSize() {
		if (type == Integer.TYPE) {
			return wgqueue_int.size();
		} else if (type == Float.TYPE) {
			return wgqueue_float.size();
		} else {
			throw unsupportedType();
		}
	}

	protected RuntimeException unsupportedType() {
		return new IllegalStateException("JCC channel has unsupported type: "
				+ type.getName());
	}

	public int peekInt(int index) {
		assert type == Integer.TYPE;
		ensurePeek(index);
		return wgqueue_int.elem(index);
	}

	public float peekFloat(int index) {
		assert type == Float.TYPE;
		ensurePeek(index);
		return wgqueue_float.elem(index);
	}

	public int popInt() {
		assert type == Integer.TYPE;
		ensurePeek(0);
		return wgqueue_int.dequeue();
	}

	public float popFloat() {
		assert type == Float.TYPE;
		ensurePeek(0);
		return wgqueue_float.dequeue();
	}

	public void pushInt(int i) {
		assert type == Integer.TYPE;

		if (dynamicRateSource == null) {
			// Normal filter - we are executing in the JCC vat thread, do a
			// regular push
			sourceEnd.push(new jcc.lang.Integer(i));
		} else {
			// Dynamic rate filter - we are executing the source filter in the
			// worker thread. Save the pushed item and block. It is safe to
			// create a realized Promise here.
			pushedItem = new jcc.lang.Integer(i);
			stopWork(dynamicRateSource);
		}
	}

	public void pushFloat(float d) {
		assert type == Float.TYPE;

		if (dynamicRateSource == null) {
			sourceEnd.push(new jcc.lang.Float(d));
		} else {
			pushedItem = new jcc.lang.Float(d);
			stopWork(dynamicRateSource);
		}
	}

	/**
	 * Sets the filter at the channel's source when it is dynamic rate (setting
	 * to null means non-dynamic-rate).
	 */
	void setDynamicRateSource(JccDynamicRateFilter sourceFilter) {
		dynamicRateSource = sourceFilter;
	}

	void setDynamicRateSink(JccDynamicRateFilter sinkFilter) {
		dynamicRateSink = sinkFilter;
	}

	/**
	 * This method takes a queued push done in a worker thread and does it in
	 * the JCC vat thread. This method is only called when the filter at the
	 * channel's source is dynamic rate, from the JCC vat thread. Returns true
	 * if an item was just pushed by the worker, false if the worker blocked for
	 * some other reason.
	 */
	boolean processPush() {
		if (pushedItem != null) {
			sourceEnd.push(pushedItem);
			pushedItem = null;
			return true;
		} else {
			return false;
		}
	}

	protected void ensurePeek(int itemIndex) {
		if (dynamicRateSink == null) {
			// Normal filter
			if (myqueue.size() <= itemIndex) {
				throw new RuntimeException("Tried to peek/pop at index "
						+ itemIndex + " when " + myqueue.size()
						+ " item(s) are available");
			}
		} else {
			// Dynamic rate filter - block if data is needed
			while (myqueue.size() <= itemIndex) {
				stopWork(dynamicRateSink);
			}
		}
	}

	/**
	 * Stops execution of a dynamic rate filter's worker thread. This method is
	 * only called in that thread while the specified filter's monitor is held.
	 */
	protected void stopWork(JccDynamicRateFilter filter) {
		// Tell the JCC vat thread we are done
		filter.working = false;
		filter.notify();

		while (!filter.working) {
			try {
				filter.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
