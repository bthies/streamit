import streamit.library.*;

class Source extends Filter {
    int i;
    public void init() {
	output = new Channel(Integer.TYPE, 1);
	i=0;
    }
    public void work() {
	output.pushInt(i++);
    }
}

class Sink extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	System.out.println(input.popInt());
    }
}

class OneToOne extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	output.pushInt(input.popInt());
    }
}

public class WeightedRR extends StreamIt {

    public static void main(String[] args) {
	new WeightedRR().run(args);
    }

    public void init() {
	add(new Source());
	add(new SplitJoin() {
		public void init() {
		    setSplitter(WEIGHTED_ROUND_ROBIN(2,2,2));
		    this.add(new OneToOne());
		    this.add(new OneToOne());
		    this.add(new OneToOne());
		    setJoiner(WEIGHTED_ROUND_ROBIN(2,2,2));
		}
		
	    });
	add(new Sink());
    }
}
