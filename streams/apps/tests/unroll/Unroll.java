import streamit.library.*;

class Incrementer extends Filter {
    public void init() {
        input = new Channel(Integer.TYPE, 1);
        output = new Channel(Integer.TYPE, 1);
    }

    public void work() {
	output.pushInt(input.popInt()+1);
    }
}

class Unroll extends StreamIt {

    public static void main(String[] args) {
	new Unroll().run(args);
    }

    public void init() {
	int i;
	this.add(new Filter() {
		int x;
		public void init() {
		   output = new Channel(Integer.TYPE, 1);
		}
		public void work() {
		    output.pushInt(x++);
		}
	    });
	for (i=0; i<5; i+=1) {
	    add(new Incrementer());
	}
	this.add(new Filter() {
		public void init() {
		    input = new Channel(Integer.TYPE, 1);
		}
		public void work() {
		    System.out.println(input.popInt());
		}
	    });
    }

}
