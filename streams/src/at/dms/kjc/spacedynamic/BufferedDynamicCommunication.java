/**
 * 
 */
package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;

/**
 * @author mgordon
 *
 */
public class BufferedDynamicCommunication {
    /** the layout object for the flatnode * */
    private Layout layout;

    /** the ssg of this flat node * */
    private StaticStreamGraph ssg;

    /** the flat node we are generating code for * */
    private FlatNode node;
    
    public BufferedDynamicCommunication(StaticStreamGraph ssg, FlatNode node) {
        this.ssg = ssg;
        this.node = node;
    }
    
    public void doit() {
        assert false;
    }
}
