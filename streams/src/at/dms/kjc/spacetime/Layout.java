/**
 * 
 */
package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.FilterTraceNode;

/**
 * @author mgordon
 *
 */
public interface Layout {
    public RawTile getTile(FilterTraceNode node);
    public void setTile(FilterTraceNode node, RawTile tile);
    public void run();
}