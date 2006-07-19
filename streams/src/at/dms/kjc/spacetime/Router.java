package at.dms.kjc.spacetime;

import java.util.LinkedList;
import at.dms.util.Utils;

public interface Router
{
    public LinkedList<ComputeNode> getRoute(ComputeNode src, ComputeNode dst);
    public int distance(ComputeNode src, ComputeNode dst); 
}

