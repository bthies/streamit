import streamit.*;

public class SimpleSplit extends StreamIt {
    public static void main(String args[]) {
	new SimpleSplit().run();
    }

    public void init() {
	// push increasing integers
	add(new Filter() {
		Channel output = new Channel(Integer.TYPE, 1);
		int count;
		public void initIO() {
		    this.streamOutput = output;
		}
		public void init() {
		    this.count = 0;
		}
		public void work() {
		    output.pushInt(count++);
		}
	    });
	// add a split-join
	add(new SplitJoin() {
		public void init() {
		    setSplitter(ROUND_ROBIN());
		    // pop two and push two
		    this.add(new Filter() {
			    Channel input = new Channel(Integer.TYPE, 2);
			    Channel output = new Channel(Integer.TYPE, 2);
			    public void initIO() {
				this.streamInput = input;
				this.streamOutput = output;
			    }
			    public void work() {
				output.pushInt(input.popInt());
				output.pushInt(input.popInt());
			    }
			});
		    // pop two and push two in opposite order
		    this.add(new Filter() {
			    Channel input = new Channel(Integer.TYPE, 2);
			    Channel output = new Channel(Integer.TYPE, 2);
			    public void initIO() {
				this.streamInput = input;
				this.streamOutput = output;
			    }
			    public void work() {
				int i1 = input.popInt();
				int i2 = input.popInt();
				output.pushInt(i2);
				output.pushInt(i1);
			    }
			});
		    setJoiner(ROUND_ROBIN());
		}});
	// print result
	add(new Filter() {
		Channel input = new Channel(Integer.TYPE, 1);
		public void initIO() {
		    this.streamInput = input;
		}
		public void work() {
		    System.out.println(input.popInt());
		}
	    });
    }
}
