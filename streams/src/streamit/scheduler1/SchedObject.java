package streamit.scheduler;

import streamit.AssertedClass;
import java.io.PrintStream;

public abstract class SchedObject extends AssertedClass
{
    private Object streamObject = null;
    final private String uniqueStreamName;
    final private String streamName;

    protected SchedObject (Object streamObject)
    {
        ASSERT (streamObject);
        this.streamObject = streamObject;

        streamName = this.getStreamObject ().getClass ().getName ();
        uniqueStreamName = this.getStreamObject ().getClass ().getName ().replace ('.', '_').replace ('$', '_') + "_" + hashCode ();
    }

    /**
     * returns the original object this SchedObject is associated with
     */
    public Object getStreamObject ()
    {
        return streamObject;
    }

    /**
     * returns a (hopefully) unique name for this stream.
     * The name is related to the original stream class
     */
    public String getUniqueStreamName ()
    {
        return uniqueStreamName;
    }

    /**
     * returns a (hopefully) representative name for this stream.
     * The name is related to the original stream class
     */
    public String getStreamName ()
    {
        return streamName;
    }

    /**
     * returns the first (input) stream object of the stream subgraph
     */
    abstract SchedObject getFirstChild ();

    /**
     * returns the last (output) stream object of the stream subgraph
     */
    abstract SchedObject getLastChild ();

    /**
     * A function for printing dot graph of the system
     */
    abstract void printDot (PrintStream outputStream);

    /**
     * prints an edge for printDot
     */
    static protected void printEdge(String from, String to, PrintStream outputStream)
    {
        if (from == null || to == null)
            return;
        print (from + " -> " + to + "\n", outputStream);
    }

    /**
     * prints a string
     */
    static protected void print (String string, PrintStream outputStream)
    {
        System.out.print (string);
    }
}