
package at.dms.kjc.cluster;

import java.lang.*;
import at.dms.kjc.CType;

public class NetStream {

    int source, dest;
    CType type;

    public NetStream(int source, int dest, CType type) {
	this.source = source;
	this.dest = dest;
	this.type = type;
    }
	
    public int getSource() {
	return source;
    }

    public int getDest() {
	return dest;
    }

    public CType getType() {
	return type;
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
