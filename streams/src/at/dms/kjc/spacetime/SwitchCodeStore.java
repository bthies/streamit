package at.dms.kjc.spacetime;

import java.util.Vector;

import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.spacetime.switchIR.*;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SwitchCodeStore {
    protected RawTile parent;
    private Vector<SwitchIns> steadySwitchIns;
    private Vector<SwitchIns> initSwitchIns;
    private Vector<SwitchIns> commAddrIns;
    private static final String LABEL_PREFIX="L_";
    private static int labelId=0;

    public SwitchCodeStore(RawTile parent) {
        this.parent = parent;
        initSwitchIns = new Vector<SwitchIns>();
        steadySwitchIns = new Vector<SwitchIns>();
        commAddrIns = new Vector<SwitchIns>();
    }

    public void appendCommAddrIns(SwitchIns ins) 
    {
        parent.setSwitches();
        commAddrIns.add(ins);
    }

    public void appendIns(SwitchIns ins, boolean init) {
        //this tile has switch code
        parent.setSwitches();
        if (init) 
            initSwitchIns.add(ins);
        else
            steadySwitchIns.add(ins);
    }
    
    public void appendIns(int i, SwitchIns ins, boolean init) 
    {
        //this tile has switch code
        parent.setSwitches();
        if (init) 
            initSwitchIns.add(i, ins);
        else
            steadySwitchIns.add(i, ins);
    }
    

    public int size(boolean init) {
        return init ? initSwitchIns.size() : steadySwitchIns.size();
    }

    public SwitchIns getIns(int i, boolean init) {
        return (init) ? initSwitchIns.get(i) : 
            steadySwitchIns.get(i);
    }

    public Label getFreshLabel() {
        return new Label(LABEL_PREFIX+(labelId++));
    }
    
    public SwitchIns getCommAddrIns(int i) 
    {
        return commAddrIns.get(i);
    }
    
    public int commAddrSize()
    {
        return commAddrIns.size();
    }
    
    /*
      public static void generateSwitchCode(ComputeNode source, ComputeNode[] dests,
      int stage, String comment)
      {
      if (stage == 0) 
      source.getRawChip().getTile(i).getSwitchCode().appendCommAddrIns(new Comment(comment));
      if (stage == 1) 
      source.getRawChip().getTile(i).getSwitchCode().appendIns(new Comment(comment), true);
      if (stage == 2)
      source.getRawChip().getTile(i).getSwitchCode().appendIns(new Comment(comment), false);
      generateSwitchCode(source, dests, stage);
      }
    */


    //create the header of a switch loop on all the tiles in <pre>tiles</pre> and 
    //first send mult from the compute processor to the switch
    //returns map of tile->Label
    public static HashMap<RawTile, Label> switchLoopHeader(HashSet<ComputeNode> tiles, int mult, boolean init, boolean primePump) 
    {
        assert mult > 1;
        HashMap<RawTile, Label> labels = new HashMap<RawTile, Label>();
        Iterator<ComputeNode> it = tiles.iterator();
        while (it.hasNext()) {
            RawTile tile = (RawTile)it.next();
            Util.sendConstFromTileToSwitch(tile, mult - 1, init, primePump, SwitchReg.R2);
            //add the label
            Label label = new Label();
            tile.getSwitchCode().appendIns(label, (init || primePump)); 
            //remember the label
            labels.put(tile, label);
        }
        return labels;
    }
    
    //create the trailer of the loop for all tiles in the key set of <pre>lables</pre>
    //labels maps RawTile->label
    public static void switchLoopTrailer(HashMap<RawTile, Label> labels, boolean init, boolean primePump) 
    {
        Iterator<RawTile> tiles = labels.keySet().iterator();
        while (tiles.hasNext()) {
            RawTile tile = tiles.next();
            Label label = labels.get(tile);
            //add the branch back
            BnezdIns branch = new BnezdIns(SwitchReg.R2, SwitchReg.R2, 
                                           label.getLabel());
            tile.getSwitchCode().appendIns(branch, (init || primePump));
        }
    }
    
    
    //return a list of all the raw tiles used in routing from source to dests
    public static HashSet<ComputeNode> getTilesInRoutes(Router router, ComputeNode source, ComputeNode[] dests) 
    {
        HashSet<ComputeNode> tiles = new HashSet<ComputeNode>();

        for (int i = 0; i < dests.length; i++) {
            ComputeNode dest = dests[i];

            LinkedList route = router.getRoute(source, dest);

            Iterator it = route.iterator();
            while (it.hasNext()) {
                ComputeNode current = (ComputeNode)it.next();
                if (current instanceof RawTile) {
                    tiles.add(current);
                }
            }
        }
        return tiles;
    }
    

    
    
    /**
     * Given a source and an array of dests, generate the code to 
     * route an item from the source to the dests and place the
     * place the switch instruction in the appropriate stage's instruction
     * vector based on the <pre>stage</pre> argument.
     * 
     * @param source 
     * @param dests the dests for this item, if multiple, duplicate
     * @param stage 0 is communicate address stage, 1 is init or primepump,
     * 2 is steady-state
     */
    public static void generateSwitchCode(Router router, 
            ComputeNode source, ComputeNode[] dests,
            int stage)
    {
        RouteIns[] ins = new RouteIns[source.getRawChip().getXSize() *
                                      source.getRawChip().getYSize()];
    
        for (int i = 0; i < dests.length; i++) {
            ComputeNode dest = dests[i];
        
            LinkedList<ComputeNode> route = router.getRoute(source, dest);
            //append the dest again to the end of route 
            //so we can place the item in the processor queue
            route.add(dest);
            Iterator<ComputeNode> it = route.iterator();
            if (!it.hasNext()) {
                System.err.println("Warning sending item to itself");
                continue;
            }
        
            ComputeNode prev = it.next();
            ComputeNode current = prev;
            ComputeNode next;
        
            while (it.hasNext()) {
                next = it.next();
                CommonUtils.println_debugging("    Route on " + current + ": " + prev + "->" + next);
                //only add the instructions to the raw tile
                if (current instanceof RawTile) {
                    //create the route instruction if it does not exist
                    if (ins[((RawTile)current).getTileNumber()] == null)
                        ins[((RawTile)current).getTileNumber()] = new RouteIns((RawTile)current);
                    //add this route to it
                    //RouteIns will take care of the duplicate routes 
                    ins[((RawTile)current).getTileNumber()].addRoute(prev, next);
                }
                prev = current;
                current = next;
            }
        }
        //add the non-null instructions
        for (int i = 0; i < ins.length; i++) {
            if (ins[i] != null) { 
                if (stage == 0) 
                    source.getRawChip().getTile(i).getSwitchCode().appendCommAddrIns(ins[i]);
                if (stage == 1) 
                    source.getRawChip().getTile(i).getSwitchCode().appendIns(ins[i], true);
                if (stage == 2)
                    source.getRawChip().getTile(i).getSwitchCode().appendIns(ins[i], false);
            }
        }
    }
    
    /**
     * Generate code on the switch neigboring <pre>dev</pre> to disregard the input from
     * the dev for <pre>words</pre> words.
     * 
     * @param dev
     * @param words
     * @param init
     */
    public static void disregardIncoming(IODevice dev, int words, boolean init) 
    {
        assert words < RawChip.cacheLineWords : "Should not align more than cache-line size";
        //get the neighboring tile 
        RawTile neighbor = dev.getNeighboringTile();
        //generate instructions to disregard the items and place 
        //them in the right vector
        for (int i = 0; i < words; i++) {
            MoveIns ins = new MoveIns(SwitchReg.R1,
                                      SwitchIPort.getIPort(neighbor.getRawChip().getDirection(neighbor, dev)));
            neighbor.getSwitchCode().appendIns(ins, init);
        }
    }
    
    /**
     * Generate code on the switch neighboring <pre>dev</pre> to send dummy values to <pre>dev</pre> for
     * <pre>words</pre> words. 
     * 
     * @param dev
     * @param words
     * @param init
     */
    public static void dummyOutgoing(IODevice dev, int words, boolean init) 
    {
        assert words < RawChip.cacheLineWords : "Should not align more than cache-line size";
        //get the neighboring tile 
        RawTile neighbor = dev.getNeighboringTile();
        for (int i = 0; i < words; i++) {
            RouteIns route = new RouteIns(neighbor);
            route.addRoute(SwitchReg.R1 , dev);
            neighbor.getSwitchCode().appendIns(route, init);
        }
    }
    

    public void appendComment(boolean init, String str) 
    {
        appendIns(new Comment(str), init);
    }
    
    
    /* 
       public void addCommAddrRoute(RawTile dest) 
       {
       LinkedList route = Router.getRoute(parent, dest);
       //append the dest again to the end of route 
       //so we can place the item in the processor queue
       route.add(dest);
    
       Iterator it = route.iterator();
    
       if (!it.hasNext()) {
       System.err.println("Warning sending item to itself");
       return;
       }
    
       RawTile prev = (RawTile)it.next();
       RawTile current = prev;
       RawTile next;
    
       while (it.hasNext()) {
       next = (RawTile)it.next();
       //generate the route on 
       RouteIns ins = new RouteIns(current);
       ins.addRoute(prev, next);
       current.getSwitchCode().appendCommAddrIns(ins);
        
       prev = current;
       current = next;
       }
       }*/
}
