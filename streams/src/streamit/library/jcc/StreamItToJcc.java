package streamit.library.jcc;

import java.util.ArrayList;
import java.util.List;
import jcc.lang.AbortAgent;
import jcc.lang.Agent;
import jcc.lang.Atom;
import jcc.lang.BasicAgent;
import jcc.lang.Teller;
import jcc.lang.Vat;
import streamit.library.Channel;
import streamit.library.FeedbackLoop;
import streamit.library.Filter;
import streamit.library.Pipeline;
import streamit.library.Rate;
import streamit.library.SplitJoin;
import streamit.library.Splitter;
import streamit.library.Stream;

/**
 * This class provides methods to run a StreamIt library program to using the
 * JCC library.
 */
public class StreamItToJcc {

	public static final int INFINITE = -1;

	protected List<JccOperator> sourceList;

	protected Thread workThread = null;

	// List of all sinks
	protected List<JccFilter> sinkList;

	// Number of sinks that have not executed for the required number of
	// iterations
	static int waitingSinkCount;

	// Source operator
	static JccOperator source;

	public StreamItToJcc() {
		sourceList = new ArrayList<JccOperator>();
		sinkList = new ArrayList<JccFilter>();
	}

	protected JccStream convertStream(Stream stream) {
		if (stream instanceof Filter) {
			return convertFilter((Filter) stream);
		} else if (stream instanceof Pipeline) {
			return convertPipeline((Pipeline) stream);
		} else if (stream instanceof SplitJoin) {
			return convertSplitJoin((SplitJoin) stream);
		} else if (stream instanceof FeedbackLoop) {
			return convertFeedbackLoop((FeedbackLoop) stream);
		} else {
			throw new RuntimeException("Unsupported stream class: "
					+ stream.getClass().getName());
		}
	}

	/**
	 * Creates a JCC library filter object (JccFilter or JccDynamicRateFilter)
	 * from a Filter.
	 */
	protected JccFilter convertFilter(Filter filter) {
		// Only 1 init phase is supported
		assert filter.getNumInitPhases() <= 1;

		boolean hasPrework = (filter.getNumInitPhases() > 0);
		JccFilter jccFilter;

		if (filter.inputChannel == null) {
			// Filter is source
			jccFilter = new JccFilter(filter, 0, hasPrework, 0);
			sourceList.add(jccFilter);
		} else {
			int preworkPeekRate = (hasPrework ? filter.getInitPeekStage(0) : 0);
			int peekRate = getFilterPeekRate(filter);

			if ((preworkPeekRate == Rate.DYNAMIC_RATE)
					|| (peekRate == Rate.DYNAMIC_RATE)) {
				// Work and/or prework function has dynamic peek rate, create
				// dynamic rate filter
				System.out.println(filter.toString()
						+ ": using dynamic rate filter");
				jccFilter = new JccDynamicRateFilter(filter, hasPrework);
			} else {
				// Create normal filter
				jccFilter = new JccFilter(filter, peekRate, hasPrework,
						preworkPeekRate);

				if (filter.outputChannel == null) {
					sinkList.add(jccFilter);
				}
			}
		}

		return jccFilter;
	}

	/**
	 * Creates a JccPipeline from a Pipeline.
	 */
	protected JccPipeline convertPipeline(Pipeline pipeline) {
		int filterCount = pipeline.getNumChildren();
		JccStream[] jccFilters = new JccStream[filterCount];

		// Create first filter
		jccFilters[0] = convertStream(pipeline.getChild(0));

		for (int i = 1; i < filterCount; i++) {
			// Create next filter
			Stream filter = pipeline.getChild(i);
			jccFilters[i] = convertStream(filter);

			// Create input channel for the new filter and connect it to the
			// last one
			assert filter.inputChannel != null;
			JccChannel jccChannel = convertChannel(filter.inputChannel);
			jccFilters[i - 1].setOutChannel(jccChannel);
			jccFilters[i].setInChannel(jccChannel);
		}

		return new JccPipeline(pipeline, jccFilters);
	}

