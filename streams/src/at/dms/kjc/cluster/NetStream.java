
package at.dms.kjc.cluster;

//import java.lang.*;
import at.dms.kjc.CType;

/**
 * WTF?
 * 
 * I (AD) think that this represents a tape between two operators as a source operator number,
 * destination operator number, and type carried by the tape.
 * 
 * @author Janis
 *
 */
public class NetStream {

    int source, dest;
    CType type;

    /**
     * Constructor.
     * 
     * @param source  a number (representing a SIROperator?)
     * @param dest    a number (representing a SIROperator?)
     * @param type    a CType (representing type of data communicated between the operators?)
     */
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

    public String pop_name() {
        return new String("__pop_"+source+"_"+dest);    
    }

    public String push_name() {
        return new String("__push_"+source+"_"+dest);   
    }

    public String pop_buffer() {
        return new String("__pop_buffer_"+source+"_"+dest); 
    }

    public String push_buffer() {
        return new String("__push_buffer_"+source+"_"+dest);    
    }

    public String pop_index() {
        return new String("__pop_index_"+source+"_"+dest);  
    }

    public String push_index() {
        return new String("__push_index_"+source+"_"+dest); 
    }

}
