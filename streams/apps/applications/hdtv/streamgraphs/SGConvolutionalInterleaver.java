import streamit.library.*;

public class SGConvolutionalInterleaver extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new SGConvolutionalInterleaver().run(args); 
    }

    public void init() {
	this.add(new InputSource());
	this.add(new ConvolutionalInterleaver(5));
	this.add(new InputSink());
    }
}
