import streamit.library.*;

public class SGRSEncoder extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new SGRSEncoder().run(args); 
    }

    public void init() {
	this.add(new InputSource());
	this.add(new ReedSolomonEncoder());
	this.add(new InputSink());
    }
}
