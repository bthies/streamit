
package at.dms.kjc.sir;

/** 
 * Represents a SIRStream and SIRLatency pair
 */

public class SIRPortalSender {

    protected SIRStream stream;
    protected SIRLatency latency;

    public SIRPortalSender(SIRStream stream, SIRLatency latency) {
	this.stream = stream;
	this.latency = latency;
    }

    public boolean equals(Object obj) {   
	if (obj instanceof SIRPortalSender) {
	    SIRPortalSender ps = (SIRPortalSender)obj;
	    return stream.equals(ps.stream)&&latency.equals(ps.latency);
	} else {
	    return false;
	}
    }

    /*
     * Returns the SIRStream
     */

    public SIRStream getStream() {
	return stream;
    } 

    /*
     * Returns the SIRLatency
     */

    public SIRLatency getLatency() {
	return latency;
    } 
}
