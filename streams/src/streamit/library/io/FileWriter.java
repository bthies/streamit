package streamit.io;

import streamit.Filter;
import streamit.Channel;

import java.io.*;

public class FileWriter extends Filter
{
    Class fileType;
    File outputFile;
    java.io.FileOutputStream fileOutputStream;
    DataOutputStream outputStream;

    public FileWriter (String fileName, Class type)
    {
        fileType = type;

        try
        {
            outputFile = new File(fileName);
            fileOutputStream = new java.io.FileOutputStream (outputFile);
            outputStream = new DataOutputStream (fileOutputStream);
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
            {
                ERROR ("You must define a writer for your type here.\nObject writing isn't supported right now (for compatibility with the\nC library).");
            }
        }
        catch (Throwable e)
        {
            ERROR (e);
        }
    }
}
