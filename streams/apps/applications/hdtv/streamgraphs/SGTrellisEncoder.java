import streamit.library.*;

public class SGTrellisEncoder extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new SGTrellisEncoder().run(args); 
    }

    public void init() {
	this.add(new InputSource());
	this.add(new TrellisEncoder());
	this.add(new InputSink());
    }
}
