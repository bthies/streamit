import streamit.*;
class TestSource extends Filter{
    public void init() {
	output = new Channel(Float.TYPE, 1650);
    }
    public void work() {
	int i;
	for (i = 0; i < 1650; i++) 
	    output.pushFloat(0);
    }
}
	    
class NullSink extends Filter {
    public void init() {
	input = new Channel(Short.TYPE, 1);
    }
    public void work() {
	//System.out.println(input.popShort());
	input.popShort();
    }
}

class PerftestPipeline extends Pipeline {
    public PerftestPipeline(final float center_freq) {
	super(center_freq);
    }
    
    public void init(final float center_freq) {
	add(new ComplexFIRFilter(33000000, 825, 400, center_freq, 2));
	add(new QuadratureDemod(5, 0));
	add(new RealFIRFilter(8000, 5, 4000, 20, 1));
    }
}


class PerftestSplitJoin extends Pipeline {
    public PerftestSplitJoin()
    {
        super();
    }

    public void init() {
	add(new SplitJoin() {
		public void init() {
		    setSplitter(WEIGHTED_ROUND_ROBIN(825, 825, 825, 825));
		    add(new ComplexFIRFilter(33000000, 825, 400, 10370400, 2));
		    add(new ComplexFIRFilter(33000000, 825, 400, 10355400, 2));
		    add(new ComplexFIRFilter(33000000, 825, 400, 10340400, 2));
		    add(new ComplexFIRFilter(33000000, 825, 400, 10960500, 2));
		    setJoiner(ROUND_ROBIN());
		}
	    });

	add(new SplitJoin() {
		public void init() {
		    setSplitter(ROUND_ROBIN());
		    add(new QuadratureDemod(5, 0));
		    add(new QuadratureDemod(5, 0));
		    add(new QuadratureDemod(5, 0));
		    add(new QuadratureDemod(5, 0));
		    setJoiner(ROUND_ROBIN());
		}
	    });

	add(new SplitJoin() {
		public void init() {
		    setSplitter(ROUND_ROBIN());
		    add(new RealFIRFilter(8000, 5, 4000, 20, 1));
		    add(new RealFIRFilter(8000, 5, 4000, 20, 1));
		    add(new RealFIRFilter(8000, 5, 4000, 20, 1));
		    add(new RealFIRFilter(8000, 5, 4000, 20, 1));
		    setJoiner(ROUND_ROBIN());
		}
	    });
    }
}

public class perftest4_inout extends StreamIt
{
    
    static public void main(String[] t)
    {
        perftest4_inout test = new perftest4_inout();
        test.run(t);
    }
    
    public void init() {
	
	add(new TestSource());
	add(new PerftestSplitJoin());
	add(new NullSink());
    }
}	
