import streamit.library.*;

public class SGTrellisDecoderPipeline extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new SGTrellisDecoderPipeline().run(args); 
    }

    public void init() {
	this.add(new InputSource());
	this.add(new DataReorder(2));
	this.add(new TrellisDecoderPipeline(12));
	this.add(new InputSink());
    }
}
