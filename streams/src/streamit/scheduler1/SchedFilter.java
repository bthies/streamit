package streamit.scheduler;

import java.util.*;

public class SchedFilter extends SchedStream
{
    Object operator;

    final List srcMsgs = new LinkedList ();
    final List dstMsgs = new LinkedList ();
}
