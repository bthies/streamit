import streamit.*;

class Incrementer extends Filter {
    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel(Integer.TYPE, 1);

    public void initIO() {
	this.streamInput = input;
	this.streamOutput = output;
    }

    public void work() {
	output.pushInt(input.popInt()+1);
    }
}

class Unroll extends StreamIt {

    public static void main(String[] args) {
	new Unroll().run();
    }

    public void init() {
	int i;
	this.add(new Filter() {
		Channel output = new Channel(Integer.TYPE, 1);
		int x;
		public void initIO() {
		    this.streamOutput = output;
		}
		public void work() {
		    output.pushInt(x++);
		}
	    });
	for (i=0; i<5; i+=1) {
	    add(new Incrementer());
	}
	this.add(new Filter() {
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
