package streamit.library.io;

import streamit.library.Filter;
import streamit.library.Channel;

public class DataPrinter extends Filter
{
    Class fileType;

    public DataPrinter (Class type)
    {
        fileType = type;
    }

    public void init ()
    {
        input = new Channel (fileType, 1);
    }

    public void work ()
    {
        try
        {
            if (fileType == Character.TYPE)
            {
                System.out.print (input.popChar () + ", ");
            } else
            if (fileType == Float.TYPE)
            {
                System.out.print (input.popFloat () + ", ");
            } else
            if (fileType == Integer.TYPE)
            {
                System.out.print (input.popInt () + ", ");
            } else
            if (Class.forName ("java.io.Serializable").isAssignableFrom (fileType))
            {
                System.out.print (input.pop () + ", ");
            } else
            {
                ERROR ("You must define a writer for your type here.\nIf you're trying to write an object, it should be a serializable object\n(and then you won't have to do anything special).");
            }
        }
        catch (Throwable e)
        {
            ERROR ("There was an error reading from a file");
        }
    }
}
