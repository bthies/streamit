import streamit.library.*;

public class SGConvolutionalDeinterleaver extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new SGConvolutionalDeinterleaver().run(args); 
    }

    public void init() {
	this.add(new InputSource());
	this.add(new ConvolutionalDeinterleaver(5));
	this.add(new InputSink());
    }
}
