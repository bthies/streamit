package at.dms.kjc.spacetime;

import java.util.LinkedList;
import at.dms.util.Utils;

public interface Router
{
    public LinkedList<RawComputeNode> getRoute(RawComputeNode src, RawComputeNode dst);
    public int distance(RawComputeNode src, RawComputeNode dst); 
}

