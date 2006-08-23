
package at.dms.kjc.cluster;

//import java.lang.*;
import at.dms.kjc.CType;
//import java.util.HashSet;

/**
 * A class that represents a tape with input and output operators and object type.
 * The input and output operators are identified by numbers. The mapping between
 * numbers and nodes is maintained by NodeEnumerator.
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
     * @param source  a number representing input operator
     * @param dest    a number representing output operator
     * @param type    a CType representing type of data communicated between the operators
     */
    public NetStream(int source, int dest, CType type) {
        this.source = source;
        this.dest = dest;
        this.type = type;
    }


    /** 
     * Get number representing the input operator
     *
     * @return The number representing the input operator
     */
    
    public int getSource() {
        return source;
    }

    /** 
     * Get number representing the output operator
     *
     * @return The number representing the output operator
     */

    public int getDest() {
        return dest;
    }

    /** 
     * Get type of objects communicated over the tape
     *
     * @return The type of objects communicated over the tape
     */

    public CType getType() {
        return type;
    }

//    public String name() {
//        return new String("__stream_"+source+"_"+dest); 
//    }

    public String producer_name() {
        return new String("__producer_"+source+"_"+dest);   
   }

    public String consumer_name() {
        return new String("__consumer_"+source+"_"+dest);   
   }

    public String pop_name() {
        return new String("__pop_"+source+"_"+dest);    
    }

    /** name of a push routine */
    public String push_name() {
        return new String("__push_"+source+"_"+dest);   
    }

    /** a buffer name */
    public String pop_buffer() {
        return new String("__pop_buffer_"+source+"_"+dest); 
    }

    /** a buffer name */
    public String push_buffer() {
        return new String("__push_buffer_"+source+"_"+dest);    
    }

    /** a variable name for a tape offset */
    public String pop_index() {
        return new String("__pop_index_"+source+"_"+dest);  
    }

    /** a variable name for a tape offset */
    public String push_index() {
        return new String("__push_index_"+source+"_"+dest); 
    }

}