	/**
	 * Creates a JccSplitJoin from a SplitJoin.
	 */
	protected JccSplitJoin convertSplitJoin(SplitJoin splitJoin) {
		Splitter splitter = splitJoin.getSplitter();
		JccSplitter jccSplitter;

		// Create splitter (with weights). Output channels are connected later.
		if (splitter.duplicateSplitter) {
			jccSplitter = new JccSplitter.DuplicateSplitter();
		} else {
			JccSplitter.RoundRobinSplitter jccRRSplitter = new JccSplitter.RoundRobinSplitter();
			int[] splitterWeights = splitter.getWeights();

			for (int i = 0; i < splitterWeights.length; i++) {
				jccRRSplitter.addWeight(splitterWeights[i]);
			}

			jccSplitter = jccRRSplitter;
		}

		// Create joiner with. Input channels are connected later.
		JccJoiner jccJoiner = new JccJoiner();
		int[] joinerWeights = splitJoin.getJoiner().getWeights();

		for (int i = 0; i < joinerWeights.length; i++) {
			jccJoiner.addWeight(joinerWeights[i]);
		}

		int filterCount = splitJoin.getNumChildren();
		JccStream[] jccFilters = new JccStream[filterCount];

		// Create child filters
		for (int i = 0; i < filterCount; i++) {
			// Create filter
			Stream filter = splitJoin.getChild(i);
			jccFilters[i] = convertStream(filter);

			// Create input channel for filter and attach it to splitter
			assert filter.inputChannel != null;
			JccChannel inChannel = convertChannel(filter.inputChannel);
			jccSplitter.addOutChannel(inChannel);
			jccFilters[i].setInChannel(inChannel);

			// Create output channel for filter and attach it to joiner
			if (filter.outputChannel == null) {
				jccJoiner.addInChannel(null);
			} else {
				JccChannel outChannel = convertChannel(filter.outputChannel);
				jccFilters[i].setOutChannel(outChannel);
				jccJoiner.addInChannel(outChannel);
			}
		}

		return new JccSplitJoin(splitJoin, jccSplitter, jccJoiner, jccFilters);
	}

	/**
	 * Creates a JccFeedbackLoop from a FeedbackLoop.
	 */
	protected JccFeedbackLoop convertFeedbackLoop(FeedbackLoop feedbackLoop) {
		// Create joiner. Joiner's input 0 is temporarily set to null until the
		// feedbackloop's input is set.
		JccJoiner jccJoiner = new JccJoiner();
		jccJoiner.addInChannel(null);

		int[] joinerWeights = feedbackLoop.getJoiner().getWeights();
		assert joinerWeights.length == 2;
		jccJoiner.addWeight(joinerWeights[0]);
		jccJoiner.addWeight(joinerWeights[1]);

		if (jccJoiner.isSource()) {
			sourceList.add(jccJoiner);
		}

		// Create splitter. Splitter's output 0 is null until the feedbackloop's
		// output is set.
		Splitter splitter = feedbackLoop.getSplitter();
		JccSplitter jccSplitter;

		if (splitter.duplicateSplitter) {
			jccSplitter = new JccSplitter.DuplicateSplitter();
		} else {
			JccSplitter.RoundRobinSplitter jccRRSplitter = new JccSplitter.RoundRobinSplitter();
			int[] splitterWeights = splitter.getWeights();
			assert splitterWeights.length == 2;
			jccRRSplitter.addWeight(splitterWeights[0]);
			jccRRSplitter.addWeight(splitterWeights[1]);
			jccSplitter = jccRRSplitter;
		}

		jccSplitter.addOutChannel(null);

		// Create body and loop filters and associated channels
		JccChannel jccChannel;

		Stream body = feedbackLoop.getBody();
		JccStream jccBody = convertStream(body);

		assert body.inputChannel != null;
		jccChannel = convertChannel(body.inputChannel);
		jccJoiner.setOutChannel(jccChannel);
		jccBody.setInChannel(jccChannel);

		assert body.outputChannel != null;
		jccChannel = convertChannel(body.outputChannel);
		jccBody.setOutChannel(jccChannel);
		jccSplitter.setInChannel(jccChannel);

		Stream loop = feedbackLoop.getLoop();
		JccStream jccLoop = convertStream(loop);

		assert loop.inputChannel != null;
		jccChannel = convertChannel(loop.inputChannel);
		jccSplitter.addOutChannel(jccChannel);
		jccLoop.setInChannel(jccChannel);

		assert loop.outputChannel != null;
		jccChannel = convertChannel(loop.outputChannel);
		jccLoop.setOutChannel(jccChannel);
		jccJoiner.addInChannel(jccChannel);

		return new JccFeedbackLoop(feedbackLoop, jccJoiner, jccSplitter,
				jccBody, jccLoop);
	}

