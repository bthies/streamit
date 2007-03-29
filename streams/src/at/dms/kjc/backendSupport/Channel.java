package at.dms.kjc.backendSupport;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.slicegraph.Util;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;
import at.dms.kjc.spacetime.InterSliceBuffer;
import at.dms.kjc.*;
import at.dms.util.Utils;

/**
 * A Buffer is an implementation of an Edge in a back end.
 * It refers to nodes in a slice graph like an edge does, but a Buffer also
 * contains code that a back end can emit to pass data between nodes in the slice graph.
 * 
 * @author dimock
 */
public class Channel {

    /**
     * Technical note: a Buffer in a backend implements an Edge in a slice graph
     * This data structure uses an Edge to store source, destination, and type information.
     *
     * XXX fix this? is sharing edges with the sliceGraph
     * can see wanting to optimize buffers to remove useless edges
     * and wanting to change source and dest,
     * presumably without changing slice graph. 
     */
    protected Edge theEdge;
    /** unique ident for the buffer */
    protected String ident;
    /** used to make sure ident is unique */
    protected int unique_id;
    private static int unique_id_generator;
    /** the store for all Buffers, indexed by edge.
     */
    protected static HashMap<Edge, Channel> bufferStore;
    /** the rotation length of this buffer for software pipelining 
     * Includes any length from extraLength field. **/
    protected int rotationLength;
    /** set to 1 for double bufferring or higher if even more extra buffers desired */
    protected int extraCount;

    static {
        unique_id_generator = 0;
        bufferStore = new HashMap<Edge, Channel>();
    }

    /**
     * Create a buffer given an edge.
     * @param edge
     */
    protected Channel(Edge edge) {
        assert edge != null;
        theEdge = edge;
        edge.getType(); // side effect of catching error if source and dest types not the same
        unique_id = unique_id_generator++;
        ident = "__buf__" + unique_id + "__";
        rotationLength = 1;
    }
    
    /**
     * Create a buffer given src and dst SliceNode's
     * @param src
     * @param dst
     */
    protected Channel(SliceNode src, SliceNode dst) {
        this(Util.srcDstToEdge(src, dst));
    }
    
    public static Channel getChannel(Edge edge) {
        if (!bufferStore.containsKey(edge)) {
            System.out.println("Creating Channel from " + edge.getSrc() + " to " + edge.getDest());
            bufferStore.put(edge, new Channel(edge));
        }
        return bufferStore.get(edge);
    }

    /**
     * Reset the buffer store and create all number buffer objects.  
     * Used if one wants to munge the trace graph.
     */
    public static void reset() {
        unique_id_generator = 0;
        bufferStore = new HashMap<Edge, Channel>();
    }
    
    /**
     * For debugging.
     */
    public static void printBuffers() {
        for (Channel buf : bufferStore.values()) {
            System.out.println(buf);
        }
    }

    /** @return of the buffers of this stream program */
    public static Collection<Channel> getBuffers() {
        return bufferStore.values();
    }

    /** Get the type of data to be stored in the buffer 
     * You can not set the type, it is determined at buffer
     * creation time.
     * @return type of data to be stored in the buffer */
    public CType getType() {
        return theEdge.getType();
    }


    /** 
     * Determine whether logical buffer has extra
     * physical space for double-bufferring.
     * If 0 then no double bufferring.
     * If 1 then double bufferring.
     * If > 1 then n+1 bufferring.
     * @return number of extra comm buffers for double bufferring or 0 if none.
     */
    public int getExtraCount() {
        return extraCount;
    }
    
    /**
     * Call with 1 to set double bufferring.
     * Or > 1 for n+1-bufferring.
     * No effect after {@link #setRotationLengths} has been called.
     */
    public void setExtralength(int extracount) {
        this.extraCount = extracount;
    }
    
    /** get the source (upstream) SliceNode for this buffer. */
    public SliceNode getSource() {
        return theEdge.getSrc();
    }

    /** get the destination (downstream) SliceNode for this buffer. */
    public SliceNode getDest() {
        return theEdge.getDest();
    }

    /**
     * Set rotation count: number of buffers needed during primePump phase.
     * Also adds in extracount to set up double-bufferring if desired.
     * @param sched BasicSpaceTimeSchedule gives primePump multiplicities.
     */
    public static void setRotationLengths(BasicSpaceTimeSchedule sched) {
        for (Channel buf : getBuffers()) {
            setRotationLength(buf, sched);
        }
    }
    
    /**
     * Set the rotation length of each buffer based on the multiplicities 
     * of the source trace and the dest trace in the prime pump schedule and add
     * in extraCount field to enable double bufferring if desired.
     * 
     * @param buffer
     */
    private static void setRotationLength(Channel buffer, BasicSpaceTimeSchedule spaceTimeSchedule) {
        int sourceMult = spaceTimeSchedule.getPrimePumpMult(buffer.getSource().getParent());
        int destMult = spaceTimeSchedule.getPrimePumpMult(buffer.getDest().getParent());

        // if source run more often than dest, then need extra space.
        if (sourceMult > destMult) {
            buffer.rotationLength = sourceMult - destMult + buffer.extraCount; 
        } else {
            buffer.rotationLength = buffer.extraCount;
        }
        
        //System.out.println("Setting rotation length: " + buffer + " " + length);
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
 
    JMethodDeclaration popManyCode = null;
    
    /** void pop(int N).  This default version just calls pop() N times. */
    public JMethodDeclaration popManyMethod() {
        if (popManyCode != null) {
            return popManyCode;
        }
        
        String formalParamName = "n";
        CType formalParamType = CStdType.Integer;
        
        JVariableDefinition nPopsDef = new JVariableDefinition(formalParamType, formalParamName);
        JExpression nPops = new JLocalVariableExpression(nPopsDef);
        
        JVariableDefinition loopIndex = new JVariableDefinition(formalParamType, "i");
        
        JStatement popOne = new JExpressionStatement(
                new JMethodCallExpression(popMethodName(),new JExpression[0]));
        
        JBlock body = new JBlock();
        body.addStatement(Utils.makeForLoop(popOne, nPops, loopIndex));
        
        popManyCode = new JMethodDeclaration(CStdType.Void,
                popManyMethodName(),
                new JFormalParameter[]{new JFormalParameter(formalParamType, formalParamName)},
                body);
        return popManyCode;
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
