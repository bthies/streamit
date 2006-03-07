package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.*;
/** 
 *
 **/
public class FilterTraceNode extends TraceNode
{
    private FilterContent filter;
   
    private boolean predefined;
    private boolean laidout;

    public FilterTraceNode(FilterContent filter) {
        predefined = (filter instanceof PredefinedContent);
        this.filter = filter;
        laidout = false;
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
        return filter.toString() + " " + layout.getTile(this);   
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



