package at.dms.kjc.slicegraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import at.dms.kjc.*;

/**
 * A Buffer is an implementation of an Edge in a back end.
 * It refers to nodes in a slice graph like an edge does, but a Buffer also
 * contains code that a back end can emit to pass data between nodes in the slice graph.
 * 
 * @author dimock
 */
public class Buffer {

    /**
     * technical note: a Buffer in a backend implements an Edge in a slice graph
     * so this data structure uses an Edge to store source, destination, and type information.
     */
    protected Edge theEdge;
    /** unique ident for the buffer */
    protected String ident;
    /** used to make sure ident is unique */
    protected int unique_id;
    private static int unique_id_generator;
    /** the store for all Buffers, indexed by edge.
     */
    protected static HashMap<Edge, Buffer> bufferStore;
 
    static {
        unique_id_generator = 0;
        bufferStore = new HashMap<Edge, Buffer>();
    }

    protected Buffer(Edge edge) {
        assert edge != null;
        theEdge = edge;
        edge.getType(); // side effect of catching error if source and dest types not the same
        unique_id = unique_id_generator++;
    }
    /**
     * Reset the buffer store and create all number buffer objects.  
     * Used if one wants to munge the trace graph.
     */
    public static void reset() {
        unique_id_generator = 0;
        bufferStore = new HashMap<Edge, Buffer>();
    }
    
    /**
     * For debugging.
     */
    public static void printBuffers() {
        for (Buffer buf : bufferStore.values()) {
            System.out.println(buf);
        }
    }

    /** @return of the buffers of this stream program */
    public static Collection<Buffer> getBuffers() {
        return bufferStore.values();
    }


    /* Lots of JBlock, JMethodDeclaration, ... here 
     * Set to empty values, since many do not need to be overridden in many backends. 
     */
    
    public String popMethodName() {
        return "__pop_" + unique_id;
    }
    /** input_type pop() */
    public JMethodDeclaration popMethod() {
        return null;
    }
    
    
    public String popManyMethodName() {
        return "__popN_" + unique_id;
    }
    /** void pop(int N) */
    public JMethodDeclaration popManyMethod() {
        return null;
    }
    
    public String assignFromPopMethodName() {
        return "__popv_" + unique_id;
    }
    /** void pop(input_type val)  generally assign if val is not an array, else memcpy */
    public JMethodDeclaration assignFromPopMethod() {
        return null;
    }
    
    public String peekMethodName() {
        return "__peek_" + unique_id;
    }
    /** input_type peek(int offset) */
    public JMethodDeclaration peekMethod() {
        return null;
    }
    
    public String assignFromPeekMethodName() {
        return "__peekv_" + unique_id;
    }
    /** void peek(input_type val, int offset)  generally assign if val is not an array, else memcpy */
    public JMethodDeclaration assignFromPeekMethod() {
        return null;
    }
    
    public String pushMethodName() {
        return "__push_" + unique_id;
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
