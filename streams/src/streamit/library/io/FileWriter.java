package streamit.io;

import streamit.Filter;
import streamit.Channel;

import java.io.*;

public class FileWriter extends Filter
{
    Class fileType;
    File outputFile;
    java.io.FileOutputStream fileOutputStream;
    ObjectOutputStream outputStream;

    public FileWriter (String fileName, Class type)
    {
        fileType = type;

        try
        {
            outputFile = new File(fileName);
            fileOutputStream = new java.io.FileOutputStream (outputFile);
            outputStream = new ObjectOutputStream (fileOutputStream);
        }
        catch(Throwable e)
        {
            ERROR (e);
        }
    }

    public void init ()
    {
        input = new Channel (fileType, 1);
    }

    public void work ()
    {
        try
        {
            if (fileType == Integer.TYPE)
            {
                outputStream.writeInt (input.popInt ());
            } else
            if (fileType == Short.TYPE)
            {
                outputStream.writeShort (input.popShort ());
            } else
            if (fileType == Character.TYPE)
            {
                outputStream.writeChar (input.popChar ());
            } else
            if (fileType == Float.TYPE)
            {
                outputStream.writeFloat (input.popFloat ());
            } else
            if (Class.forName ("java.io.Serializable").isAssignableFrom (fileType))
            {
                outputStream.writeObject (input.pop ());
            } else
            {
                ERROR ("You must define a writer for your type here.\nIf you're trying to write an object, it should be a serializable object\n(and then you won't have to do anything special).");
            }
        }
        catch (Throwable e)
        {
            ERROR (e);
        }
    }
}
