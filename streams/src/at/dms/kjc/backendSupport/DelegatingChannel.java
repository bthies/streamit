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
 *  but all other operatoins are delegated.
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

    public JMethodDeclaration assignFromPeekMethod() {
        // TODO Auto-generated method stub
        return other.assignFromPeekMethod();
    }

    public String assignFromPeekMethodName() {
        // TODO Auto-generated method stub
        return other.assignFromPeekMethodName();
    }

    public JMethodDeclaration assignFromPopMethod() {
        // TODO Auto-generated method stub
        return other.assignFromPopMethod();
    }

    public String assignFromPopMethodName() {
        // TODO Auto-generated method stub
        return other.assignFromPopMethodName();
    }

    public List<JStatement> beginInitRead() {
        // TODO Auto-generated method stub
        return other.beginInitRead();
    }

    public List<JStatement> beginInitWrite() {
        // TODO Auto-generated method stub
        return other.beginInitWrite();
    }

    public List<JStatement> beginSteadyRead() {
        // TODO Auto-generated method stub
        return other.beginSteadyRead();
    }

    public List<JStatement> beginSteadyWrite() {
        // TODO Auto-generated method stub
        return other.beginSteadyWrite();
    }

    public List<JStatement> dataDecls() {
        // TODO Auto-generated method stub
        return other.dataDecls();
    }

    public List<JStatement> dataDeclsH() {
        // TODO Auto-generated method stub
        return other.dataDeclsH();
    }

    public List<JStatement> endInitRead() {
        // TODO Auto-generated method stub
        return other.endInitRead();
    }

    public List<JStatement> endInitWrite() {
        // TODO Auto-generated method stub
        return other.endInitWrite();
    }

    public List<JStatement> endSteadyRead() {
        // TODO Auto-generated method stub
        return other.endSteadyRead();
    }

    public List<JStatement> endSteadyWrite() {
        // TODO Auto-generated method stub
        return other.endSteadyWrite();
    }

    public int getExtraCount() {
        // TODO Auto-generated method stub
        return other.getExtraCount();
    }

    public String getIdent() {
        // TODO Auto-generated method stub
        return ident;
    }

    public SliceNode getDest() {
        // TODO Auto-generated method stub
        return theEdge.getDest();
    }
    public SliceNode getSource() {
        // TODO Auto-generated method stub
        return theEdge.getSrc();
    }

    public CType getType() {
        // TODO Auto-generated method stub
        return theEdge.getType();
    }

    public JMethodDeclaration peekMethod() {
        // TODO Auto-generated method stub
        return other.peekMethod();
    }

    public String peekMethodName() {
        // TODO Auto-generated method stub
        return other.peekMethodName();
    }

    public JMethodDeclaration popManyMethod() {
        // TODO Auto-generated method stub
        return other.popManyMethod();
    }

    public String popManyMethodName() {
        // TODO Auto-generated method stub
        return other.popManyMethodName();
    }

    public JMethodDeclaration popMethod() {
        // TODO Auto-generated method stub
        return other.popMethod();
    }

    public String popMethodName() {
        // TODO Auto-generated method stub
        return other.popMethodName();
    }

    public JMethodDeclaration pushMethod() {
        // TODO Auto-generated method stub
        return other.pushMethod();
    }

    public String pushMethodName() {
        // TODO Auto-generated method stub
        return other.pushMethodName();
    }

    public List<JStatement> readDecls() {
        // TODO Auto-generated method stub
        return other.readDecls();
    }

    public List<JStatement> readDeclsExtern() {
        // TODO Auto-generated method stub
        return other.readDeclsExtern();
    }

    public void setExtralength(int extracount) {
        // TODO Auto-generated method stub
        other.setExtralength(extracount);
    }

    public List<JStatement> topOfWorkSteadyRead() {
        // TODO Auto-generated method stub
        return other.topOfWorkSteadyRead();
    }

    public List<JStatement> topOfWorkSteadyWrite() {
        // TODO Auto-generated method stub
        return other.topOfWorkSteadyWrite();
    }

    public List<JStatement> writeDecls() {
        // TODO Auto-generated method stub
        return other.writeDecls();
    }

    public List<JStatement> writeDeclsExtern() {
        // TODO Auto-generated method stub
        return other.writeDeclsExtern();
    }
}
