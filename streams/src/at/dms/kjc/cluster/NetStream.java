
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

}
