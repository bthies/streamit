import streamit.library.*;

public class SGRSDecoder extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new SGRSDecoder().run(args); 
    }

    public void init() {
	this.add(new InputSource());
	this.add(new ReedSolomonDecoder());
	this.add(new InputSink());
    }
}
