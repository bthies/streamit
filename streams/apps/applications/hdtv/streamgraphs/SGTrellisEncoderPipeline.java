import streamit.library.*;

public class SGTrellisEncoderPipeline extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new SGTrellisEncoderPipeline().run(args); 
    }

    public void init() {
	this.add(new InputSource());
	this.add(new TrellisEncoderPipeline(12));
	this.add(new DataReorder(1));
	this.add(new InputSink());
    }
}
