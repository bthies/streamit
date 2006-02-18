/**
 * 
 */
package at.dms.kjc.spacetime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;


/**
 * Given a trace, ask the user where he/she wants it placed on
 *              the raw chip.
 */
public class ManualTraceLayout {

    /**
     * Ask the user to lay out the trace on the raw chip.
     * @param rawChip The Raw Chip 
     * @param trace The Trace we would want to layout out on <rawChip>
     * @return HashMap of FilterTracesNode->RawTile 
     */
    public static HashMap layout(RawChip rawChip, Trace trace) {
        HashMap layout = new HashMap();
        BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(
                                                                              System.in));
        // the current node we are getting the tile assignment for
        TraceNode node = trace.getHead().getNext();
        // the tile number we are assigning
        int tileNumber;
        String str = "";
        RawTile tile;
        
        System.out.println("Enter layout for trace: " + trace);
        
        while (node instanceof FilterTraceNode) {
            while (true) {
                System.out.print("Enter tile number for " + node + ": ");

                try {
                    str = inputBuffer.readLine();
                    tileNumber = Integer.valueOf(str).intValue();
                } catch (Exception e) {
                    System.out.println("Bad number " + str);
                    continue;
                }

                if (tileNumber < 0 || tileNumber >= rawChip.getTotalTiles()) {
                    System.out.println("Bad tile number!");
                    continue;
                }
                tile = rawChip.getTile(tileNumber);
                if (layout.values().contains(tile)) {
                    System.out.println("Tile Already Assigned!");
                    continue;
                }
                // other wise the assignment is valid, assign and break!!
                System.out.println("Assigning " + node.toString() + " to tile "
                                   + tileNumber);
                layout.put(node, tile);
                break;
            }
            node = node.getNext();
        }

        return layout;
    }

}
