package at.dms.kjc.backendSupport;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import at.dms.kjc.slicegraph.Edge;
//import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.slicegraph.Util;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;
//import at.dms.kjc.spacetime.InterSliceBuffer;
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
     * Create a channel given an edge.
     * Subclasses should provide factories for their channel types.
     * @param edge
     */
    protected Channel(Edge edge) {
        assert edge != null;
        theEdge = edge;
        edge.getType(); // side effect of catching error if source and dest types not the same
        unique_id = unique_id_generator++;
        ident = "__chan__" + unique_id + "__";
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
    
    
    /**
     * Determine whther a channel for an edge exists in out collection of channels.
     * @param edge  Edge that the channel should implement
     * @return an existing channel if there is one, else null.
     */
    public static Channel findChannel(Edge edge) {
        return bufferStore.get(edge);
    }

    /**
     * Add a Channel to our collection of channels.
     * @param c Channel to add to our collection of channels. 
     */
    public static void addChannel(Channel c) {
        bufferStore.put(c.theEdge, c);
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

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#getIdent()
     */
    public String getIdent() {
        return ident;
    }
    
    /** @return of the buffers of this stream program */
    public static Collection<Channel> getBuffers() {
        return bufferStore.values();
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#getType()
     */
    public CType getType() {
        return theEdge.getType();
    }


    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#getExtraCount()
     */
    public int getExtraCount() {
        return extraCount;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#setExtralength(int)
     */
    public void setExtralength(int extracount) {
        this.extraCount = extracount;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#getSource()
     */
    public SliceNode getSource() {
        return theEdge.getSrc();
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#getDest()
     */
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
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethodName()
     */
    public String popMethodName() {
        return "__pop_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popMethod()
     */
    public JMethodDeclaration popMethod() {
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popManyMethodName()
     */
    public String popManyMethodName() {
        return "__popN_" + unique_id;
    }
 
    JMethodDeclaration popManyCode = null;
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#popManyMethod()
     */
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

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethodName()
     */
    public String assignFromPopMethodName() {
        return "__popv_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPopMethod()
     */
    public JMethodDeclaration assignFromPopMethod() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethodName()
     */
    public String peekMethodName() {
        return "__peek_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#peekMethod()
     */
    public JMethodDeclaration peekMethod() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethodName()
     */
    public String assignFromPeekMethodName() {
        return "__peekv_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#assignFromPeekMethod()
     */
    public JMethodDeclaration assignFromPeekMethod() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethodName()
     */
    public String pushMethodName() {
        return "__push_" + unique_id;
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#pushMethod()
     */
    public JMethodDeclaration pushMethod() {
        return null;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitRead()
     */
    public List<JStatement> beginInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitRead()
     */
    public List<JStatement> postPreworkInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitRead()
     */
    public List<JStatement> endInitRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginInitWrite()
     */
    public List<JStatement> beginInitWrite() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endInitWrite()
     */
    public List<JStatement> endInitWrite() {
        return new LinkedList<JStatement>(); 
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyRead()
     */
    public List<JStatement> beginSteadyRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyRead()
     */
    public List<JStatement> endSteadyRead() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#beginSteadyWrite()
     */
    public List<JStatement> beginSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#endSteadyWrite()
     */
    public List<JStatement> endSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#topOfWorkSteadyRead()
     */
    public List<JStatement> topOfWorkSteadyRead() {
        return new LinkedList<JStatement>(); 
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#topOfWorkSteadyWrite()
     */
    public List<JStatement> topOfWorkSteadyWrite() {
        return new LinkedList<JStatement>(); 
    }
 
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#dataDeclsH()
     */
    public List<JStatement> dataDeclsH() {
        return new LinkedList<JStatement>();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#dataDecls()
     */
    public List<JStatement> dataDecls() {
        return new LinkedList<JStatement>();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#readDeclsExtern()
     */
    public List<JStatement> readDeclsExtern() {
        return new LinkedList<JStatement>();
    }   
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#readDecls()
     */
    public List<JStatement> readDecls() {
        return new LinkedList<JStatement>();
    }   
    
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDeclsExtern()
     */
    public List<JStatement> writeDeclsExtern() {
        return new LinkedList<JStatement>();
    }   
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.ChannelI#writeDecls()
     */
    public List<JStatement> writeDecls() {
        return new LinkedList<JStatement>();
    }   
    
}
