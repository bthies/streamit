
package at.dms.kjc.cluster;

import java.lang.*;

public class NetStream {

    int source, dest;
    
    public NetStream(int source, int dest) {
	this.source = source;
	this.dest = dest;
    }
	
    public int getSource() {
	return source;
    }

    public int getDest() {
	return dest;
    }

    public String name() {
	return new String("__stream_"+source+"_"+dest);	
    }

    public String producer_name() {
	return new String("__producer_"+source+"_"+dest);	
    }

    public String consumer_name() {
	return new String("__consumer_"+source+"_"+dest);	
    }

}
