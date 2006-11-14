package at.dms.kjc.slicegraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.dms.kjc.sir.SIRJoiner;
import at.dms.kjc.sir.SIROperator;
import at.dms.kjc.sir.SIRSplitter;

public class FlatFilter {

    private SIROperator contents;
    private List<FlatFilter> outgoing;
    private List<FlatFilter> incoming;
    private List<Integer> outWeights;
    private List<Integer> inWeights;
//    private int numOut;
//    private int numIn;
    
    private static int ID_COUNTER = 0;
    private final int id;
    
    public FlatFilter(SIROperator op) {
        contents = op;
        outgoing = new ArrayList<FlatFilter>();
        incoming = new ArrayList<FlatFilter>();
        outWeights = new ArrayList<Integer>();
        inWeights = new ArrayList<Integer>();
        if (op instanceof SIRSplitter) {
            int[] weights = ((SIRSplitter) op).getWeights();
            for (int i=0; i<weights.length; i++) {
                outWeights.add(weights[i]);
            }
        } else if (op instanceof SIRJoiner) {
            int[] weights = ((SIRJoiner) op).getWeights();
            for (int i=0; i<weights.length; i++) {
                inWeights.add(weights[i]);
            }
        }
        id = ID_COUNTER++;
    }

    public SIROperator getContents() {
        return contents;
    }

    public void setContents(SIROperator contents) {
        this.contents = contents;
    }
    
    public void addOutgoing(FlatFilter f, Integer weight) {
        outgoing.add(f);
        outWeights.add(weight);
    }
    
    public void addOutgoing(FlatFilter f) {
        outgoing.add(f);
    }
    
    public void addIncoming(FlatFilter f, Integer weight) {
        incoming.add(f);
        inWeights.add(weight);
    }
    
    public void addIncoming(FlatFilter f) {
        incoming.add(f);
    }
    
    public boolean isSplitter() {
        return contents instanceof SIRSplitter;
    }
    
    public boolean isJoiner() {
        return contents instanceof SIRJoiner;
    }
}
