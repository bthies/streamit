import streamit.library.*;
class TestSource extends Filter{
    int i;
    public void init() {
	output = new Channel(Float.TYPE, 1);
	i=0;
    }
    public void work() {
        output.pushFloat(i++);
	// pseudo-random input
	if (i==10000) { i=0; }
    }
}
	    
class NullSink extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 1);
    }
    public void work() {
	//System.out.println(input.popFloat());
	input.popFloat();
    }
}

class PerftestPipeline extends Pipeline {
    public PerftestPipeline(final float center_freq) {
	super(center_freq);
    }
    
    public void init(final float center_freq) {
	add(new ComplexFIRFilter(33000000, 825, 400, center_freq, 2));
	add(new QuadratureDemod(5, 1));
	add(new RealFIRFilter(8000, 5, 4000, 20, 1));
    }
}


class PerftestSplitJoin extends SplitJoin {
    public PerftestSplitJoin()
    {
        super();
    }

    public void init() {
	setSplitter(WEIGHTED_ROUND_ROBIN(825, 825, 825, 825));
	add(new PerftestPipeline(10370400));
	add(new PerftestPipeline(10355400));
	add(new PerftestPipeline(10340400));
	add(new PerftestPipeline(10960500));
	setJoiner(ROUND_ROBIN());
    }
}

class FloatPrinter extends Filter {
    public void init()
    {
	input = new Channel(Float.TYPE, 1);
    }
    
    public void work()
    {
	System.out.println(input.popFloat());
    }
}

public class perftest4 extends StreamIt
{
    
    static public void main(String[] t)
    {
        new perftest4().run(t);
    }
    
    public void init() {
	
	add(new TestSource());
	add(new PerftestSplitJoin());
	add(new FloatPrinter());
    }
}	
