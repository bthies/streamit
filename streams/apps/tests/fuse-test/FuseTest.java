import streamit.library.*;

public class FuseTest extends StreamIt {
    static public void main(String[] args) {
	new FuseTest().run(args);
    }

    public void init() {
	add(new FloatSource());
	add(new UpSampler(3));
	add(new MovingAverage(4));
	add(new FloatPrinter());
    }
}

class FloatSource extends Filter {

    float i;

    public FloatSource() {
	super();
    }

    public void init() {
	output = new Channel(Float.TYPE, 1);
	i = 0f;
    }

    public void work() {
	output.pushFloat(i);
	i = i+1f;
    }
}

class FloatPrinter extends Filter {
    public FloatPrinter() {
	super();
    }

    public void init() {
	input = new Channel(Float.TYPE, 1);
    }

    public void work() {
	System.out.println(input.pop());
    }
}

class UpSampler extends Filter {
    int myK;

    public UpSampler(int K) {
	super(K);
    }

    public void init(int K) {
	this.myK = K;
	input = new Channel(Float.TYPE, 1);
	output = new Channel(Float.TYPE, K);
    }

    public void work() {
	int i;
	float val = input.popFloat();
	for (i=0; i<myK; i++) {
	    output.pushFloat(val);
	}
    }
}

class MovingAverage extends Filter {
    float myN;

    public MovingAverage(int N) {
	super(N);
    }

    public void init(int N) {
	this.myN = N;
	input = new Channel(Float.TYPE, 1, N);
	output = new Channel(Float.TYPE, 1);
    }

    public void work() {
	int i;
	float sum = 0;
	for (i=0; i<myN; i++) {
	    sum = sum + input.peekFloat(i);
	}
	output.pushFloat(sum/myN);
	input.pop();
    }
}
