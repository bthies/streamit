import streamit.library.*;

public class Test extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new Test().run(args); 
    }

    public void init() {
	// add a data source
	//this.add(new TestDataSource());
	this.add(new SimpleTestDataSource());
	//this.add(new LimitedSimpleTestDataSource(100));

	// try a delay pipeline
	//this.add(new DelayPipeline(5));
	
	// try a Convolutional Interleaver
	//this.add(new ConvolutionalInterleaver(5));
	//this.add(new ConvolutionalDeinterleaver(5));	

	// try trellis encoding/decoding pipeline
	//this.add(new TrellisEncoderPipeline(1));	
	//this.add(new TrellisDecoderPipeline(1));

	//this.add(new TrellisEncoder());
	//this.add(new TrellisDecoder());
	
	
	// try data reordering...
	//this.add(new Bitifier());
	//this.add(new DataReorder(1));
	//this.add(new DataReorder(2));
	//this.add(new UnBitifier());
	
	
	// add a reed solomon encoder
	//this.add(new ReedSolomonEncoder());
	//this.add(new ReedSolomonDecoder());
	

	//this.add(new PreCoder());
	//this.add(new PreDecoder());

	//this.add(new TrellisEncoder());
	//this.add(new TrellisDecoder());

	//this.add(new TestDataSnooper("input",1));	
	//this.add(new UngerboeckEncoder());
	//this.add(new UngerboeckDecoder());
	//this.add(new TestDataSnooper("output",1));
	
	//this.add(new TrellisEncoder());
	//this.add(new TrellisDecoder());

	
	//add a data sink
	this.add(new TestDataSink());

    }
}





class TestDataSource extends Filter {
    int[] x = new int[10];
    int i = 0;
    public void init() {
	output = new Channel(Integer.TYPE,1);
	x[0] = 0;
	x[1] = 1;
	x[2] = 1;
	x[3] = 0;
	x[4] = 1;
	x[5] = 0;
	x[6] = 1;
	x[7] = 1;
	x[8] = 1;
	x[9] = 0;
	
    }
    public void work() {
	output.pushInt(this.x[this.i]);
	this.i = (this.i + 1) % 10;
    }
}

class SimpleTestDataSource extends Filter {
    int x;
    public void init() {
	this.x = 0;
	output = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	output.pushInt(this.x);
	this.x++;
    }
}


class LimitedSimpleTestDataSource extends Filter {
    int x;
    int limit;
    public LimitedSimpleTestDataSource(int l) {
	super(l);
    }
    public void init(int l) {
	this.limit = l;
	this.x = 0;
	output = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	output.pushInt(this.x);
	this.x = (this.x + 1) % limit;
    }
}


class TestDataSink extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE,1);
    }
    public void work() {
	System.out.println(input.popInt());
    }
}

/**
 * Prints integers on the way by.
 **/
class TestDataSnooper extends Filter {
    String label;
    int num;
    public TestDataSnooper(String l, int n) {
	this.label = l;
	this.num = n;
    }
    public void init() {
	input  = new Channel(Integer.TYPE, this.num);
	output = new Channel(Integer.TYPE, this.num);
    }
    public void work() {
	for (int i=0; i<this.num; i++) {
	    int t = input.popInt();
	    System.out.println(this.label + ": " + t);
	    output.pushInt(t);
	}
    }
}	
	    
	 


