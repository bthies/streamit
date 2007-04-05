package at.dms.kjc.backendSupport;

import java.util.*;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.*;

/**
 * Channel implementation as an array.
 * TODO: missing support for multi-bufferring, rotating buffers
 * TODO: missing support for arrays passed over channels.
 * @author dimock
 *
 */
public class ChannelAsArray extends Channel {

    protected int arraySize;
    protected String bufName;
    protected String headName;
    protected String tailName;
    
    /**
     * Make a new Channel or return an already-made channel.
     * @param edge     The edge that this channel implements.
     * @param other    The channel that this delegates to.
     * @return A channel for this edge, that 
     */
    public static ChannelAsArray getChannel(Edge edge) {
        Channel oldChan = Channel.bufferStore.get(edge);
        if (oldChan == null) {
            ChannelAsArray chan = new ChannelAsArray(edge);
            Channel.bufferStore.put(edge, chan);
            return chan;
       } else {
            assert oldChan instanceof ChannelAsArray; 
            return (ChannelAsArray)oldChan;
        }
    }

    public ChannelAsArray(Edge edge) {
        super(edge);
        arraySize = BufferSize.calculateSize(edge);
        bufName = this.getIdent() + "buf";
        headName = this.getIdent() + "head";
        tailName = this.getIdent() + "tail";
    }

    public ChannelAsArray(SliceNode src, SliceNode dst) {
        super(src, dst);
        throw new AssertionError("Creating ChannelAsArray from src, dst not supported.");
    }

    /** input_type pop() */
    public JMethodDeclaration popMethod() {
        return null;
    }

    /** void pop(int N).  This default version just calls pop() N times. */
    public JMethodDeclaration popManyMethod() {
        return null;
    }
    
    /** void pop(input_type val)  generally assign if val is not an array, else memcpy */
    public JMethodDeclaration assignFromPopMethod() {
        return null;
    }
    
    /** input_type peek(int offset) */
    public JMethodDeclaration peekMethod() {
        return null;
    }

    /** void peek(input_type val, int offset)  generally assign if val is not an array, else memcpy */
    public JMethodDeclaration assignFromPeekMethod() {
        return null;
    }

   /** void push(output_type val) */
    public JMethodDeclaration pushMethod() {
        return null;
    }
    
    /** Statements for beginning of init() on read (downstream) end of buffer */
    public List<JStatement> beginInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /** Statements for end of init() on read (downstream) end of buffer */
    public List<JStatement> endInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /** Statements for beginning of init() on write (upstream) end of buffer */
    public List<JStatement> beginInitWrite() {
        return new LinkedList<JStatement>(); 
    }

    /** Statements for end of init() on write (upstream) end of buffer */
    public List<JStatement> endInitWrite() {
        return new LinkedList<JStatement>(); 
    }
    
    /** Statements for beginning of steady state iteration on read (downstream) end of buffer */
    public List<JStatement> beginSteadyRead() {
        return new LinkedList<JStatement>(); 
    }

    /** Statements for end of steady state iteration on read (downstream) end of buffer */
    public List<JStatement> endSteadyRead() {
        return new LinkedList<JStatement>(); 
    }

    /** Statements for beginning of steady state iteration on write (upstream) end of buffer */
    public List<JStatement> beginSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }

    /** Statements for end of steady state iteration on write (upstream) end of buffer */
    public List<JStatement> endSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }
    
    /** Statements for beginning of work function.
     * May be more convenient than at top of steady state if work function iterated. */
    public List<JStatement> topOfWorkSteadyRead() {
        return new LinkedList<JStatement>(); 
    }
    
    /** Statements for beginning of work function.
     * May be more convenient than at top of steady state if work function iterated. */
    public List<JStatement> topOfWorkSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }
 
    /** Statements for data declaration in .h file */
    public List<JStatement> dataDeclsH() {
        return new LinkedList<JStatement>();
    }
    
    /** Statements for data declaration at top of .c / .cpp file */
    public List<JStatement> dataDecls() {
        return new LinkedList<JStatement>();
    }
    
    /** Statements for extern declarations needed for read 
     * in steady state but at global scope in .c / .cpp */
    public List<JStatement> readDeclsExtern() {
        return new LinkedList<JStatement>();
    }   
    
    /** Statements for other declarations needed for read  
     * in steady state but at file scope in .c / .cpp */
    public List<JStatement> readDecls() {
        return new LinkedList<JStatement>();
    }   
    
    
    /** Statements for extern declarations needed for write 
     * in steady state but at global scope in .c / .cpp */
    public List<JStatement> writeDeclsExtern() {
        return new LinkedList<JStatement>();
    }   
    
    /** Statements for other declarations needed for write
     * in steady state but at file scope in .c / .cpp */
    public List<JStatement> writeDecls() {
        return new LinkedList<JStatement>();
    }   

}

