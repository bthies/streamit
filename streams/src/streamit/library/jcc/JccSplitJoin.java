package streamit.library.jcc;

import streamit.library.SplitJoin;
import streamit.library.Stream;

public class JccSplitJoin extends JccCompositeFilter {

	protected final SplitJoin splitJoin;

	protected final JccSplitter splitter;

	protected final JccJoiner joiner;

	protected final JccStream[] filters;

	JccSplitJoin(SplitJoin splitJoin, JccSplitter splitter, JccJoiner joiner,
			JccStream[] filters) {
		this.splitJoin = splitJoin;
		this.splitter = splitter;
		this.joiner = joiner;
		this.filters = (JccStream[]) filters.clone();
	}

	Stream getStreamIt() {
		return splitJoin;
	}

	void setInChannel(JccChannel channel) {
		splitter.setInChannel(channel);
	}

	void setOutChannel(JccChannel channel) {
		joiner.setOutChannel(channel);
	}

	public void init() {
		splitter.init();
		joiner.init();

		for (int i = 0; i < filters.length; i++) {
			filters[i].init();
		}
	}

}
