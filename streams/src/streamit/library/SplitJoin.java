package streamit;

// creates a split/join
public class SplitJoin extends Stream {

    public SplitJoin() {
	super();
    }

    public SplitJoin(int n) {
	super(n);
    }

    // specify the splitter
    public Stream splitter(Splitter s) {
	return null;
    }

    // specify the joiner
    public Stream joiner(Joiner s) {
	return null;
    }

    // add a stream to the parallel section between the splitter and the joiner
    public Stream add(Stream s) {
	return null;
    }

}
