package streamit.library.jcc;

import streamit.library.FeedbackLoop;
import streamit.library.Stream;

public class JccFeedbackLoop extends JccCompositeFilter {

	protected final FeedbackLoop feedbackLoop;

	protected final JccJoiner joiner;

	protected final JccSplitter splitter;

	protected final JccStream body;

	protected final JccStream loop;

	JccFeedbackLoop(FeedbackLoop feedbackLoop, JccJoiner joiner,
			JccSplitter splitter, JccStream body, JccStream loop) {
		this.feedbackLoop = feedbackLoop;
		this.joiner = joiner;
		this.splitter = splitter;
		this.body = body;
		this.loop = loop;
	}

	Stream getStreamIt() {
		return this.feedbackLoop;
	}

	void setInChannel(JccChannel channel) {
		// Input 0 of the joiner is used as the feedbackloop's input
		joiner.setInChannel(0, channel);
	}

	void setOutChannel(JccChannel channel) {
		// Output 0 of the splitter is used as the feedbackloop's output
		splitter.setOutChannel(0, channel);
	}

	public void init() {
		joiner.init();
		splitter.init();
		body.init();
		loop.init();
	}

}
