import streamit.library.*;

public class SimpleSplit extends StreamIt {
    public static void main(String args[]) {
	new SimpleSplit().run(args);
    }

    public void init() {
	// push increasing integers
	add(new Filter() {
		int count;
		public void init() {
		    output = new Channel(Integer.TYPE, 1);
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
			    public void init() {
			        input = new Channel(Integer.TYPE, 2);
			        output = new Channel(Integer.TYPE, 2);
			    }
			    public void work() {
				output.pushInt(input.popInt());
				output.pushInt(input.popInt());
			    }
			});
		    // pop two and push two in opposite order
		    this.add(new Filter() {
			    public void init() {
			        input = new Channel(Integer.TYPE, 2);
			        output = new Channel(Integer.TYPE, 2);
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
		public void init() {
		    input = new Channel(Integer.TYPE, 1);
		}
		public void work() {
		    System.out.println(input.popInt());
		}
	    });
    }
}
