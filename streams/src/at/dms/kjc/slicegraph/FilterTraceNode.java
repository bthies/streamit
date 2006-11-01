package at.dms.kjc.slicegraph;

import at.dms.kjc.sir.*;
import at.dms.kjc.spacetime.Layout;
import java.util.*;
/** 
 *
 **/
public class FilterTraceNode extends TraceNode
{
    private FilterContent filter;
   
    private boolean predefined;
    private boolean laidout;

    private static HashMap<FilterContent, FilterTraceNode> contentToNode;
    
    static {
        contentToNode = new HashMap<FilterContent, FilterTraceNode>();
    }
    
    public FilterTraceNode(FilterContent filter) {
        predefined = (filter instanceof PredefinedContent);
        this.filter = filter;
        laidout = false;
        contentToNode.put(filter, this);
    }
    
    public static FilterTraceNode getFilterNode(FilterContent f) {
        return contentToNode.get(f);
    }
    
    public boolean isPredefined() 
    {
        return predefined;
    }

    public boolean isAssignedTile() 
    {
        return laidout;
    }

    public FilterContent getFilter() {
        return filter;
    }

    public String toString() {
        return filter.toString();   
    }
    
    public String toString(Layout layout) 
    {
        return filter.toString() + " " + 
        (layout != null ? layout.getTile(this) : "");   
    }
    
    
    public boolean isFileInput()
    {
        return (filter instanceof FileInputContent);
    }
    
    public boolean isFileOutput() 
    {
        return (filter instanceof FileOutputContent);
    }
}



