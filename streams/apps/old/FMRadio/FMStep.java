import streamit.library.*;

class TestSource extends Filter{
    int c;

    public void init() {
	output = new Channel(Float.TYPE, 1);
	c = 0;
    }
    public void work() {
	output.pushFloat((float)c);
	c++;
	if (c >= 1000)
	    c= 0;
    }
}
	    
class NullSink extends Filter {
    int i;

    public void init() {
	input = new Channel(Float.TYPE, 1);
	i =0;
    }
    public void work() {
	//System.out.println(input.popFloat());
	input.popFloat();
    }
}

public class FMStep extends StreamIt {
    static public void main(String[] t)
    {
        new FMStep().run(t);
    }
    
    public void init() {
	final float samplingRate = 200000; //200khz sampling rate according to jeff at vanu
	final float cutoffFrequency = 108000000; //guess... doesn't FM freq max at 108 Mhz? 
	final int numberOfTaps = 100;
	final float maxAmplitude = 27000;
	final float bandwidth = 10000;

	add(new TestSource());
	add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps, 4));
	add(new FMDemodulator(samplingRate, maxAmplitude, bandwidth));
	add(new Equalizer(samplingRate));
	add(new NullSink());
    }
}
