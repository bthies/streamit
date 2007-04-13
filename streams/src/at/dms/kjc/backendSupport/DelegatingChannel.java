package at.dms.kjc.backendSupport;

import java.util.List;

import at.dms.kjc.CType;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JStatement;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;

/**
 * A Channel that delegates all useful work to another channel.
 * This is used to maintain one Channel per Edge in the case where
 * a SliceNode does not generate any code.  In such a case the
 * Channel for an intra-slice edge will delagate to the Channel
 * for the InterSliceEdge.
 * 
 *  A {@link DelegatingChannel} has its own source and destination
 *  but all other operations are delegated.
 *  It does not produce any method (or other) declarations since
 *  these would be redundant with the channel delegated to.
 *  
 * @author dimock
 */
public class DelegatingChannel extends Channel {
    
    private Channel other;
    /**
     * Make a new Channel or return an already-made channel.
     * @param edge     The edge that this channel implements.
     * @param other    The channel that this delegates to.
     * @return A channel for this edge, that 
     */
    public static DelegatingChannel getChannel(Edge edge, Channel other) {
        Channel oldChan = Channel.bufferStore.get(edge);
        if (oldChan == null) {
            DelegatingChannel chan = new DelegatingChannel(edge, other);
            Channel.bufferStore.put(edge, chan);
            return chan;
       } else {
            assert oldChan instanceof DelegatingChannel; 
            return (DelegatingChannel)oldChan;
        }
    }
    
    private DelegatingChannel(Edge edge, Channel other) {
        super(edge);
        this.other = other;
    }

    @Override
    public JMethodDeclaration assignFromPeekMethod() {
        return null;
    }

    @Override
    public String assignFromPeekMethodName() {
        return other.assignFromPeekMethodName();
    }

    @Override
    public JMethodDeclaration assignFromPopMethod() {
        return null;
    }

    @Override
    public String assignFromPopMethodName() {
        return other.assignFromPopMethodName();
    }

    @Override
    public List<JStatement> beginInitRead() {
        return other.beginInitRead();
    }

    @Override
    public List<JStatement> beginInitWrite() {
        return other.beginInitWrite();
    }

    @Override
    public List<JStatement> beginSteadyRead() {
        return other.beginSteadyRead();
    }

    @Override
    public List<JStatement> beginSteadyWrite() {
        return other.beginSteadyWrite();
    }

    @Override
    public List<JStatement> dataDecls() {
        return null;
    }

    @Override
    public List<JStatement> dataDeclsH() {
        return null;
    }

    @Override
    public List<JStatement> endInitRead() {
        return other.endInitRead();
    }

    @Override
    public List<JStatement> endInitWrite() {
        return other.endInitWrite();
    }

    @Override
    public List<JStatement> endSteadyRead() {
        return other.endSteadyRead();
    }

    @Override
    public List<JStatement> endSteadyWrite() {
        return other.endSteadyWrite();
    }

    @Override
    public int getExtraCount() {
        return other.getExtraCount();
    }

    @Override
    public String getIdent() {
        return ident;
    }

    @Override
    public SliceNode getDest() {
        return theEdge.getDest();
    }

    @Override
    public SliceNode getSource() {
        return theEdge.getSrc();
    }

    @Override
    public CType getType() {
        return theEdge.getType();
    }

    @Override
    public JMethodDeclaration peekMethod() {
        return null;
    }

    @Override
    public String peekMethodName() {
        return other.peekMethodName();
    }

    @Override
    public JMethodDeclaration popManyMethod() {
        return null;
    }

    @Override
    public String popManyMethodName() {
        return other.popManyMethodName();
    }

    @Override
    public JMethodDeclaration popMethod() {
        return null;
    }

    @Override
    public String popMethodName() {
        return other.popMethodName();
    }

    @Override
    public List<JStatement> postPreworkInitRead() {
        return other.postPreworkInitRead();
    }

    @Override
    public JMethodDeclaration pushMethod() {
        return null;
    }

    @Override
    public String pushMethodName() {
        return other.pushMethodName();
    }

    @Override
    public List<JStatement> readDecls() {
        return null;
    }

    @Override
    public List<JStatement> readDeclsExtern() {
        return null;
    }

    @Override
    public void setExtralength(int extracount) {
        other.setExtralength(extracount);
    }

    @Override
    public List<JStatement> topOfWorkSteadyRead() {
        return other.topOfWorkSteadyRead();
    }

    @Override
    public List<JStatement> topOfWorkSteadyWrite() {
        return other.topOfWorkSteadyWrite();
    }

    @Override
    public List<JStatement> writeDecls() {
        return null;
    }

    @Override
    public List<JStatement> writeDeclsExtern() {
        return null;
    }
}
