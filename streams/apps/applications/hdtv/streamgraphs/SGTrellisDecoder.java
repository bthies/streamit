import streamit.library.*;

public class SGTrellisDecoder extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new SGTrellisDecoder().run(args); 
    }

    public void init() {
	this.add(new InputSource());
	this.add(new TrellisDecoder());
	this.add(new InputSink());
    }
}
