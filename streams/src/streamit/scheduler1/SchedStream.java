package streamit.scheduler;

import java.util.*;
import streamit.*;

public class SchedStream extends AssertedClass
{
    int consumes, produces, peeks;
    SchedStream parent;

    final List allChildren = new LinkedList ();

    public void addStream (SchedStream stream)
    {
        boolean result = allChildren.add (stream);
        ASSERT (result);
    }
}