	/**
	 * Creates a JccChannel from a Channel. Any items enqueued to the channel
	 * (e.g., in feedbackloops) are pushed onto the JccChannel.
	 */
	protected JccChannel convertChannel(Channel channel) {
		Class type = channel.getType();
		assert (type == Integer.TYPE) || (type == Float.TYPE);
		JccChannel result = new JccChannel(type);

		int enqueuedItems = channel.getItemsPushed();

		for (int i = 0; i < enqueuedItems; i++) {
			if (type == Integer.TYPE) {
				result.pushInt(channel.peekInt(i));
			} else if (type == Float.TYPE) {
				result.pushFloat(channel.peekFloat(i));
			} else {
				// Unreachable
				assert false;
			}
		}

		return result;
	}

	/**
	 * Returns the peek rate of a filter. Returns DYNAMIC_RATE if the filter has
	 * more than one phase or a dynamic peek rate.
	 */
	protected int getFilterPeekRate(Filter filter) {
		if (filter.getNumSteadyPhases() > 1) {
			System.out.println(filter.toString() + ": is phased filter");
			return Rate.DYNAMIC_RATE;
		}

		int peekRate = filter.getSteadyPeekPhase(0);
		assert peekRate != 0;

		return peekRate;
	}

	/**
	 * Runs the program for the specified number of iterations. This should
	 * produce the same behavior as -library -nosched as long as sinks aren't
	 * sources or dynamic rate filters. This (and all convert{class} methods)
	 * must be called from the JCC vat thread.
	 */
	protected void run(int iterations) {
		if (sourceList.size() != 1) {
			throw new RuntimeException("Expected exactly 1 void-> filter, got "
					+ sourceList.size());
		}

		source = sourceList.get(0);
		sourceList = null;

		long startTime = System.nanoTime();

		if (iterations == INFINITE) {
			while (true) {
				source.work();
			}
		} else {
			waitingSinkCount = sinkList.size();

			for (int i = 0; i < waitingSinkCount; i++) {
				sinkList.get(i).runCount = iterations;
			}

			while (waitingSinkCount > 0) {
				source.work();
			}
		}

		long time = System.nanoTime() - startTime;

		System.out.println("Execution time (ms): " + (time / 1000000L));
	}

	/**
	 * Converts a StreamIt library program to use the JCC library and runs it
	 * for the specified number of iterations. All init() methods in the program
	 * should be run before this method is called.
	 * 
	 * @param program
	 *            The highest-level stream that represents the program.
	 */
	public void convertAndRun(final Stream program, final int iterations) {
		// Create JCC vat to do the conversion and execution in
		Teller teller = Vat.makeVat(new Vat.BasicInitCall() {

			public Agent getAgent() {
				return new BasicAgent() {

					public void now() {
						// Report the thread object
						synchronized (StreamItToJcc.this) {
							workThread = Thread.currentThread();
							StreamItToJcc.this.notify();
						}

						convertStream(program).init();
						run(iterations);
					}

					public Agent next() {
						// Return an agent that aborts on the next tick so the
						// vat can terminate
						return new AbortAgent();
					}

				};
			}

		});

		// Send 2 messages to the vat. First one starts the conversion and
		// computation, second one causes the vat to abort.
		try {
			teller.equate(Atom.NIL);
			teller.equate(Atom.NIL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Wait for vat thread to report its object
		synchronized (this) {
			while (workThread == null) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
		}

		// Wait for vat thread to terminate
		while (workThread.isAlive()) {
			try {
				workThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

}
