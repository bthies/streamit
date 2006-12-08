/**
 * 
 */
package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.FilterSliceNode;

/**
 * @author mgordon
 *
 */
public interface Layout {
    public RawTile getTile(FilterSliceNode node);
    public void setTile(FilterSliceNode node, RawTile tile);
    public void run();
}